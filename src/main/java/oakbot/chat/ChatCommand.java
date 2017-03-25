package oakbot.chat;

/**
 * Represents a chat message that is in the form of a bot command.
 * @author Michael Angstadt
 */
public class ChatCommand {
	private final ChatMessage message;
	private final String commandName;
	private final String content;

	/**
	 * @param message the original chat message
	 * @param commandName the name of the command
	 * @param content the rest of the text that came after the command name
	 * (must not be null)
	 */
	public ChatCommand(ChatMessage message, String commandName, String content) {
		this.message = message;
		this.commandName = commandName;
		this.content = content;
	}

	/**
	 * Gets the original chat message.
	 * @return the chat message
	 */
	public ChatMessage getMessage() {
		return message;
	}

	/**
	 * <p>
	 * Gets the command name.
	 * </p>
	 * <p>
	 * For example, given the chat message "/define java", this method would
	 * return "define".
	 * <p>
	 * @return the command name
	 */
	public String getCommandName() {
		return commandName;
	}

	/**
	 * <p>
	 * Gets the rest of the text that came after the command name.
	 * </p>
	 * <p>
	 * For example, given the chat message "/define java", this method would
	 * return "java". All whitespace after the command name are excluded from
	 * this method's return value.
	 * <p>
	 * @return the text or empty string if there is no text after the command
	 */
	public String getContent() {
		return content;
	}
}
