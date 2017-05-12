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
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
import oakbot.chat.ChatConnection;
import oakbot.chat.ChatMessage;
import oakbot.chat.ChatMessageHandler;
import oakbot.chat.InvalidCredentialsException;
import oakbot.chat.RoomNotFoundException;
import oakbot.chat.RoomPermissionException;
import oakbot.chat.SplitStrategy;
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
	private final ChatConnection connection;
	private final List<Integer> admins, bannedUsers;
	private final Integer hideOneboxesAfter;
	private final Rooms rooms;
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
	 * Determines if a chat message the bot retrieved from the chat room is a
	 * onebox.
	 */
	private final Predicate<String> oneboxRegex = Pattern.compile("^<div class=\"([^\"]*?)onebox([^\"]*?)\"[^>]*?>").asPredicate();

	/**
	 * <p>
	 * A collection of messages that the bot posted, but have not been "echoed"
	 * back yet through the {@link ChatConnection#listen} method. When a message
	 * is echoed back, it is removed from this map.
	 * </p>
	 * <p>
	 * This is used to determine whether something the bot posted was converted
	 * to a onebox. It is then used to edit the message in order to hide the
	 * onebox.
	 * </p>
	 * <ul>
	 * <li>Key = The message ID.</li>
	 * <li>Value = The raw message content that was sent to the chat room by the
	 * bot (this can be different from what was echoed back).</li>
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
	 * Starts the chat bot. This method blocks until the bot is terminated,
	 * either by an unexpected error or a shutdown command.
	 * @param quiet true to start the bot without broadcasting the greeting
	 * message, false to broadcast the greeting message
	 * @throws InvalidCredentialsException if the login credentials are bad
	 * @throws IOException if there's a network problem
	 */
	public void connect(boolean quiet) throws InvalidCredentialsException, IOException {
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

		startQuoteOfTheDay();

		try {
			connection.listen(new ChatMessageHandler() {
				@Override
				public void onMessage(ChatMessage message) {
					if (message.getContent() == null) {
						//user deleted his/her message, ignore
						return;
					}

					if (bannedUsers.contains(message.getUserId())) {
						//message was posted by a banned user, ignore
						return;
					}

					if (message.getUserId() == userId) {
						//message was posted by this bot

						PostedMessage originalMessage;
						synchronized (postedMessages) {
							originalMessage = postedMessages.remove(message.getMessageId());
						}

						/*
						 * Check to see if the bot posted something that Stack
						 * Overflow Chat converted to a onebox.
						 * 
						 * Stack Overflow Chat converts certain URLs to
						 * "oneboxes". Oneboxes can be fairly large and can spam
						 * the chat. For example, if the message is a URL to an
						 * image, the image itself will be displayed in the chat
						 * room. This is nice, but gets annoying if the image is
						 * large or if it's an animated GIF.
						 * 
						 * After giving people some time to see the onebox, edit
						 * the message so that the onebox no longer displays,
						 * but the URL is still preserved.
						 */
						if (originalMessage != null && hideOneboxesAfter != null && isOnebox(message)) {
							long hideIn = hideOneboxesAfter - (System.currentTimeMillis() - originalMessage.getTimePosted());
							if (logger.isLoggable(Level.INFO)) {
								logger.info("Hiding onebox in " + hideIn + "ms [room=" + message.getRoomId() + ", id=" + message.getMessageId() + "]: " + message.getContent());
							}

							timer.schedule(new TimerTask() {
								@Override
								public void run() {
									try {
										connection.editMessage(message.getRoomId(), message.getMessageId(), "> " + originalMessage.getContent());
									} catch (Exception e) {
										logger.log(Level.SEVERE, "Problem editing chat message [room=" + message.getRoomId() + ", id=" + message.getMessageId() + "]", e);
									}
								}
							}, hideIn);
						}

						return;
					}

					inactiveRoomTasks.reset(message.getRoomId());

					List<ChatResponse> replies = new ArrayList<>();
					boolean isUserAdmin = admins.contains(message.getUserId());
					BotContext context = new BotContext(isUserAdmin, trigger, connection, Bot.this.rooms.getRooms(), Bot.this.rooms.getHomeRooms());

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
									sendMessage(message.getRoomId(), shutdownMessage);
								}
							} catch (IOException e) {
								//ignore
							}
						}
						try {
							connection.close();
						} catch (IOException e) {
							//ignore
						}

						if (database != null) {
							database.commit();
						}

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
							sendMessage(message.getRoomId(), reply);
						}
					}

					for (JoinRoomEvent event : context.getRoomsToJoin()) {
						ChatResponse response = null;
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

						if (response != null) {
							sendMessage(message.getRoomId(), response);
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

				@Override
				public void onMessageEdited(ChatMessage message) {
					onMessage(message);
				}

				@Override
				public void onError(int roomId, Exception thrown) {
					logger.log(Level.SEVERE, "An error occurred getting messages from room " + roomId + ". Leaving room.", thrown);

					try {
						leave(roomId);
					} catch (IOException e) {
						logger.log(Level.SEVERE, "Problem leaving room " + roomId, e);
					}
				}
			});
		} finally {
			timer.cancel();
		}
	}

	private void sendMessage(int roomId, String message) {
		sendMessage(roomId, new ChatResponse(message));
	}

	private void sendMessage(int roomId, ChatResponse reply) {
		final String filteredMessage;
		{
			String messageText = reply.getMessage();
			for (ChatResponseFilter filter : responseFilters) {
				if (filter.isEnabled(roomId)) {
					messageText = filter.filter(messageText);
				}
			}
			filteredMessage = messageText;
		}

		try {
			if (logger.isLoggable(Level.INFO)) {
				logger.info("Sending message [room=" + roomId + "]: " + filteredMessage);
			}

			synchronized (postedMessages) {
				List<Long> messageIds = connection.sendMessage(roomId, filteredMessage, reply.getSplitStrategy());
				long now = System.currentTimeMillis();
				messageIds.forEach((id) -> postedMessages.put(id, new PostedMessage(now, filteredMessage)));
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem sending chat message.", e);
		}
	}

	/**
	 * Determines if a chat message is a onebox.
	 * @param message the chat message
	 * @return true if it's a onebox, false if not
	 */
	private boolean isOnebox(ChatMessage message) {
		return oneboxRegex.test(message.getContent());
	}

	/**
	 * Joins a room.
	 * @param roomId the room ID
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if messages cannot be posted to this room
	 * @throws IOException if there's a problem connecting to the room
	 */
	public void join(int roomId) throws IOException {
		if (rooms.contains(roomId)) {
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
		connection.joinRoom(roomId);
		if (!quiet && greeting != null) {
			sendMessage(roomId, greeting);
		}
		rooms.add(roomId);
		inactiveRoomTasks.reset(roomId);
	}

	/**
	 * Leaves a room.
	 * @param roomId the room ID
	 * @throws IOException if there's a problem leaving the room
	 */
	public void leave(int roomId) throws IOException {
		connection.leaveRoom(roomId);
		rooms.remove(roomId);
		inactiveRoomTasks.cancel(roomId);
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
			return Arrays.asList(unknownCommandHandler.onMessage(chatCommand, context));
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
		/*
		 * Commands can join rooms, so make a copy of the room list to prevent a
		 * concurrent modification exception.
		 */
		List<Integer> roomIds = new ArrayList<>(this.rooms.getRooms());

		for (Integer roomId : roomIds) {
			sendMessage(roomId, new ChatResponse(message, SplitStrategy.WORD));
		}
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
			"Dead chat. :(",
			"*picks nose*",
			"*reads a book*",
			"*computes 10 trillionth digit of pi*"
		};
		//@formatter:on

		private final long waitTime, leaveRoomAfter;
		private final Map<Integer, TimerTask> tasks = new HashMap<>();
		private final Map<Integer, Long> resetTimes = new HashMap<>();

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

		public void reset(int roomId) {
			reset(roomId, true);
		}

		public void reset(int roomId, boolean userPostedMessage) {
			if (rooms.getQuietRooms().contains(roomId)) {
				return;
			}

			TimerTask task = tasks.get(roomId);
			if (task != null) {
				task.cancel();
			}

			task = new TimerTask() {
				@Override
				public void run() {
					long lastReset = resetTimes.get(roomId);
					long elapsed = System.currentTimeMillis() - lastReset;
					if (elapsed > leaveRoomAfter) {
						leaveRoom();
						return;
					}

					String message = Command.random(messages);
					try {
						sendMessage(roomId, message);
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Could not post message to room " + roomId + ".", e);
					}
				}

				private void leaveRoom() {
					try {
						sendMessage(roomId, "*quietly closes door behind him*");
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

			if (userPostedMessage || !resetTimes.containsKey(roomId)) {
				resetTimes.put(roomId, System.currentTimeMillis());
			}
			tasks.put(roomId, task);
			timer.scheduleAtFixedRate(task, waitTime, waitTime);
		}

		public void resetAfterQotd() {
			for (Integer roomId : tasks.keySet()) {
				reset(roomId, false);
			}
		}

		public void cancel(int roomId) {
			TimerTask task = tasks.remove(roomId);
			if (task != null) {
				task.cancel();
			}
			resetTimes.remove(roomId);
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
		private ChatConnection connection;
		private String email, password, userName, trigger = "=", greeting;
		private Integer userId, hideOneboxesAfter;
		private Rooms rooms = new Rooms(Arrays.asList(1), Collections.emptyList());
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

		public Builder connection(ChatConnection connection) {
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
