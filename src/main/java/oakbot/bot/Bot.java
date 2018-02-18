package oakbot.bot;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;

import oakbot.Database;
import oakbot.Rooms;
import oakbot.Statistics;
import oakbot.bot.BotContext.JoinRoomCallback;
import oakbot.chat.ChatMessage;
import oakbot.chat.IChatClient;
import oakbot.chat.IRoom;
import oakbot.chat.InvalidCredentialsException;
import oakbot.chat.RoomNotFoundException;
import oakbot.chat.RoomPermissionException;
import oakbot.chat.event.MessageEditedEvent;
import oakbot.chat.event.MessagePostedEvent;
import oakbot.command.Command;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommands;
import oakbot.filter.ChatResponseFilter;
import oakbot.listener.Listener;
import oakbot.task.ScheduledTask;

/**
 * A Stackoverflow chat bot.
 * @author Michael Angstadt
 */
public class Bot {
	private static final Logger logger = Logger.getLogger(Bot.class.getName());

	private final String email, password, userName, trigger, greeting;
	private final Integer userId;
	private final IChatClient connection;
	private final BlockingQueue<ChatMessage> newMessages = new LinkedBlockingQueue<>();
	private final ChatMessage CLOSE_MESSAGE = new ChatMessage.Builder().build();
	private final List<Integer> admins, bannedUsers;
	private final Integer hideOneboxesAfter;
	private final Rooms rooms;
	private final Integer maxRooms;
	private final List<Command> commands;
	private final LearnedCommands learnedCommands;
	private final List<Listener> listeners;
	private final List<ChatResponseFilter> responseFilters;
	private final List<ScheduledTask> scheduledTasks;
	private final Statistics stats;
	private final Database database;
	private final UnknownCommandHandler unknownCommandHandler;
	private final Timer timer = new Timer();
	private final InactiveRoomTasks inactiveRoomTasks = new InactiveRoomTasks(Duration.ofHours(6).toMillis(), Duration.ofDays(3).toMillis());

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
		connection = builder.connection;
		email = builder.email;
		password = builder.password;
		userName = builder.userName;
		userId = builder.userId;
		hideOneboxesAfter = builder.hideOneboxesAfter;
		trigger = builder.trigger;
		greeting = builder.greeting;
		rooms = builder.rooms;
		maxRooms = builder.maxRooms;
		admins = builder.admins.build();
		bannedUsers = builder.bannedUsers.build();
		stats = builder.stats;
		database = builder.database;
		unknownCommandHandler = builder.unknownCommandHandler;
		commands = builder.commands.build();
		learnedCommands = builder.learnedCommands;
		listeners = builder.listeners.build();
		scheduledTasks = builder.tasks.build();
		responseFilters = builder.responseFilters.build();
	}

	private void scheduleTask(ScheduledTask task) {
		long nextRun = task.nextRun();
		if (nextRun <= 0) {
			return;
		}

		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					task.run(Bot.this);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Problem running task " + task.getClass().getSimpleName() + ".", e);
				}
				scheduleTask(task);
			}
		}, nextRun);
	}

	/**
	 * Starts the chat bot.
	 * @param quiet true to start the bot without broadcasting the greeting
	 * message, false to broadcast the greeting message
	 * @return the thread that the bot is running in. This thread will terminate
	 * when the bot terminates
	 * @throws InvalidCredentialsException if the login credentials are bad
	 * @throws IOException if there's a network problem
	 */
	public Thread connect(boolean quiet) throws InvalidCredentialsException, IOException {
		//login
		logger.info("Logging in as " + email + "...");
		connection.login(email, password);

		//connect to each room
		List<Integer> rooms = new ArrayList<>(this.rooms.getRooms());
		for (Integer room : rooms) {
			try {
				join(room, quiet);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Could not join room " + room + ". Removing from rooms list.", e);
				this.rooms.remove(room);
			}

			/*
			 * Insert a pause between joining each room in an attempt to resolve
			 * an issue where the bot chooses to ignore all messages in certain
			 * rooms.
			 */
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				//ignore
			}
		}

		Thread thread = new Thread(() -> {
			try {
				for (ScheduledTask task : scheduledTasks) {
					scheduleTask(task);
				}
				scheduleNextRoomCheck();

				while (true) {
					ChatMessage message;
					try {
						message = newMessages.take();
					} catch (InterruptedException e) {
						break;
					}

					if (message == CLOSE_MESSAGE) {
						break;
					}

					handleMessage(message);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Bot terminated due to unexpected exception.", e);
			} finally {
				try {
					connection.close();
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Problem closing ChatClient connection.", e);
				}

				if (database != null) {
					database.commit();
				}

				timer.cancel();
			}
		});

		thread.start();

		return thread;
	}

	private void handleMessage(ChatMessage message) {
		if (message.getContent() == null) {
			//user deleted his/her message, ignore
			return;
		}

		if (bannedUsers.contains(message.getUserId())) {
			//message was posted by a banned user, ignore
			return;
		}

		IRoom room = connection.getRoom(message.getRoomId());
		if (room == null) {
			//no longer in the room
			return;
		}

		if (message.getUserId() == userId) {
			//message was posted by this bot

			PostedMessage originalMessage;
			synchronized (postedMessages) {
				originalMessage = postedMessages.remove(message.getMessageId());
			}

			/*
			 * Check to see if the message should be edited for brevity after a
			 * short time so it doesn't spam the chat history.
			 * 
			 * This could happen if (1) the bot posted something that Stack
			 * Overflow Chat converted to a onebox (e.g. an image) or (2) the
			 * message itself has asked to be edited (e.g. a javadoc
			 * description).
			 * 
			 * Stack Overflow Chat converts certain URLs to "oneboxes". Oneboxes
			 * can be fairly large and can spam the chat. For example, if the
			 * message is a URL to an image, the image itself will be displayed
			 * in the chat room. This is nice, but gets annoying if the image is
			 * large or if it's an animated GIF.
			 * 
			 * After giving people some time to see the onebox, edit the message
			 * so that the onebox no longer displays, but the URL is still
			 * preserved.
			 */
			if (originalMessage != null && hideOneboxesAfter != null && (message.getContent().isOnebox() || originalMessage.hide())) {
				long hideIn = hideOneboxesAfter - (System.currentTimeMillis() - originalMessage.getTimePosted());
				if (logger.isLoggable(Level.INFO)) {
					String action = message.getContent().isOnebox() ? "Hiding onebox" : "Condensing message";
					logger.info(action + " in " + hideIn + "ms [room=" + message.getRoomId() + ", id=" + message.getMessageId() + "]: " + message.getContent());
				}

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						try {
							room.editMessage(message.getMessageId(), "> " + originalMessage.getContent());
							for (Long id : originalMessage.getRelatedMessageIds()) {
								room.deleteMessage(id);
							}
						} catch (Exception e) {
							logger.log(Level.SEVERE, "Problem editing chat message [room=" + message.getRoomId() + ", id=" + message.getMessageId() + "]", e);
						}
					}
				}, hideIn);
			}

			return;
		}

		inactiveRoomTasks.resetTimer(room);

		List<ChatResponse> replies = new ArrayList<>();
		boolean isUserAdmin = admins.contains(message.getUserId());
		BotContext context = new BotContext(isUserAdmin, trigger, connection, rooms.getRooms(), rooms.getHomeRooms(), maxRooms);

		replies.addAll(handleListeners(message, context));

		ChatCommand command = ChatCommand.fromMessage(message, trigger);
		if (command != null) {
			replies.addAll(handleCommands(command, context));
		}

		if (context.isShutdown()) {
			String shutdownMessage = context.getShutdownMessage();
			if (shutdownMessage != null) {
				try {
					if (context.isShutdownMessageBroadcast()) {
						broadcast(shutdownMessage);
					} else {
						sendMessage(room, shutdownMessage);
					}
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Problem sending shutdown message.", e);
				}
			}

			newMessages.clear();
			stop();

			return;
		}

		if (!replies.isEmpty()) {
			if (logger.isLoggable(Level.INFO)) {
				logger.info("Responding to message [room=" + message.getRoomId() + ", user=" + message.getUsername() + ", id=" + message.getMessageId() + "]: " + message.getContent());
			}

			if (stats != null) {
				stats.incMessagesRespondedTo(replies.size());
			}

			for (ChatResponse reply : replies) {
				try {
					sendMessage(room, reply);
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Problem posting message [room=" + room.getRoomId() + "]: " + reply.getMessage(), e);
				}
			}
		}

		for (Map.Entry<Integer, JoinRoomCallback> entry : context.getRoomsToJoin().entrySet()) {
			int roomId = entry.getKey();
			JoinRoomCallback callback = entry.getValue();

			ChatResponse response = null;

			if (maxRooms != null && connection.getRooms().size() >= maxRooms) {
				response = callback.ifOther(new IOException("Max rooms reached."));
			} else {
				try {
					IRoom joinedRoom = join(roomId);
					if (joinedRoom.canPost()) {
						response = callback.success();
					} else {
						response = callback.ifBotDoesNotHavePermission();
						try {
							leave(roomId);
						} catch (IOException e) {
							logger.log(Level.SEVERE, "Problem leaving room after it was found that the bot can't post messages to it.", e);
						}
					}
				} catch (RoomNotFoundException e) {
					response = callback.ifRoomDoesNotExist();
				} catch (IOException e) {
					response = callback.ifOther(e);
				}
			}

			if (response != null) {
				try {
					sendMessage(room, response);
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Problem posting leave message [room=" + room.getRoomId() + "]: " + response, e);
				}
			}
		}

		for (Integer roomId : context.getRoomsToLeave()) {
			try {
				leave(roomId);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Problem leaving room " + roomId, e);
			}
		}

		if (database != null) {
			database.commit();
		}
	}

	/**
	 * Posts a message to a room. If the bot has not joined the given room, then
	 * it will not post anything.
	 * @param roomId the room ID
	 * @param message the message to post
	 * @throws IOException if there's a problem sending the message
	 */
	public void sendMessage(int roomId, ChatResponse message) throws IOException {
		IRoom room = connection.getRoom(roomId);
		if (room != null) {
			sendMessage(room, message);
		}
	}

	private void sendMessage(IRoom room, String message) throws IOException {
		sendMessage(room, new ChatResponse(message));
	}

	private void sendMessage(IRoom room, ChatResponse reply) throws IOException {
		final String filteredMessage;
		if (reply.isBypassFilters()) {
			filteredMessage = reply.getMessage();
		} else {
			String messageText = reply.getMessage();
			for (ChatResponseFilter filter : responseFilters) {
				if (filter.isEnabled(room.getRoomId())) {
					messageText = filter.filter(messageText);
				}
			}
			filteredMessage = messageText;
		}

		if (logger.isLoggable(Level.INFO)) {
			logger.info("Sending message [room=" + room.getRoomId() + "]: " + filteredMessage);
		}

		synchronized (postedMessages) {
			List<Long> messageIds = room.sendMessage(filteredMessage, reply.getSplitStrategy());
			long now = System.currentTimeMillis();

			String hideMessage = reply.getHideMessage();
			boolean hide = (hideMessage != null);
			if (!hide) {
				hideMessage = filteredMessage;
			}

			PostedMessage postedMessage = new PostedMessage(now, hideMessage, hide, messageIds.subList(1, messageIds.size()));
			postedMessages.put(messageIds.get(0), postedMessage);
		}
	}

	/**
	 * Joins a room.
	 * @param roomId the room ID
	 * @return the connection to the room
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if messages cannot be posted to this room
	 * @throws IOException if there's a problem connecting to the room
	 */
	private IRoom join(int roomId) throws IOException {
		IRoom room = connection.getRoom(roomId);
		if (room != null) {
			return room;
		}

		return join(roomId, false);
	}

	/**
	 * Joins a room.
	 * @param roomId the room ID
	 * @param quiet true to not post an announcement message, false to post one
	 * @return the connection to the room
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws IOException if there's a problem connecting to the room
	 */
	private IRoom join(int roomId, boolean quiet) throws RoomNotFoundException, IOException {
		logger.info("Joining room " + roomId + "...");
		IRoom room = connection.joinRoom(roomId);

		room.addEventListener(MessagePostedEvent.class, (event) -> {
			newMessages.add(event.getMessage());
		});
		room.addEventListener(MessageEditedEvent.class, (event) -> {
			newMessages.add(event.getMessage());
		});

		if (!quiet && greeting != null) {
			sendMessage(room, greeting);
		}

		rooms.add(roomId);
		inactiveRoomTasks.resetTimer(room);

		return room;
	}

	/**
	 * Leaves a room.
	 * @param roomId the room ID
	 * @throws IOException if there's a problem leaving the room
	 */
	private void leave(int roomId) throws IOException {
		logger.info("Leaving room " + roomId + "...");
		IRoom room = connection.getRoom(roomId);
		if (room == null) {
			return;
		}

		room.leave();
		rooms.remove(roomId);
		inactiveRoomTasks.cancelTimer(room);
	}

	/**
	 * Gets the bot's command trigger.
	 * @return the trigger (e.g. "/")
	 */
	public String getTrigger() {
		return trigger;
	}

	/**
	 * Gets the rooms that the bot is connected to.
	 * @return the room IDs
	 */
	public Rooms getRooms() {
		return rooms;
	}

	private List<ChatResponse> handleListeners(ChatMessage message, BotContext context) {
		List<ChatResponse> replies = new ArrayList<>();
		for (Listener listener : listeners) {
			try {
				ChatResponse reply = listener.onMessage(message, context);
				if (reply != null) {
					replies.add(reply);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, "A listener threw an exception responding to a message.", e);
			}
		}
		return replies;
	}

	private List<ChatResponse> handleCommands(ChatCommand chatCommand, BotContext context) {
		List<Command> commands = getCommands(chatCommand.getCommandName());
		if (commands.isEmpty()) {
			if (unknownCommandHandler == null) {
				return Collections.emptyList();
			}

			ChatResponse response = unknownCommandHandler.onMessage(chatCommand, context);
			return (response == null) ? Collections.emptyList() : Arrays.asList(response);
		}

		List<ChatResponse> replies = new ArrayList<>(commands.size());
		for (Command command : commands) {
			try {
				ChatResponse reply = command.onMessage(chatCommand, context);
				if (reply != null) {
					replies.add(reply);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, "A command threw an exception responding to a message.", e);
			}
		}
		return replies;
	}

	/**
	 * Gets all commands that have a given name.
	 * @param name the command name
	 * @return the matching commands
	 */
	private List<Command> getCommands(String name) {
		List<Command> result = new ArrayList<>();
		for (Command command : commands) {
			if (command.name().equals(name) || command.aliases().contains(name)) {
				result.add(command);
			}
		}
		for (LearnedCommand command : learnedCommands) {
			if (command.name().equals(name) || command.aliases().contains(name)) {
				result.add(command);
			}
		}
		return result;
	}

	/**
	 * Sends a message to all the chat rooms the bot is logged into.
	 * @param message the message to send
	 * @throws IOException if there's a problem sending the message
	 */
	public void broadcast(String message) throws IOException {
		broadcast(new ChatResponse(message));
	}

	/**
	 * Sends a message to all the chat rooms the bot is logged into.
	 * @param message the message to send
	 * @throws IOException if there's a problem sending the message
	 */
	public void broadcast(ChatResponse message) throws IOException {
		for (IRoom room : connection.getRooms()) {
			sendMessage(room, message);
		}
	}

	/**
	 * Terminates the bot. This method blocks until it finishes processing all
	 * existing messages in its queue.
	 */
	public void stop() {
		newMessages.add(CLOSE_MESSAGE);
	}

	private void scheduleNextRoomCheck() {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Instant now = Instant.now();
				for (IRoom room : connection.getRooms()) {
					long hours = ChronoUnit.HOURS.between(room.getTimeOfLastReceivedMessagePostedEvent(), now);
					if (hours > 5) {
						logger.info("No \"message posted\" events have been received in over " + hours + " for room " + room.getRoomId());
					}
				}

				scheduleNextRoomCheck();
			}
		}, Duration.ofHours(6).toMillis());
	}

	private class InactiveRoomTasks {
		//@formatter:off
		private final String[] messages = {
			"*farts*",
			"*picks nose*",
			"*reads a book*",
			"*dreams of electric sheep*"
		};
		//@formatter:on

		private final long waitTime, leaveRoomAfter;
		private final Map<IRoom, TimerTask> tasks = new HashMap<>();

		/**
		 * @param waitTime how long to wait in between messages (in
		 * milliseconds)
		 * @param leaveRoomAfter how long to wait before the bot leaves the room
		 * (in milliseconds)
		 */
		public InactiveRoomTasks(long waitTime, long leaveRoomAfter) {
			this.waitTime = waitTime;
			this.leaveRoomAfter = leaveRoomAfter;
		}

		public void resetTimer(IRoom room) {
			int roomId = room.getRoomId();
			if (rooms.getQuietRooms().contains(roomId)) {
				return;
			}

			cancelTimer(room);

			TimerTask task = new TimerTask() {
				private long taskStarted = System.currentTimeMillis();

				@Override
				public void run() {
					boolean isHomeRoom = rooms.getHomeRooms().contains(roomId);
					if (!isHomeRoom) {
						long sinceLastMessage = System.currentTimeMillis() - taskStarted;
						if (sinceLastMessage > leaveRoomAfter) {
							leaveRoom();
							return;
						}
					}

					String message = Command.random(messages);
					try {
						sendMessage(room, message);
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Could not post message to room " + roomId + ".", e);
					}
				}

				private void leaveRoom() {
					try {
						sendMessage(room, "*quietly closes the door behind him*");
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Could not post message to room " + roomId + ".", e);
					}

					try {
						leave(roomId);
					} catch (IOException e) {
						logger.log(Level.SEVERE, "Could not leave room " + roomId + ".", e);
					}
				}
			};

			tasks.put(room, task);
			timer.scheduleAtFixedRate(task, waitTime, waitTime);
		}

		public void cancelTimer(IRoom room) {
			TimerTask task = tasks.remove(room);
			if (task != null) {
				task.cancel();
			}
		}
	}

	private static class PostedMessage {
		private final long timePosted;
		private final String content;
		private final boolean hide;
		private final List<Long> relatedMessageIds;

		public PostedMessage(long timePosted, String content, boolean hide, List<Long> relatedMessageIds) {
			this.timePosted = timePosted;
			this.content = content;
			this.hide = hide;
			this.relatedMessageIds = relatedMessageIds;
		}

		public long getTimePosted() {
			return timePosted;
		}

		public String getContent() {
			return content;
		}

		public boolean hide() {
			return hide;
		}

		public List<Long> getRelatedMessageIds() {
			return relatedMessageIds;
		}
	}

	/**
	 * Builds {@link Bot} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private IChatClient connection;
		private String email, password, userName, trigger = "=", greeting;
		private Integer userId, hideOneboxesAfter;
		private Rooms rooms = new Rooms(Arrays.asList(1), Collections.emptyList());
		private Integer maxRooms;
		private ImmutableList.Builder<Integer> admins = ImmutableList.builder();
		private ImmutableList.Builder<Integer> bannedUsers = ImmutableList.builder();
		private ImmutableList.Builder<Command> commands = ImmutableList.builder();
		private LearnedCommands learnedCommands = new LearnedCommands();
		private ImmutableList.Builder<Listener> listeners = ImmutableList.builder();
		private ImmutableList.Builder<ScheduledTask> tasks = ImmutableList.builder();
		private ImmutableList.Builder<ChatResponseFilter> responseFilters = ImmutableList.builder();
		private Statistics stats;
		private Database database;
		private UnknownCommandHandler unknownCommandHandler;

		public Builder login(String email, String password) {
			this.email = email;
			this.password = password;
			return this;
		}

		public Builder connection(IChatClient connection) {
			this.connection = connection;
			return this;
		}

		public Builder user(String userName, Integer userId) {
			this.userName = (userName == null || userName.isEmpty()) ? null : userName;
			this.userId = userId;
			return this;
		}

		public Builder hideOneboxesAfter(Integer hideOneboxesAfter) {
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

		public Builder rooms(Rooms rooms) {
			this.rooms = rooms;
			return this;
		}

		public Builder rooms(Integer... roomIds) {
			return rooms(new Rooms(Arrays.asList(roomIds), Collections.emptyList()));
		}

		public Builder maxRooms(Integer maxRooms) {
			this.maxRooms = maxRooms;
			return this;
		}

		public Builder admins(Integer... admins) {
			return admins(Arrays.asList(admins));
		}

		public Builder admins(Collection<Integer> admins) {
			this.admins.addAll(admins);
			return this;
		}

		public Builder bannedUsers(Integer... bannedUsers) {
			return bannedUsers(Arrays.asList(bannedUsers));
		}

		public Builder bannedUsers(Collection<Integer> bannedUsers) {
			this.bannedUsers.addAll(bannedUsers);
			return this;
		}

		public Builder commands(Command... commands) {
			return commands(Arrays.asList(commands));
		}

		public Builder commands(Collection<Command> commands) {
			this.commands.addAll(commands);
			return this;
		}

		public Builder learnedCommands(LearnedCommands commands) {
			this.learnedCommands = commands;
			return this;
		}

		public Builder listeners(Listener... listeners) {
			return listeners(Arrays.asList(listeners));
		}

		public Builder listeners(Collection<Listener> listeners) {
			this.listeners.addAll(listeners);
			return this;
		}

		public Builder tasks(ScheduledTask... tasks) {
			return tasks(Arrays.asList(tasks));
		}

		public Builder tasks(Collection<ScheduledTask> tasks) {
			this.tasks.addAll(tasks);
			return this;
		}

		public Builder responseFilters(ChatResponseFilter... filters) {
			return responseFilters(Arrays.asList(filters));
		}

		public Builder responseFilters(Collection<ChatResponseFilter> filters) {
			this.responseFilters.addAll(filters);
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

		public Builder unknownCommandHandler(UnknownCommandHandler unknownCommandHandler) {
			this.unknownCommandHandler = unknownCommandHandler;
			return this;
		}

		public Bot build() {
			if (connection == null) {
				throw new IllegalStateException("No ChatConnection given.");
			}
			return new Bot(this);
		}
	}
}
