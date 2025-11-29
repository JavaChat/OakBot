package oakbot.bot;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.IChatClient;
import com.github.mangstadt.sochat4j.IRoom;
import com.github.mangstadt.sochat4j.PrivateRoomException;
import com.github.mangstadt.sochat4j.RoomNotFoundException;
import com.github.mangstadt.sochat4j.RoomPermissionException;
import com.github.mangstadt.sochat4j.event.Event;
import com.github.mangstadt.sochat4j.event.InvitationEvent;
import com.github.mangstadt.sochat4j.event.MessageEditedEvent;
import com.github.mangstadt.sochat4j.event.MessagePostedEvent;
import com.github.mangstadt.sochat4j.util.Sleeper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import oakbot.Database;
import oakbot.MemoryDatabase;
import oakbot.Rooms;
import oakbot.Statistics;
import oakbot.filter.ChatResponseFilter;
import oakbot.inactivity.InactivityTask;
import oakbot.listener.Listener;
import oakbot.task.ScheduledTask;
import oakbot.util.ChatBuilder;

/**
 * A Stackoverflow chat bot.
 * @author Michael Angstadt
 */
public class Bot implements IBot {
	private static final Logger logger = LoggerFactory.getLogger(Bot.class);
	static final int BOTLER_ID = 13750349;
	
	private static final int ROOM_JOIN_DELAY_MS = 2000;
    private static final Duration ROOM_JOIN_DELAY = Duration.ofSeconds(2);

	private final BotConfiguration config;
	private final SecurityConfiguration security;
	private final IChatClient connection;
	private final AtomicLong choreIdCounter = new AtomicLong();
	private final BlockingQueue<Chore> choreQueue = new PriorityBlockingQueue<>();
	private final Rooms rooms;
	private final Integer maxRooms;
	private final List<Listener> listeners;
	private final List<ChatResponseFilter> responseFilters;
	private final List<ScheduledTask> scheduledTasks;
	private final List<InactivityTask> inactivityTasks;
	private final Map<Integer, LocalDateTime> timeOfLastMessageByRoom = new HashMap<>();
	private final Multimap<Integer, TimerTask> inactivityTimerTasksByRoom = ArrayListMultimap.create();
	private final Statistics stats;
	private final Database database;
	private final Timer timer = new Timer();
	private TimerTask timeoutTask;
	private volatile boolean timeout = false;

	/**
	 * <p>
	 * A collection of messages that the bot posted, but have not been "echoed"
	 * back yet in the chat room. When a message is echoed back, it is removed
	 * from this map.
	 * </p>
	 * <p>
	 * This is used to determine whether something the bot posted was converted
	 * to a onebox. It is then used to edit the message in order to hide the
	 * onebox.
	 * </p>
	 * <ul>
	 * <li>Key = The message ID.</li>
	 * <li>Value = The raw message content that was sent to the chat room by the
	 * bot (which can be different from what was echoed back).</li>
	 * </ul>
	 */
	private final Map<Long, PostedMessage> postedMessages = new HashMap<>();

	private Bot(Builder builder) {
		connection = Objects.requireNonNull(builder.connection);

		var userName = (connection.getUsername() == null) ? builder.userName : connection.getUsername();
		var userId = (connection.getUserId() == null) ? builder.userId : connection.getUserId();
		
		config = new BotConfiguration(userName, userId, builder.trigger, builder.greeting, builder.hideOneboxesAfter);
		security = new SecurityConfiguration(builder.admins, builder.bannedUsers, builder.allowedUsers);
		
		maxRooms = builder.maxRooms;
		stats = builder.stats;
		database = (builder.database == null) ? new MemoryDatabase() : builder.database;
		rooms = new Rooms(database, builder.roomsHome, builder.roomsQuiet);
		listeners = builder.listeners;
		scheduledTasks = builder.tasks;
		inactivityTasks = builder.inactivityTasks;
		responseFilters = builder.responseFilters;
	}

	private void scheduleTask(ScheduledTask task) {
		var nextRun = task.nextRun();
		if (nextRun <= 0) {
			return;
		}

		scheduleChore(nextRun, new ScheduledTaskChore(task));
	}

	private void scheduleTask(InactivityTask task, IRoom room, Duration nextRun) {
		var timerTask = scheduleChore(nextRun, new InactivityTaskChore(task, room));
		inactivityTimerTasksByRoom.put(room.getRoomId(), timerTask);
	}

	/**
	 * Starts the chat bot. The bot will join the rooms in the current thread
	 * before launching its own thread.
	 * @param quiet true to start the bot without broadcasting the greeting
	 * message, false to broadcast the greeting message
	 * @return the thread that the bot is running in. This thread will terminate
	 * when the bot terminates
	 * @throws IOException if there's a network problem
	 */
	public Thread connect(boolean quiet) throws IOException {
		joinRoomsOnStart(quiet);

		var thread = new ChoreThread();
		thread.start();
		return thread;
	}

	private void joinRoomsOnStart(boolean quiet) {
		var first = true;
		var roomsCopy = new ArrayList<>(rooms.getRooms());
		for (var room : roomsCopy) {
			if (!first) {
				/*
				 * Insert a pause between joining each room in an attempt to
				 * resolve an issue where the bot chooses to ignore all messages
				 * in certain rooms.
				 */
				Sleeper.sleep(ROOM_JOIN_DELAY);
			}

			try {
				joinRoom(room, quiet);
			} catch (Exception e) {
				logger.atError().setCause(e).log(() -> "Could not join room " + room + ". Removing from rooms list.");
				rooms.remove(room);
			}

			first = false;
		}
	}

	private class ChoreThread extends Thread {
		@Override
		public void run() {
			try {
				scheduledTasks.forEach(Bot.this::scheduleTask);

				while (true) {
					Chore chore;
					try {
						chore = choreQueue.take();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						logger.atError().setCause(e).log(() -> "Thread interrupted while waiting for new chores.");
						break;
					}

					if (chore instanceof StopChore || chore instanceof FinishChore) {
						break;
					}

					chore.complete();
					database.commit();
				}
			} catch (Exception e) {
				logger.atError().setCause(e).log(() -> "Bot terminated due to unexpected exception.");
			} finally {
				try {
					connection.close();
				} catch (IOException e) {
					logger.atError().setCause(e).log(() -> "Problem closing ChatClient connection.");
				}

				database.commit();
				timer.cancel();
			}
		}
	}

	@Override
	public List<ChatMessage> getLatestMessages(int roomId, int count) throws IOException {
		var room = connection.getRoom(roomId);
		var notInRoom = (room == null);
		if (notInRoom) {
			return List.of();
		}

		//@formatter:off
		return room.getMessages(count).stream()
			.map(this::convertFromBotlerRelayMessage)
		.toList();
		//@formatter:on
	}

	@Override
	public String getOriginalMessageContent(long messageId) throws IOException {
		return connection.getOriginalMessageContent(messageId);
	}

	@Override
	public String uploadImage(String url) throws IOException {
		return connection.uploadImage(url);
	}

	@Override
	public String uploadImage(byte[] data) throws IOException {
		return connection.uploadImage(data);
	}

	@Override
	public void sendMessage(int roomId, PostMessage message) throws IOException {
		var room = connection.getRoom(roomId);
		if (room != null) {
			sendMessage(room, message);
		}
	}

	private void sendMessage(IRoom room, String message) throws IOException {
		sendMessage(room, new PostMessage(message));
	}

	private void sendMessage(IRoom room, PostMessage message) throws IOException {
		final String filteredMessage;
		if (message.bypassFilters()) {
			filteredMessage = message.message();
		} else {
			var messageText = message.message();
			for (var filter : responseFilters) {
				if (filter.isEnabled(room.getRoomId())) {
					messageText = filter.filter(messageText);
				}
			}
			filteredMessage = messageText;
		}

		logger.atInfo().log(() -> "Sending message [room=" + room.getRoomId() + "]: " + filteredMessage);

		synchronized (postedMessages) {
			var messageIds = room.sendMessage(filteredMessage, message.parentId(), message.splitStrategy());
			var condensedMessage = message.condensedMessage();
			var ephemeral = message.ephemeral();

			var postedMessage = new PostedMessage(Instant.now(), filteredMessage, condensedMessage, ephemeral, room.getRoomId(), message.parentId(), messageIds);
			postedMessages.put(messageIds.get(0), postedMessage);
		}
	}

	@Override
	public void join(int roomId) throws IOException {
		joinRoom(roomId);
	}

	/**
	 * Joins a room.
	 * @param roomId the room ID
	 * @return the connection to the room
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws PrivateRoomException if the room can't be joined because it is
	 * private
	 * @throws IOException if there's a problem connecting to the room
	 */
	private IRoom joinRoom(int roomId) throws RoomNotFoundException, PrivateRoomException, IOException {
		return joinRoom(roomId, false);
	}

	/**
	 * Joins a room.
	 * @param roomId the room ID
	 * @param quiet true to not post an announcement message, false to post one
	 * @return the connection to the room
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws PrivateRoomException if the room can't be joined because it is
	 * private
	 * @throws IOException if there's a problem connecting to the room
	 */
	private IRoom joinRoom(int roomId, boolean quiet) throws RoomNotFoundException, PrivateRoomException, IOException {
		var room = connection.getRoom(roomId);
		if (room != null) {
			return room;
		}

		logger.atInfo().log(() -> "Joining room " + roomId + "...");

		room = connection.joinRoom(roomId);

		room.addEventListener(MessagePostedEvent.class, event -> choreQueue.add(new ChatEventChore(event)));
		room.addEventListener(MessageEditedEvent.class, event -> choreQueue.add(new ChatEventChore(event)));
		room.addEventListener(InvitationEvent.class, event -> choreQueue.add(new ChatEventChore(event)));

		if (!quiet && config.getGreeting() != null) {
			try {
				sendMessage(room, config.getGreeting());
			} catch (RoomPermissionException e) {
				logger.atWarn().setCause(e).log(() -> "Unable to post greeting when joining room " + roomId + ".");
			}
		}

		rooms.add(roomId);

		for (var task : inactivityTasks) {
			var nextRun = task.getInactivityTime(room, this);
			if (nextRun == null) {
				continue;
			}

			scheduleTask(task, room, nextRun);
		}

		return room;
	}

	@Override
	public void leave(int roomId) throws IOException {
		logger.atInfo().log(() -> "Leaving room " + roomId + "...");

		inactivityTimerTasksByRoom.removeAll(roomId).forEach(TimerTask::cancel);
		timeOfLastMessageByRoom.remove(roomId);
		rooms.remove(roomId);

		var room = connection.getRoom(roomId);
		if (room != null) {
			room.leave();
		}
	}

	@Override
	public String getUsername() {
		return config.getUserName();
	}

	@Override
	public Integer getUserId() {
		return config.getUserId();
	}

	@Override
	public List<Integer> getAdminUsers() {
		return security.getAdmins();
	}

	private boolean isAdminUser(Integer userId) {
		return security.isAdmin(userId);
	}

	@Override
	public boolean isRoomOwner(int roomId, int userId) throws IOException {
		var userInfo = connection.getUserInfo(roomId, userId);
		return (userInfo == null) ? false : userInfo.isOwner();
	}

	@Override
	public String getTrigger() {
		return config.getTrigger();
	}

	@Override
	public List<Integer> getRooms() {
		return rooms.getRooms();
	}

	@Override
	public IRoom getRoom(int roomId) {
		return connection.getRoom(roomId);
	}

	@Override
	public List<Integer> getHomeRooms() {
		return rooms.getHomeRooms();
	}

	@Override
	public List<Integer> getQuietRooms() {
		return rooms.getQuietRooms();
	}

	@Override
	public Integer getMaxRooms() {
		return maxRooms;
	}

	@Override
	public void broadcastMessage(PostMessage message) throws IOException {
		for (var room : connection.getRooms()) {
			if (!rooms.isQuietRoom(room.getRoomId())) {
				sendMessage(room, message);
			}
		}
	}

	@Override
	public synchronized void timeout(Duration duration) {
		if (timeout) {
			timeoutTask.cancel();
		} else {
			timeout = true;
		}

		timeoutTask = new TimerTask() {
			@Override
			public void run() {
				timeout = false;
			}
		};

		timer.schedule(timeoutTask, duration.toMillis());
	}

	@Override
	public synchronized void cancelTimeout() {
		timeout = false;
		if (timeoutTask != null) {
			timeoutTask.cancel();
		}
	}

	/**
	 * Sends a signal to immediately stop processing tasks. The bot thread will
	 * stop running once it is done processing the current task.
	 */
	public void stop() {
		choreQueue.add(new StopChore());
	}

	/**
	 * Sends a signal to finish processing the tasks in the queue, and then
	 * terminate.
	 */
	public void finish() {
		choreQueue.add(new FinishChore());
	}

	private TimerTask scheduleChore(long delay, Chore chore) {
		var timerTask = new TimerTask() {
			@Override
			public void run() {
				choreQueue.add(chore);
			}
		};
		timer.schedule(timerTask, delay);

		return timerTask;
	}

	private TimerTask scheduleChore(Duration delay, Chore chore) {
		return scheduleChore(delay.toMillis(), chore);
	}

	/**
	 * Represents a message that was posted to the chat room.
	 * @author Michael Angstadt
	 */
	private static class PostedMessage {
		private final Instant timePosted;
		private final String originalContent;
		private final String condensedContent;
		private final boolean ephemeral;
		private final int roomId;
		private final long parentId;
		private final List<Long> messageIds;

		/**
		 * @param timePosted the time the message was posted
		 * @param originalContent the original message that the bot sent to the
		 * chat room
		 * @param condensedContent the text that the message should be changed
		 * to after the amount of time specified in the "hideOneboxesAfter"
		 * setting
		 * @param ephemeral true to delete the message after the amount of time
		 * specified in the "hideOneboxesAfter" setting, false not to
		 * @param roomId the ID of the room the message was posted in
		 * @param parentId the ID of the message that this was a reply to
		 * @param messageIds the ID of each message that was actually posted to
		 * the room (the chat client may split up the original message due to
		 * length limitations)
		 */
		public PostedMessage(Instant timePosted, String originalContent, String condensedContent, boolean ephemeral, int roomId, long parentId, List<Long> messageIds) {
			this.timePosted = timePosted;
			this.originalContent = originalContent;
			this.condensedContent = condensedContent;
			this.ephemeral = ephemeral;
			this.roomId = roomId;
			this.parentId = parentId;
			this.messageIds = messageIds;
		}

		/**
		 * Gets the time the message was posted.
		 * @return the time the message was posted
		 */
		public Instant getTimePosted() {
			return timePosted;
		}

		/**
		 * Gets the content of the original message that the bot sent to the
		 * chat room. This is used for when a message was converted to a onebox.
		 * @return the original content
		 */
		public String getOriginalContent() {
			return originalContent;
		}

		/**
		 * Gets the text that the message should be changed to after the amount
		 * of time specified in the "hideOneboxesAfter" setting.
		 * @return the new content or null to leave the message alone
		 */
		public String getCondensedContent() {
			return condensedContent;
		}

		/**
		 * Gets the ID of each message that was actually posted to the room. The
		 * chat client may split up the original message due to length
		 * limitations.
		 * @return the message IDs
		 */
		public List<Long> getMessageIds() {
			return messageIds;
		}

		/**
		 * Gets the ID of the room the message was posted in.
		 * @return the room ID
		 */
		public int getRoomId() {
			return roomId;
		}

		/**
		 * Determines if the message has requested that it be condensed or
		 * deleted after the amount of time specified in the "hideOneboxesAfter"
		 * setting. Does not include messages that were converted to oneboxes.
		 * @return true to condense or delete the message, false to leave it
		 * alone
		 */
		public boolean isCondensableOrEphemeral() {
			return condensedContent != null || isEphemeral();
		}

		/**
		 * Determines if the message has requested that it be deleted after the
		 * amount of time specified in the "hideOneboxesAfter"
		 * setting. Does not include messages that were converted to oneboxes.
		 * @return true to delete the message, false not to
		 */
		public boolean isEphemeral() {
			return ephemeral;
		}
		
		/**
		 * Gets the ID of the message that this was a reply to.
		 * @return the parent ID or 0 if it's not a reply
		 */
		public long getParentId() {
			return parentId;
		}
	}

	private abstract class Chore implements Comparable<Chore> {
		private final long choreId;

		public Chore() {
			choreId = choreIdCounter.getAndIncrement();
		}

		public abstract void complete();

		@Override
		public int compareTo(Chore that) {
			/*
			 * The "lowest" value will be popped off the queue first.
			 */

			if (isBothStopChore(that)) {
				return 0;
			}
			if (isThisStopChore()) {
				return -1;
			}
			if (isThatStopChore(that)) {
				return 1;
			}

			if (isBothCondenseMessageChore(that)) {
				return Long.compare(this.choreId, that.choreId);
			}
			if (isThisCondenseMessageChore()) {
				return -1;
			}
			if (isThatCondenseMessageChore(that)) {
				return 1;
			}

			return Long.compare(this.choreId, that.choreId);
		}

		private boolean isBothStopChore(Chore that) {
			return this instanceof StopChore && that instanceof StopChore;
		}

		private boolean isThisStopChore() {
			return this instanceof StopChore;
		}

		private boolean isThatStopChore(Chore that) {
			return that instanceof StopChore;
		}

		private boolean isBothCondenseMessageChore(Chore that) {
			return this instanceof CondenseMessageChore && that instanceof CondenseMessageChore;
		}

		private boolean isThisCondenseMessageChore() {
			return this instanceof CondenseMessageChore;
		}

		private boolean isThatCondenseMessageChore(Chore that) {
			return that instanceof CondenseMessageChore;
		}
	}

	private class StopChore extends Chore {
		@Override
		public void complete() {
			//empty
		}
	}

	private class FinishChore extends Chore {
		@Override
		public void complete() {
			//empty
		}
	}

	private class ChatEventChore extends Chore {
		private final Event event;

		public ChatEventChore(Event event) {
			this.event = event;
		}

		@Override
		public void complete() {
			if (event instanceof MessagePostedEvent mpe) {
				handleMessage(mpe.getMessage());
				return;
			}

			if (event instanceof MessageEditedEvent mee) {
				handleMessage(mee.getMessage());
				return;
			}

			if (event instanceof InvitationEvent ie) {
				var roomId = ie.getRoomId();
				var userId = ie.getUserId();
				var inviterIsAdmin = isAdminUser(userId);

				boolean acceptInvitation;
				if (inviterIsAdmin) {
					acceptInvitation = true;
				} else {
					try {
						acceptInvitation = isRoomOwner(roomId, userId);
					} catch (IOException e) {
						logger.atError().setCause(e).log(() -> "Unable to handle room invite. Error determining whether user is room owner.");
						acceptInvitation = false;
					}
				}

				if (acceptInvitation) {
					handleInvitation(ie);
				}

				return;
			}

			logger.atError().log(() -> "Ignoring event: " + event.getClass().getName());
		}

		private void handleMessage(ChatMessage message) {
			var userId = message.getUserId();
			var isAdminUser = isAdminUser(userId);
			var isBotInTimeout = timeout && !isAdminUser;
			
			if (isBotInTimeout) {
				//bot is in timeout, ignore
				return;
			}

			var messageWasDeleted = message.getContent() == null;
			if (messageWasDeleted) {
				//user deleted their message, ignore
				return;
			}

			var hasAllowedUsersList = !security.getAllowedUsers().isEmpty();
			var userIsAllowed = security.isAllowed(userId);
			if (hasAllowedUsersList && !userIsAllowed) {
				//message was posted by a user who is not in the green list, ignore
				return;
			}

			var userIsBanned = security.isBanned(userId);
			if (userIsBanned) {
				//message was posted by a banned user, ignore
				return;
			}

			var room = connection.getRoom(message.getRoomId());
			if (room == null) {
				//the bot is no longer in the room
				return;
			}

			if (message.getUserId() == userId) {
				//message was posted by this bot
				handleBotMessage(message);
				return;
			}

			message = convertFromBotlerRelayMessage(message);

			timeOfLastMessageByRoom.put(message.getRoomId(), message.getTimestamp());

			var actions = handleListeners(message);
			handleActions(message, actions);
		}

		private void handleBotMessage(ChatMessage message) {
			PostedMessage postedMessage;
			synchronized (postedMessages) {
				postedMessage = postedMessages.remove(message.getMessageId());
			}

			/*
			 * Check to see if the message should be edited for brevity
			 * after a short time so it doesn't spam the chat history.
			 * 
			 * This could happen if (1) the bot posted something that Stack
			 * Overflow Chat converted to a onebox (e.g. an image) or (2)
			 * the message itself has asked to be edited (e.g. a javadoc
			 * description).
			 * 
			 * ===What is a onebox?===
			 * 
			 * Stack Overflow Chat converts certain URLs to "oneboxes".
			 * Oneboxes can be fairly large and can spam the chat. For
			 * example, if the message is a URL to an image, the image
			 * itself will be displayed in the chat room. This is nice, but
			 * gets annoying if the image is large or if it's an animated
			 * GIF.
			 * 
			 * After giving people some time to see the onebox, the bot will
			 * edit the message so that the onebox no longer displays, but
			 * the URL is still preserved.
			 */
			var messageIsOnebox = message.getContent().isOnebox();
			if (postedMessage != null && config.getHideOneboxesAfter() != null && (messageIsOnebox || postedMessage.isCondensableOrEphemeral())) {
				var postedMessageAge = Duration.between(postedMessage.getTimePosted(), Instant.now());
				var hideIn = config.getHideOneboxesAfter().minus(postedMessageAge);

				logger.atInfo().log(() -> {
					var action = messageIsOnebox ? "Hiding onebox" : "Condensing message";
					return action + " in " + hideIn.toMillis() + "ms [room=" + message.getRoomId() + ", id=" + message.getMessageId() + "]: " + message.getContent();
				});

				scheduleChore(hideIn, new CondenseMessageChore(postedMessage));
			}
		}

		private ChatActions handleListeners(ChatMessage message) {
			var actions = new ChatActions();
			for (var listener : listeners) {
				try {
					actions.addAll(listener.onMessage(message, Bot.this));
				} catch (Exception e) {
					logger.atError().setCause(e).log(() -> "Problem running listener.");
				}
			}
			return actions;
		}

		private void handleActions(ChatMessage message, ChatActions actions) {
			if (actions.isEmpty()) {
				return;
			}

			logger.atInfo().log(() -> "Responding to message [room=" + message.getRoomId() + ", user=" + message.getUsername() + ", id=" + message.getMessageId() + "]: " + message.getContent());

			if (stats != null) {
				stats.incMessagesRespondedTo();
			}

			var queue = new LinkedList<>(actions.getActions());
			while (!queue.isEmpty()) {
				var action = queue.removeFirst();
				processAction(action, message, queue);
			}
		}

		private void processAction(ChatAction action, ChatMessage message, LinkedList<ChatAction> queue) {
			// Polymorphic dispatch - each action knows how to execute itself
			// Special handling for PostMessage delays is done within PostMessage.execute()
			if (action instanceof PostMessage pm && pm.delay() != null) {
				// Delayed messages need access to internal scheduling
				handlePostMessageAction(pm, message);
				return;
			}
			
			var context = new ActionContext(this, message);
			var response = action.execute(context);
			queue.addAll(response.getActions());
		}

		private void handlePostMessageAction(PostMessage action, ChatMessage message) {
			try {
				if (action.delay() != null) {
					scheduleChore(action.delay(), new DelayedMessageChore(message.getRoomId(), action));
				} else {
					if (action.broadcast()) {
						broadcastMessage(action);
					} else {
						sendMessage(message.getRoomId(), action);
					}
				}
			} catch (Exception e) {
				logger.atError().setCause(e).log(() -> "Problem posting message [room=" + message.getRoomId() + "]: " + action.message());
			}
		}

		private ChatActions handleDeleteMessageAction(DeleteMessage action, ChatMessage message) {
			try {
				var room = connection.getRoom(message.getRoomId());
				room.deleteMessage(action.messageId());
				return action.onSuccess().get();
			} catch (Exception e) {
				logger.atError().setCause(e).log(() -> "Problem deleting message [room=" + message.getRoomId() + ", messageId=" + action.messageId() + "]");
				return action.onError().apply(e);
			}
		}

		private ChatActions handleJoinRoomAction(JoinRoom action) {
			if (maxRooms != null && connection.getRooms().size() >= maxRooms) {
				return action.onError().apply(new IOException("Cannot join room. Max rooms reached."));
			}

			try {
				var joinedRoom = joinRoom(action.roomId());
				if (joinedRoom.canPost()) {
					return action.onSuccess().get();
				}

                leaveRoomSafely(action.roomId(), () -> "Problem leaving room " + action.roomId() + " after it was found that the bot can't post messages to it.");				return action.ifLackingPermissionToPost().get();
			} catch (PrivateRoomException | RoomPermissionException e) {
                leaveRoomSafely(action.roomId(), () -> "Problem leaving room " + action.roomId() + " after it was found that the bot can't join or post messages to it.");				return action.ifLackingPermissionToPost().get();
			} catch (RoomNotFoundException e) {
				return action.ifRoomDoesNotExist().get();
			} catch (Exception e) {
				return action.onError().apply(e);
			}
		}

		/**
		 * Attempts to leave a room and logs any errors that occur.
		 * @param roomId the room ID to leave
         * @param logMessage supplier for the complete log message (evaluated only if an error occurs)
         **/
        private void leaveRoomSafely(int roomId, Supplier<String> logMessage) {
            try {
				leave(roomId);
			} catch (Exception e) {
                logger.atError().setCause(e).log(logMessage);			}
		}

		private void handleLeaveRoomAction(LeaveRoom action) {
			try {
				leave(action.roomId());
			} catch (Exception e) {
				logger.atError().setCause(e).log(() -> "Problem leaving room " + action.roomId() + ".");
			}
		}

		private void handleInvitation(InvitationEvent event) {
			/*
			 * If the bot is currently connected to multiple rooms, the
			 * invitation event will be sent to each room and this method will
			 * be called multiple times. Check to see if the bot has already
			 * joined the room it was invited to.
			 */
			var roomId = event.getRoomId();
			if (connection.isInRoom(roomId)) {
				return;
			}

			/*
			 * Ignore the invitation if the bot is connected to the maximum
			 * number of rooms allowed. We can't really post an error message
			 * because the invitation event is not linked to a specific chat
			 * room.
			 */
			var maxRoomsExceeded = (maxRooms != null && connection.getRooms().size() >= maxRooms);
			if (maxRoomsExceeded) {
				return;
			}

			try {
				joinRoom(roomId);
			} catch (Exception e) {
				logger.atError().setCause(e).log(() -> "Bot was invited to join room " + roomId + ", but couldn't join it.");
			}
		}
	}

	private class CondenseMessageChore extends Chore {
		private final Pattern replyRegex = Pattern.compile("^:(\\d+) (.*)", Pattern.DOTALL);
		private final PostedMessage postedMessage;

		public CondenseMessageChore(PostedMessage postedMessage) {
			this.postedMessage = postedMessage;
		}

		@Override
		public void complete() {
			var roomId = postedMessage.getRoomId();
			var room = connection.getRoom(roomId);

			var botIsNoLongerInTheRoom = (room == null);
			if (botIsNoLongerInTheRoom) {
				return;
			}

			try {
				List<Long> messagesToDelete;
				if (postedMessage.isEphemeral()) {
					messagesToDelete = postedMessage.getMessageIds();
				} else {
					var condensedContent = postedMessage.getCondensedContent();
					var isAOneBox = (condensedContent == null);
					if (isAOneBox) {
						condensedContent = postedMessage.getOriginalContent();
					}

					var messageIds = postedMessage.getMessageIds();
					var quotedContent = quote(condensedContent);
					room.editMessage(messageIds.get(0), postedMessage.getParentId(), quotedContent);

					/*
					 * If the original content was split up into
					 * multiple messages due to length constraints,
					 * delete the additional messages.
					 */
					messagesToDelete = messageIds.subList(1, messageIds.size());
				}

				for (var id : messagesToDelete) {
					room.deleteMessage(id);
				}
			} catch (Exception e) {
                logger.atError().setCause(e).log(() -> "Problem editing chat message [room=" + roomId + ", id=" + postedMessage.getMessageIds().get(0) + "]");
			}
		}

		@SuppressWarnings("deprecation")
		private String quote(String content) {
			var cb = new ChatBuilder();

			var m = replyRegex.matcher(content);
			if (m.find()) {
				var id = Long.parseLong(m.group(1));
				content = m.group(2);

				cb.reply(id);
			}

			return cb.quote(content).toString();
		}
	}

	private class ScheduledTaskChore extends Chore {
		private final ScheduledTask task;

		public ScheduledTaskChore(ScheduledTask task) {
			this.task = task;
		}

		@Override
		public void complete() {
			try {
				task.run(Bot.this);
			} catch (Exception e) {
                logger.atError().setCause(e).log(() -> "Problem running scheduled task.");
			}
			scheduleTask(task);
		}
	}

	private class InactivityTaskChore extends Chore {
		private final InactivityTask task;
		private final IRoom room;

		public InactivityTaskChore(InactivityTask task, IRoom room) {
			this.task = task;
			this.room = room;
		}

		@Override
		public void complete() {
			try {
				if (!connection.isInRoom(room.getRoomId())) {
					return;
				}

				var inactivityTime = task.getInactivityTime(room, Bot.this);
				if (inactivityTime == null) {
					return;
				}

				var lastMessageTimestamp = timeOfLastMessageByRoom.get(room.getRoomId());
				var roomInactiveFor = (lastMessageTimestamp == null) ? inactivityTime : Duration.between(lastMessageTimestamp, LocalDateTime.now());
				var runNow = (roomInactiveFor.compareTo(inactivityTime) >= 0);
				if (runNow) {
					try {
						task.run(room, Bot.this);
					} catch (Exception e) {
                        logger.atError().setCause(e).log(() -> "Problem running inactivity task in room " + room.getRoomId() + ".");
					}
				}

				var nextCheck = runNow ? inactivityTime : inactivityTime.minus(roomInactiveFor);
				scheduleTask(task, room, nextCheck);
			} finally {
				inactivityTimerTasksByRoom.remove(room, this);
			}
		}
	}

	private class DelayedMessageChore extends Chore {
		private final int roomId;
		private final PostMessage message;

		public DelayedMessageChore(int roomId, PostMessage message) {
			this.roomId = roomId;
			this.message = message;
		}

		@Override
		public void complete() {
			try {
				if (message.broadcast()) {
					broadcastMessage(message);
				} else {
					sendMessage(roomId, message);
				}
			} catch (Exception e) {
                logger.atError().setCause(e).log(() -> "Problem posting delayed message [room=" + roomId + ", delay=" + message.delay() + "]: " + message.message());
			}
		}
	}

	/**
	 * Alters the username and content of a message if the message is a Botler
	 * Discord relay message. Otherwise, returns the message unaltered.
	 * @param message the original message
	 * @return the altered message or the same message if it's not a relay
	 * message
	 * @see <a href=
	 * "https://chat.stackoverflow.com/transcript/message/57337679#57337679">example</a>
	 */
	private ChatMessage convertFromBotlerRelayMessage(ChatMessage message) {
		if (message.getUserId() != BOTLER_ID) {
			return message;
		}

		var content = message.getContent();
		if (content == null) {
			return message;
		}

		//Example message content:
		//[<b><a href=\"https://discord.gg/PNMq3pBSUe\" rel=\"nofollow noopener noreferrer\">realmichael</a></b>] test
		var html = content.getContent();
		var dom = Jsoup.parse(html);
		var element = dom.selectFirst("b a[href=\"https://discord.gg/PNMq3pBSUe\"]");
		if (element == null) {
			return message;
		}
		var discordUsername = element.text();

		var endBracket = html.indexOf(']');
		if (endBracket < 0) {
			return message;
		}
		var discordMessage = html.substring(endBracket + 1).trim();

		//@formatter:off
		return new ChatMessage.Builder(message)
			.username(discordUsername)
			.content(discordMessage)
		.build();
		//@formatter:on
	}

	/**
	 * Builds {@link Bot} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private IChatClient connection;
		private String userName;
		private String trigger = "=";
		private String greeting;
		private Integer userId;
		private Duration hideOneboxesAfter;
		private Integer maxRooms;
		private List<Integer> roomsHome = List.of(1);
		private List<Integer> roomsQuiet = List.of();
		private List<Integer> admins = List.of();
		private List<Integer> bannedUsers = List.of();
		private List<Integer> allowedUsers = List.of();
		private List<Listener> listeners = List.of();
		private List<ScheduledTask> tasks = List.of();
		private List<InactivityTask> inactivityTasks = List.of();
		private List<ChatResponseFilter> responseFilters = List.of();
		private Statistics stats;
		private Database database;

		public Builder connection(IChatClient connection) {
			this.connection = connection;
			return this;
		}

		public Builder user(String userName, Integer userId) {
			this.userName = (userName == null || userName.isEmpty()) ? null : userName;
			this.userId = userId;
			return this;
		}

		public Builder hideOneboxesAfter(Duration hideOneboxesAfter) {
			this.hideOneboxesAfter = hideOneboxesAfter;
			return this;
		}

		public Builder trigger(String trigger) {
			this.trigger = trigger;
			return this;
		}

		public Builder greeting(String greeting) {
			this.greeting = greeting;
			return this;
		}

		public Builder roomsHome(Integer... roomIds) {
			roomsHome = List.of(roomIds);
			return this;
		}

		public Builder roomsHome(Collection<Integer> roomIds) {
			roomsHome = List.copyOf(roomIds);
			return this;
		}

		public Builder roomsQuiet(Integer... roomIds) {
			roomsQuiet = List.of(roomIds);
			return this;
		}

		public Builder roomsQuiet(Collection<Integer> roomIds) {
			roomsQuiet = List.copyOf(roomIds);
			return this;
		}

		public Builder maxRooms(Integer maxRooms) {
			this.maxRooms = maxRooms;
			return this;
		}

		public Builder admins(Integer... admins) {
			this.admins = List.of(admins);
			return this;
		}

		public Builder admins(Collection<Integer> admins) {
			this.admins = List.copyOf(admins);
			return this;
		}

		public Builder bannedUsers(Integer... bannedUsers) {
			this.bannedUsers = List.of(bannedUsers);
			return this;
		}

		public Builder bannedUsers(Collection<Integer> bannedUsers) {
			this.bannedUsers = List.copyOf(bannedUsers);
			return this;
		}

		public Builder allowedUsers(Integer... allowedUsers) {
			this.allowedUsers = List.of(allowedUsers);
			return this;
		}

		public Builder allowedUsers(Collection<Integer> allowedUsers) {
			this.allowedUsers = List.copyOf(allowedUsers);
			return this;
		}

		public Builder listeners(Listener... listeners) {
			this.listeners = List.of(listeners);
			return this;
		}

		public Builder listeners(Collection<Listener> listeners) {
			this.listeners = List.copyOf(listeners);
			return this;
		}

		public Builder tasks(ScheduledTask... tasks) {
			this.tasks = List.of(tasks);
			return this;
		}

		public Builder tasks(Collection<ScheduledTask> tasks) {
			this.tasks = List.copyOf(tasks);
			return this;
		}

		public Builder inactivityTasks(InactivityTask... tasks) {
			inactivityTasks = List.of(tasks);
			return this;
		}

		public Builder inactivityTasks(Collection<InactivityTask> tasks) {
			inactivityTasks = List.copyOf(tasks);
			return this;
		}

		public Builder responseFilters(ChatResponseFilter... filters) {
			responseFilters = List.of(filters);
			return this;
		}

		public Builder responseFilters(Collection<ChatResponseFilter> filters) {
			responseFilters = List.copyOf(filters);
			return this;
		}

		public Builder stats(Statistics stats) {
			this.stats = stats;
			return this;
		}

		public Builder database(Database database) {
			this.database = database;
			return this;
		}

		public Bot build() {
			if (connection == null) {
				throw new IllegalStateException("No ChatConnection given.");
			}

			if (connection.getUsername() == null && this.userName == null) {
				throw new IllegalStateException("Unable to parse username. You'll need to manually set it in the properties section of the bot-context XML file.");
			}

			if (connection.getUserId() == null && this.userId == null) {
				throw new IllegalStateException("Unable to parse user ID. You'll need to manually set it in the properties section of the bot-context XML file.");
			}

			return new Bot(this);
		}
	}
}
