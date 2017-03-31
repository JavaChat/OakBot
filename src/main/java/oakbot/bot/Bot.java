package oakbot.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;

import oakbot.Database;
import oakbot.Rooms;
import oakbot.Statistics;
import oakbot.chat.ChatConnection;
import oakbot.chat.ChatMessage;
import oakbot.chat.InvalidCredentialsException;
import oakbot.chat.RoomNotFoundException;
import oakbot.chat.RoomPermissionException;
import oakbot.command.Command;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommands;
import oakbot.filter.ChatResponseFilter;
import oakbot.listener.Listener;

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
	private final Rooms rooms;
	private final List<Command> commands;
	private final LearnedCommands learnedCommands;
	private final List<Listener> listeners;
	private final List<ChatResponseFilter> responseFilters;
	private final Statistics stats;
	private final Database database;
	private final UnknownCommandHandler unknownCommandHandler;
	private final Pattern commandRegex;

	private Bot(Builder builder) {
		connection = builder.connection;
		email = builder.email;
		password = builder.password;
		userName = builder.userName;
		userId = builder.userId;
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
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Could not join room " + room + ".", e);
				this.rooms.remove(room);
			}
		}

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

			List<ChatResponse> replies = new ArrayList<>();
			boolean isUserAdmin = admins.contains(message.getUserId());

			try {
				replies.addAll(handleListeners(message, isUserAdmin));

				ChatCommand command = asCommand(message);
				if (command != null) {
					replies.addAll(handleCommands(command, isUserAdmin));
				}
			} catch (ShutdownException e) {
				try {
					broadcast("Shutting down.  See you later.");
				} catch (IOException e2) {
					//ignore
				}
				try {
					connection.close();
				} catch (IOException e2) {
					//ignore
				}
				return;
			}

			if (replies.isEmpty()) {
				return;
			}

			if (logger.isLoggable(Level.INFO)) {
				logger.info("Responding to: [#" + message.getMessageId() + "] [" + message.getTimestamp() + "] " + message.getContent());
			}

			if (stats != null) {
				stats.incMessagesRespondedTo(replies.size());
			}

			for (ChatResponse reply : replies) {
				String messageText = reply.getMessage();
				for (ChatResponseFilter filter : responseFilters) {
					if (filter.isEnabled(message.getRoomId())) {
						messageText = filter.filter(messageText);
					}
				}

				try {
					connection.sendMessage(message.getRoomId(), messageText, reply.getSplitStrategy());
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Problem sending chat message.", e);
				}
			}

			if (database != null) {
				database.commit();
			}
		});
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
	private void join(int roomId, boolean quiet) throws IOException {
		connection.joinRoom(roomId);
		if (!quiet && greeting != null) {
			connection.sendMessage(roomId, greeting);
		}
		rooms.add(roomId);
	}

	/**
	 * Leaves a room.
	 * @param roomId the room ID
	 */
	public void leave(int roomId) throws IOException {
		connection.leaveRoom(roomId);
		rooms.remove(roomId);
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

	private List<ChatResponse> handleListeners(ChatMessage message, boolean isAdmin) {
		List<ChatResponse> replies = new ArrayList<>();
		for (Listener listener : listeners) {
			try {
				ChatResponse reply = listener.onMessage(message, isAdmin);
				if (reply != null) {
					replies.add(reply);
				}
			} catch (ShutdownException e) {
				throw e;
			} catch (Exception e) {
				logger.log(Level.SEVERE, "An error occurred responding to a message.", e);
			}
		}
		return replies;
	}

	private List<ChatResponse> handleCommands(ChatCommand chatCommand, boolean isAdmin) {
		List<Command> commands = getCommands(chatCommand.getCommandName());
		if (commands.isEmpty()) {
			if (unknownCommandHandler == null) {
				return Collections.emptyList();
			}
			return Arrays.asList(unknownCommandHandler.onMessage(chatCommand, isAdmin, this));
		}

		List<ChatResponse> replies = new ArrayList<>(commands.size());
		for (Command command : commands) {
			try {
				ChatResponse reply = command.onMessage(chatCommand, isAdmin, this);
				if (reply != null) {
					replies.add(reply);
				}
			} catch (ShutdownException e) {
				throw e;
			} catch (Exception e) {
				logger.log(Level.SEVERE, "An error occurred responding to a command.", e);
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
			connection.sendMessage(room, message);
		}
	}

	/**
	 * Builds {@link Bot} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private ChatConnection connection;
		private String email, password, userName, trigger = "=", greeting;
		private Integer userId;
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
