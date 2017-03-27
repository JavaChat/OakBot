package oakbot.util;

import oakbot.bot.ChatCommand;
import oakbot.chat.ChatMessage;

/**
 * Builds mock {@link ChatCommand} objects for unit testing.
 * @author Michael Angstadt
 */
public class ChatCommandBuilder {
	private final String commandName;

	/**
	 * @param commandName the command name (e.g. "define")
	 */
	public ChatCommandBuilder(String commandName) {
		this.commandName = commandName;
	}

	/**
	 * Constructs a mock {@link ChatCommand} object.
	 * @param messageId the message ID
	 * @param content the command content
	 * @return the chat command
	 */
	public ChatCommand build(int messageId, String content) {
		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.messageId(messageId)
			.content("/" + commandName + " " + content)
		.build();
		//@formatter:on

		return new ChatCommand(message, commandName, content);
	}
}
