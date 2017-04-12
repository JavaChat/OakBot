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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
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
	private final Integer hideImagesAfter;
	private final Rooms rooms;
	private final List<Command> commands;
	private final LearnedCommands learnedCommands;
	private final List<Listener> listeners;
	private final List<ChatResponseFilter> responseFilters;
	private final Statistics stats;
	private final Database database;
	private final UnknownCommandHandler unknownCommandHandler;
	private final Pattern commandRegex;
	private final Timer timer = new Timer();
	private final InactiveRoomTasks inactiveRoomTasks = new InactiveRoomTasks(TimeUnit.HOURS.toMillis(6));
	private final Pattern imageUrlRegex = Pattern.compile("^https?://[^\\s]*?\\.(jpg|jpeg|png|gif)$", Pattern.CASE_INSENSITIVE);

	private Bot(Builder builder) {
		connection = builder.connection;
		email = builder.email;
		password = builder.password;
		userName = builder.userName;
		userId = builder.userId;
		hideImagesAfter = builder.hideImagesAfter;
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
		commandRegex = Pattern.compile("^" + Pattern.quote(trigger) + "\\s*(.*?)(\\s+(.*)|$)");
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
			connection.listen((message) -> {
				if (message.getContent() == null) {
					//user deleted his/her message, ignore
					return;
				}

				if (message.getUserId() == userId) {
					//message was posted by this bot, ignore
					return;
				}

				if (bannedUsers.contains(message.getUserId())) {
					//message was posted by a banned user, ignore
					return;
				}

				inactiveRoomTasks.reset(message.getRoomId());

				List<ChatResponse> replies = new ArrayList<>();
				boolean isUserAdmin = admins.contains(message.getUserId());
				BotContext context = new BotContext(isUserAdmin, trigger, connection, this.rooms.getRooms(), this.rooms.getHomeRooms());

				replies.addAll(handleListeners(message, context));

				ChatCommand command = asCommand(message);
				if (command != null) {
					replies.addAll(handleCommands(command, context));
				}

				if (context.isShutdown()) {
					String shutdownMessage = context.getShutdownMessage();
					if (shutdownMessage != null) {
						try {
							broadcast(shutdownMessage);
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
						logger.info("Responding to: [#" + message.getMessageId() + "] [" + message.getTimestamp() + "] " + message.getContent());
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
			});
		} finally {
			timer.cancel();
		}
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
			List<Long> messageIds = connection.sendMessage(roomId, filteredMessage, reply.getSplitStrategy());

			/*
			 * If the message is a URL to an image, SO chat will display the
			 * image in the chat room. This is nice, but gets annoying if the
			 * image is large or if it's an animated GIF.
			 * 
			 * After giving people some time to see the image, edit these
			 * messages so that the image no longer displays, but the URL is
			 * still preserved.
			 */
			if (hideImagesAfter != null && isImageUrl(filteredMessage)) {
				long messageId = messageIds.get(0);
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						try {
							connection.editMessage(roomId, messageId, "> " + filteredMessage);
						} catch (Exception e) {
							logger.log(Level.SEVERE, "Problem editing chat message " + messageId + " in room " + roomId + ".", e);
						}
					}
				}, hideImagesAfter);
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem sending chat message.", e);
		}
	}

	/**
	 * Determines if a chat message consists of a URL to an image.
	 * @param message the chat message
	 * @return true if it is a URL to an image, false if not
	 */
	private boolean isImageUrl(String message) {
		return imageUrlRegex.matcher(message).find();
	}

	/**
	 * Tests to see if a chat message is in the format of a command, and if it
	 * is, parses it as such.
	 * @param message the chat message
	 * @return the chat command if null if the message is not a command
	 */
	private ChatCommand asCommand(ChatMessage message) {
		String content = message.getContent();
		Matcher matcher = commandRegex.matcher(content);
		if (!matcher.find()) {
			return null;
		}

		String name = matcher.group(1);
		String text = matcher.group(3);
		if (text == null) {
			text = "";
		}
		return new ChatCommand(message, name, text);
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
			connection.sendMessage(roomId, greeting);
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
		List<Integer> rooms = new ArrayList<>(this.rooms.getRooms());
		for (Integer room : rooms) {
			connection.sendMessage(room, message, SplitStrategy.WORD);
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

				inactiveRoomTasks.resetAll();
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
			"*reads a book*"
		};
		//@formatter:on

		private final long waitTime;
		private final Map<Integer, TimerTask> tasks = new HashMap<>();

		public InactiveRoomTasks(long waitTime) {
			this.waitTime = waitTime;
		}

		public void reset(int roomId) {
			TimerTask task = tasks.get(roomId);
			if (task != null) {
				task.cancel();
			}

			task = new TimerTask() {
				@Override
				public void run() {
					int rand = (int) (Math.random() * messages.length);
					String message = messages[rand];
					try {
						connection.sendMessage(roomId, message);
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Could not post message to room " + roomId + ".", e);
					}
				}
			};

			tasks.put(roomId, task);
			timer.scheduleAtFixedRate(task, waitTime, waitTime);
		}

		public void resetAll() {
			for (Integer roomId : tasks.keySet()) {
				reset(roomId);
			}
		}

		public void cancel(int roomId) {
			TimerTask task = tasks.remove(roomId);
			if (task != null) {
				task.cancel();
			}
		}
	}

	/**
	 * Builds {@link Bot} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private ChatConnection connection;
		private String email, password, userName, trigger = "=", greeting;
		private Integer userId, hideImagesAfter;
		private Rooms rooms = new Rooms(Arrays.asList(1));
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

		public Builder hideImagesAfter(Integer hideImagesAfter) {
			this.hideImagesAfter = hideImagesAfter;
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
			return rooms(new Rooms(Arrays.asList(roomIds)));
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
