package oakbot.bot;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import oakbot.Database;
import oakbot.Rooms;
import oakbot.Statistics;
import oakbot.bot.BotContext.JoinRoomEvent;
import oakbot.chat.ChatMessage;
import oakbot.chat.IChatClient;
import oakbot.chat.IRoom;
import oakbot.chat.InvalidCredentialsException;
import oakbot.chat.RoomNotFoundException;
import oakbot.chat.RoomPermissionException;
import oakbot.chat.SplitStrategy;
import oakbot.chat.event.MessageEditedEvent;
import oakbot.chat.event.MessagePostedEvent;
import oakbot.command.Command;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommands;
import oakbot.filter.ChatResponseFilter;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;

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
	private final Statistics stats;
	private final Database database;
	private final UnknownCommandHandler unknownCommandHandler;
	private final Timer timer = new Timer();
	private final InactiveRoomTasks inactiveRoomTasks = new InactiveRoomTasks(TimeUnit.HOURS.toMillis(6), TimeUnit.DAYS.toMillis(3));

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
		responseFilters = builder.responseFilters.build();
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
		}

		Thread thread = new Thread(() -> {
			try {
				startQuoteOfTheDay();

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
			 * Check to see if the bot posted something that Stack Overflow Chat
			 * converted to a onebox.
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
			if (originalMessage != null && hideOneboxesAfter != null && message.isOnebox()) {
				long hideIn = hideOneboxesAfter - (System.currentTimeMillis() - originalMessage.getTimePosted());
				if (logger.isLoggable(Level.INFO)) {
					logger.info("Hiding onebox in " + hideIn + "ms [room=" + message.getRoomId() + ", id=" + message.getMessageId() + "]: " + message.getContent());
				}

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						try {
							room.editMessage(message.getMessageId(), "> " + originalMessage.getContent());
						} catch (Exception e) {
							logger.log(Level.SEVERE, "Problem editing chat message [room=" + message.getRoomId() + ", id=" + message.getMessageId() + "]", e);
						}
					}
				}, hideIn);
			}

			return;
		}

		inactiveRoomTasks.reset(room);

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
				sendMessage(room, reply);
			}
		}

		for (JoinRoomEvent event : context.getRoomsToJoin()) {
			ChatResponse response = null;

			if (maxRooms != null && connection.getRooms().size() >= maxRooms) {
				response = event.ifOther(new IOException("Max rooms reached."));
			} else {
				try {
					join(event.getRoomId());
					response = event.success();
				} catch (RoomNotFoundException e) {
					response = event.ifRoomDoesNotExist();
				} catch (RoomPermissionException e) {
					response = event.ifBotDoesNotHavePermission();
				} catch (IOException e) {
					response = event.ifOther(e);
				}
			}

			if (response != null) {
				sendMessage(room, response);
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

	private void sendMessage(IRoom room, String message) {
		sendMessage(room, new ChatResponse(message));
	}

	private void sendMessage(IRoom room, ChatResponse reply) {
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

		try {
			if (logger.isLoggable(Level.INFO)) {
				logger.info("Sending message [room=" + room.getRoomId() + "]: " + filteredMessage);
			}

			synchronized (postedMessages) {
				List<Long> messageIds = room.sendMessage(filteredMessage, reply.getSplitStrategy());
				long now = System.currentTimeMillis();
				messageIds.forEach((id) -> postedMessages.put(id, new PostedMessage(now, filteredMessage)));
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem sending chat message.", e);
		}
	}

	/**
	 * Joins a room.
	 * @param roomId the room ID
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if messages cannot be posted to this room
	 * @throws IOException if there's a problem connecting to the room
	 */
	private void join(int roomId) throws IOException {
		if (connection.isInRoom(roomId)) {
			return;
		}

		join(roomId, false);
	}

	/**
	 * Joins a room.
	 * @param roomId the room ID
	 * @param quiet true to not post an announcement message, false to post one
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if messages cannot be posted to this room
	 * @throws IOException if there's a problem connecting to the room
	 */
	private void join(int roomId, boolean quiet) throws RoomNotFoundException, RoomPermissionException, IOException {
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
		inactiveRoomTasks.reset(room);
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
		inactiveRoomTasks.cancel(room);
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
	private void broadcast(String message) throws IOException {
		for (IRoom room : connection.getRooms()) {
			sendMessage(room, new ChatResponse(message, SplitStrategy.WORD));
		}
	}

	/**
	 * Terminates the bot. This method blocks until it finishes processing all
	 * existing messages in its queue.
	 */
	public void stop() {
		newMessages.add(CLOSE_MESSAGE);
	}

	private void startQuoteOfTheDay() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.add(Calendar.DAY_OF_MONTH, 1);
		Date firstQuote = c.getTime();

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				String quote, author, permalink;
				try {
					JsonNode response = getResponse();
					JsonNode quoteNode = response.get("contents").get("quotes").get(0);

					quote = quoteNode.get("quote").asText();
					author = quoteNode.get("author").asText();
					permalink = quoteNode.get("permalink").asText();
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error querying quote API.", e);
					return;
				}

				ChatBuilder cb = new ChatBuilder();
				cb.italic().append('"').append(quote).append('"').italic();
				cb.append(" -").append(author);
				cb.append(' ').link("(source)", permalink);

				try {
					broadcast(cb.toString());
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error broadcasting quote.", e);
				}

				inactiveRoomTasks.resetAfterQotd();
			}

			private JsonNode getResponse() throws IOException {
				ObjectMapper mapper = new ObjectMapper();
				HttpGet request = new HttpGet("http://quotes.rest/qod.json");
				try (CloseableHttpClient client = HttpClients.createDefault()) {
					try (CloseableHttpResponse response = client.execute(request)) {
						try (InputStream in = response.getEntity().getContent()) {
							return mapper.readTree(in);
						}
					}
				}
			}

			//@formatter:off
			/*
			private JsonNode getResponse() throws IOException {
				byte[] b = Files.readAllBytes(Paths.get("quotes.rest-example.json"));
				ObjectMapper mapper = new ObjectMapper();
				return mapper.readTree(b);
			}
			*/
			//@formatter:on
		}, firstQuote, TimeUnit.DAYS.toMillis(1));
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
		private final Map<IRoom, Long> resetTimes = new HashMap<>();

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

		public void reset(IRoom room) {
			reset(room, true);
		}

		public void reset(IRoom room, boolean userPostedMessage) {
			int roomId = room.getRoomId();
			if (rooms.getQuietRooms().contains(roomId)) {
				return;
			}

			TimerTask task = tasks.get(room);
			if (task != null) {
				task.cancel();
			}

			task = new TimerTask() {
				@Override
				public void run() {
					long lastReset = resetTimes.get(room);
					long elapsed = System.currentTimeMillis() - lastReset;
					if (elapsed > leaveRoomAfter) {
						leaveRoom();
						return;
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
						sendMessage(room, "*quietly closes door behind him*");
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

			if (userPostedMessage || !resetTimes.containsKey(room)) {
				resetTimes.put(room, System.currentTimeMillis());
			}
			tasks.put(room, task);
			timer.scheduleAtFixedRate(task, waitTime, waitTime);
		}

		public void resetAfterQotd() {
			for (IRoom room : tasks.keySet()) {
				reset(room, false);
			}
		}

		public void cancel(IRoom room) {
			TimerTask task = tasks.remove(room);
			if (task != null) {
				task.cancel();
			}
			resetTimes.remove(room);
		}
	}

	private static class PostedMessage {
		private final long timePosted;
		private final String content;

		public PostedMessage(long timePosted, String content) {
			this.timePosted = timePosted;
			this.content = content;
		}

		public long getTimePosted() {
			return timePosted;
		}

		public String getContent() {
			return content;
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
