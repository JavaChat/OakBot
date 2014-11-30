package oakbot.bot;

import static oakbot.util.ChatUtils.reply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.chat.ChatMessage;
import oakbot.chat.SOChat;
import oakbot.util.ChatBuilder;

/**
 * A Stackoverflow chat bot.
 * @author Michael Angstadt
 */
public class Bot {
	private static final Logger logger = Logger.getLogger(Bot.class.getName());

	private final String email, password, name, trigger;
	private final SOChat chat;
	private final int heartbeat;
	private final List<Integer> rooms, admins;
	private final List<Command> commands;
	private final Map<Integer, Long> prevMessageIds = new HashMap<>();

	private Bot(Builder builder) {
		chat = new SOChat();
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

		//listen for and reply to messages
		Pattern contentRegex = Pattern.compile("^" + Pattern.quote(trigger) + "\\s*(.*?)(\\s+(.*)|$)");
		while (true) {
			long start = System.currentTimeMillis();

			for (Integer room : rooms) {
				logger.fine("Pinging room " + room);

				//get new messages since last ping
				List<ChatMessage> messages = getNewMessages(room);
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

					if (name != null) {
						String contentToLower = content.toLowerCase();
						if (contentToLower.startsWith(mentionToLower) || contentToLower.startsWith(compressedMentionToLower)) {
							ChatBuilder cb = new ChatBuilder();
							cb.reply(message).append("Type ").code(trigger + "help").append(" to see all my commands.");
							String reply = cb.toString();

							try {
								chat.postMessage(room, reply);
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
						replies.add(reply(message, "I don't know that command. o_O"));
					}

					try {
						for (String reply : replies) {
							chat.postMessage(room, reply);
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

	private List<ChatMessage> getNewMessages(int room) throws IOException {
		long prevMessageId = prevMessageIds.get(room);

		//keep retrieving more and more messages until we got all of the ones that came in since we last pinged
		List<ChatMessage> messages;
		for (int count = 5; true; count += 5) {
			messages = chat.getMessages(room, count);
			if (messages.isEmpty() || messages.get(0).getMessageId() <= prevMessageId) {
				break;
			}
		}

		//only return the new messages
		int pos = -1;
		for (int i = 0; i < messages.size(); i++) {
			ChatMessage message = messages.get(i);
			if (message.getMessageId() > prevMessageId) {
				pos = i;
				break;
			}
		}

		return (pos < 0) ? Collections.emptyList() : messages.subList(pos, messages.size());
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
	 * Builds {@link Bot} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private String email, password, name, trigger = "=";
		private int heartbeat = 3000;
		private List<Integer> rooms = new ArrayList<>();
		private List<Integer> admins = new ArrayList<>();
		private List<Command> commands = new ArrayList<>();

		public Builder(String email, String password) {
			this.email = email;
			this.password = password;
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
