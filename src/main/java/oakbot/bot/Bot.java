package oakbot.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.chat.ChatMessage;
import oakbot.chat.SOChat;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * A Stackoverflow chat bot.
 * @author Michael Angstadt
 */
public class Bot {
	private static final Logger logger = Logger.getLogger(Bot.class.getName());

	private final String email, password, trigger, about;
	private final SOChat chat;
	private final int heartbeat;
	private final List<Integer> rooms, admins;
	private final List<Command> commands;
	private final Map<Integer, Long> prevMessageIds = new HashMap<>();

	private Bot(Builder builder) {
		chat = new SOChat();
		email = builder.email;
		password = builder.password;
		trigger = builder.trigger;
		about = builder.about;
		heartbeat = builder.heartbeat;
		rooms = builder.rooms;
		admins = builder.admins;
		commands = builder.commands;
	}

	/**
	 * Starts the chat bot.  This call is blocking.
	 * @throws IOException if there's an I/O problem
	 */
	public void connect() throws IOException {
		//login
		chat.login(email, password);

		//get the IDs of the latest messages
		for (Integer room : rooms) {
			List<ChatMessage> messages = chat.getMessages(room, 1);

			long prevId;
			if (messages.isEmpty()) {
				prevId = 0;
			} else {
				ChatMessage last = messages.get(messages.size() - 1);
				prevId = last.getMessageId();
			}
			prevMessageIds.put(room, prevId);

			chat.postMessage(room, "OakBot Online.");
		}

		//listen for, and reply to, messages
		Pattern contentRegex = Pattern.compile("^" + Pattern.quote(trigger) + "\\s*(.*?)(\\s+(.*)|$)");
		while (true) {
			long start = System.currentTimeMillis();

			for (Integer room : rooms) {
				logger.fine("Pinging room " + room);
				long prevMessageId = prevMessageIds.get(room);
				List<ChatMessage> messages = chat.getMessages(room, 5); //TODO keep adding 5 until we reach an old message to ensure that we respond to all messages
				for (ChatMessage message : messages) {
					if (message.getMessageId() <= prevMessageId) {
						//already handled, ignore
						continue;
					}

					String content = message.getContent();
					Matcher matcher = contentRegex.matcher(content);
					if (!matcher.find()) {
						//not a bot command, ignore
						prevMessageId = message.getMessageId();
						prevMessageIds.put(room, prevMessageId);
						continue;
					}

					logger.fine("Responding to: [#" + message.getMessageId() + "] [" + message.getTimestamp() + "] " + message.getContent());

					List<String> replies = new ArrayList<>();
					String commandName = matcher.group(1);
					String text = matcher.group(3);
					switch (commandName) {
					case "help":
						replies.addAll(handleHelp(text, message));
						break;
					case "about":
						replies.add(about);
						break;
					case "shutdown":
						if (admins.contains(message.getUserId())) {
							broadcast("Shutting down.  See you later.");
							return;
						} else {
							replies.add(reply(message, "Only admins can shut me down."));
						}
						break;
					default:
						message.setContent(text);
						for (Command command : getCommands(commandName)) {
							String reply = command.onMessage(message);
							if (reply != null) {
								replies.add(reply);
							}
						}
						if (replies.isEmpty()) {
							replies.add(reply(message, "I don't know that command. o_O"));
						}
						break;
					}

					for (String reply : replies) {
						chat.postMessage(room, reply);
					}

					prevMessageId = message.getMessageId();
					prevMessageIds.put(room, prevMessageId);
				}
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
			chat.postMessage(room, message);
		}
	}

	/**
	 * Builds a "reply" message.
	 * @param replyTo the message to reply to
	 * @param text the reply
	 * @return the message text
	 */
	private static String reply(ChatMessage replyTo, String text) {
		return ":" + replyTo.getMessageId() + " " + text;
	}

	/**
	 * Builds a response to the "help" command
	 * @param commandText the text that comes after the command
	 * @param message the source message
	 * @return the reply
	 */
	private List<String> handleHelp(String commandText, ChatMessage message) {
		if (commandText != null) {
			List<String> replies = new ArrayList<>();
			for (Command command : getCommands(commandText)) {
				replies.add(reply(message, "`" + command.name() + ":` " + command.helpText()));
			}
			if (replies.isEmpty()) {
				replies.add(reply(message, "No command exists with that name."));
			}
			return replies;
		}

		//TODO split help message up into multiple messages if necessary
		//build each line of the reply
		Multimap<String, String> lines = TreeMultimap.create();
		lines.put("about", "Displays a short description of this bot.");
		lines.put("help", "Displays this help message.");
		lines.put("shutdown", "Terminates the bot (admins only).");
		for (Command command : commands) {
			lines.put(command.name(), command.description());
		}

		//get the length of the longest command name
		int longestName = 0;
		for (String key : lines.keySet()) {
			int length = key.length();
			if (length > longestName) {
				longestName = length;
			}
		}

		//build message
		StringBuilder sb = new StringBuilder();
		sb.append("    OakBot Command List\n");
		for (Map.Entry<String, String> entry : lines.entries()) {
			String name = entry.getKey();
			String description = entry.getValue();

			sb.append("    ").append(name);
			for (int i = name.length(); i < longestName + 2; i++) {
				sb.append(' ');
			}
			sb.append(description).append("\n");
		}

		return Arrays.asList(sb.toString());
	}

	/**
	 * Builds {@link Bot} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private String email, password, trigger = "=", about = "Chat Bot.";
		private int heartbeat = 3000;
		private List<Integer> rooms = new ArrayList<>();
		private List<Integer> admins = new ArrayList<>();
		private List<Command> commands = new ArrayList<>();

		public Builder(String email, String password) {
			this.email = email;
			this.password = password;
		}

		public Builder trigger(String trigger) {
			this.trigger = trigger;
			return this;
		}

		public Builder about(String about) {
			this.about = about;
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
