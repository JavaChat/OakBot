package oakbot.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.chat.ChatConnection;
import oakbot.chat.ChatMessage;
import oakbot.command.Command;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;

import com.google.common.collect.ImmutableList;

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
	private final List<Listener> listeners;
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
		commands = builder.commands.build();
		listeners = builder.listeners.build();
	}

	/**
	 * Starts the chat bot. This call is blocking.
	 * @throws IllegalArgumentException if the login credentials are bad
	 * @throws IOException if there's an I/O problem
	 */
	public void connect() throws IllegalArgumentException, IOException {
		//login
		connection.login(email, password);

		//post a message to each room
		for (Integer room : rooms) {
			connection.getNewMessages(room); //prime the latest message timestamp
			connection.sendMessage(room, "OakBot Online.");
		}

		//listen for and reply to messages
		Pattern commandRegex = Pattern.compile("^" + Pattern.quote(trigger) + "\\s*(.*?)(\\s+(.*)|$)");
		while (true) {
			long start = System.currentTimeMillis();

			for (Integer room : rooms) {
				logger.fine("Pinging room " + room);

				//get new messages since last ping
				List<ChatMessage> newMessages = connection.getNewMessages(room);
				logger.fine(newMessages.size() + " new messages found.");
				if (newMessages.isEmpty()) {
					//no new messages
					continue;
				}

				for (ChatMessage message : newMessages) {
					String content = message.getContent();
					if (content == null) {
						//user deleted his/her message, ignore
						continue;
					}

					List<ChatResponse> replies = new ArrayList<>();
					boolean isAdmin = admins.contains(message.getUserId());

					//handle listeners
					for (Listener listener : listeners) {
						ChatResponse reply;
						try {
							reply = listener.onMessage(message, isAdmin);
						} catch (ShutdownException e) {
							broadcast("Shutting down.  See you later.");
							connection.flush();
							return;
						}

						if (reply != null) {
							logger.info("Responding to: [#" + message.getMessageId() + "] [" + message.getTimestamp() + "] " + message.getContent());
							replies.add(reply);
						}
					}

					//handle commands
					Matcher matcher = commandRegex.matcher(content);
					if (matcher.find()) {
						logger.info("Responding to: [#" + message.getMessageId() + "] [" + message.getTimestamp() + "] " + message.getContent());

						String commandName = matcher.group(1);
						String text = matcher.group(3);
						if (text == null) {
							text = "";
						}
						message.setContent(text);

						List<Command> commands = getCommands(commandName);
						if (commands.isEmpty()) {
							//@formatter:off
							replies.add(new ChatResponse(new ChatBuilder()
								.reply(message)
								.append("I don't know that command. o_O  Type ")
								.code(trigger + "help")
								.append(" to see my commands.")
							));
							//@formatter:on
						} else {
							for (Command command : commands) {
								ChatResponse reply;
								try {
									reply = command.onMessage(message, isAdmin);
								} catch (ShutdownException e) {
									broadcast("Shutting down.  See you later.");
									connection.flush();
									return;
								}

								if (reply != null) {
									replies.add(reply);
								}
							}
						}
					}

					try {
						for (ChatResponse reply : replies) {
							connection.sendMessage(room, reply.getMessage(), reply.getSplitStrategy());
						}
					} catch (IOException e) {
						logger.log(Level.SEVERE, "Problem sending chat message.", e);
					}
				}

				ChatMessage latestMessage = newMessages.get(newMessages.size() - 1);
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
		private ImmutableList.Builder<Command> commands = ImmutableList.builder();
		private ImmutableList.Builder<Listener> listeners = ImmutableList.builder();

		public Builder login(String email, String password) {
			this.email = email;
			this.password = password;
			return this;
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
			return rooms(Arrays.asList(rooms));
		}

		public Builder rooms(Collection<Integer> rooms) {
			this.rooms.addAll(rooms);
			return this;
		}

		public Builder admins(Integer... admins) {
			return admins(Arrays.asList(admins));
		}

		public Builder admins(Collection<Integer> admins) {
			this.admins.addAll(admins);
			return this;
		}

		public Builder commands(Command... commands) {
			return commands(Arrays.asList(commands));
		}

		public Builder commands(Collection<Command> commands) {
			this.commands.addAll(commands);
			return this;
		}

		public Builder listeners(Listener... listeners) {
			return listeners(Arrays.asList(listeners));
		}

		public Builder listeners(Collection<Listener> listeners) {
			this.listeners.addAll(listeners);
			return this;
		}

		public Bot build() throws IOException {
			if (connection == null) {
				throw new IllegalArgumentException("No ChatConnection given.");
			}
			return new Bot(this);
		}
	}
}
