package oakbot.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.chat.ChatConnection;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * A Stackoverflow chat bot.
 * @author Michael Angstadt
 */
public class Bot {
	private static final Logger logger = Logger.getLogger(Bot.class.getName());

	private final String email, password, name, trigger;
	private final ChatConnection connection;
	private final int heartbeat;
	private final List<Integer> rooms, admins;
	private final List<Command> commands;
	private final Map<Integer, Long> prevMessageIds = new HashMap<>();

	private Bot(Builder builder) {
		connection = builder.connection;
		email = builder.email;
		password = builder.password;
		name = builder.name;
		trigger = builder.trigger;
		heartbeat = builder.heartbeat;
		rooms = builder.rooms;
		admins = builder.admins;
		commands = builder.commands;
	}

	/**
	 * Starts the chat bot. This call is blocking.
	 * @throws IOException if there's an I/O problem
	 */
	public void connect() throws IOException {
		//login
		connection.login(email, password);

		//post a message to each room
		for (Integer room : rooms) {
			connection.sendMessage(room, "OakBot Online.");
		}

		//listen for and reply to messages
		Pattern contentRegex = Pattern.compile("^" + Pattern.quote(trigger) + "\\s*(.*?)(\\s+(.*)|$)");
		while (true) {
			long start = System.currentTimeMillis();

			for (Integer room : rooms) {
				logger.fine("Pinging room " + room);

				//get new messages since last ping
				List<ChatMessage> messages = connection.getNewMessages(room);
				logger.fine(messages.size() + " new messages found.");
				if (messages.isEmpty()) {
					continue;
				}

				String mentionToLower, compressedMentionToLower;
				mentionToLower = compressedMentionToLower = null;
				if (name != null) {
					mentionToLower = "@" + name.toLowerCase();
					compressedMentionToLower = mentionToLower.replace(" ", "");
				}

				for (ChatMessage message : messages) {
					String content = message.getContent();
					if (content == null) {
						//user deleted his/her message, ignore
						continue;
					}

					//if someone sent a mention to the bot, give them a generic reply
					if (name != null) {
						String contentToLower = content.toLowerCase();
						if (contentToLower.contains(mentionToLower) || contentToLower.contains(compressedMentionToLower)) {
							ChatBuilder cb = new ChatBuilder();
							cb.reply(message).append("Type ").code(trigger + "help").append(" to see all my commands.");
							String reply = cb.toString();

							try {
								connection.sendMessage(room, reply);
							} catch (IOException e) {
								logger.log(Level.SEVERE, "Problem sending chat message.", e);
							}
						}
					}

					Matcher matcher = contentRegex.matcher(content);
					if (!matcher.find()) {
						//not a bot command, ignore
						continue;
					}

					logger.info("Responding to: [#" + message.getMessageId() + "] [" + message.getTimestamp() + "] " + message.getContent());

					List<String> replies = new ArrayList<>();
					String commandName = matcher.group(1);
					String text = matcher.group(3);
					boolean isAdmin = admins.contains(message.getUserId());
					message.setContent(text);

					try {
						for (Command command : getCommands(commandName)) {
							String reply = command.onMessage(message, isAdmin);
							if (reply != null) {
								replies.add(reply);
							}
						}
					} catch (ShutdownException e) {
						broadcast("Shutting down.  See you later.");
						return;
					}

					if (replies.isEmpty()) {
						replies.add(new ChatBuilder().reply(message).append("I don't know that command. o_O").toString());
					}

					try {
						for (String reply : replies) {
							connection.sendMessage(room, reply);
						}
					} catch (IOException e) {
						logger.log(Level.SEVERE, "Problem sending chat message.", e);
					}
				}

				ChatMessage latestMessage = messages.get(messages.size() - 1);
				prevMessageIds.put(room, latestMessage.getMessageId());
			}

			long elapsed = System.currentTimeMillis() - start;
			long sleep = heartbeat - elapsed;
			if (sleep > 0) {
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	/**
	 * Gets all commands that have a given name.
	 * @param name the command name
	 * @return the matching commands
	 */
	private List<Command> getCommands(String name) {
		List<Command> result = new ArrayList<>();
		for (Command command : commands) {
			if (command.name().equals(name)) {
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
		private String email, password, name, trigger = "=";
		private int heartbeat = 3000;
		private List<Integer> rooms = new ArrayList<>();
		private List<Integer> admins = new ArrayList<>();
		private List<Command> commands = new ArrayList<>();

		public Builder(String email, String password) {
			this.email = email;
			this.password = password;
		}

		public Builder connection(ChatConnection connection) {
			this.connection = connection;
			return this;
		}

		public Builder name(String name) {
			this.name = (name == null || name.isEmpty()) ? null : name;
			return this;
		}

		public Builder trigger(String trigger) {
			this.trigger = trigger;
			return this;
		}

		public Builder heartbeat(int heartbeat) {
			this.heartbeat = heartbeat;
			return this;
		}

		public Builder rooms(Integer... rooms) {
			this.rooms = Arrays.asList(rooms);
			return this;
		}

		public Builder admins(Integer... admins) {
			this.admins = Arrays.asList(admins);
			return this;
		}

		public Builder commands(Command... commands) {
			this.commands = Arrays.asList(commands);
			return this;
		}

		public Bot build() throws IOException {
			return new Bot(this);
		}
	}
}
