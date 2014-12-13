package oakbot.bot;

import oakbot.chat.ChatMessage;

/**
 * A chat bot command
 * @author Michael Angstadt
 */
public interface Command {
	/**
	 * Gets the command's name. This is what is used to invoke the command.
	 * @return the name
	 */
	String name();

	/**
	 * Gets the command's description. This should be a short, one sentence
	 * description.
	 * @return the description
	 */
	String description();

	/**
	 * Gets the command's help text. This is shown when this command is queried
	 * with the "help" command (e.g. "help mycommand")
	 * @return the help text
	 */
	String helpText();

	/**
	 * Called when a user invokes this command.
	 * @param message the message that invoked the command
	 * @param isAdmin true if the message sender is an admin, false if not
	 * @return the response
	 */
	ChatResponse onMessage(ChatMessage message, boolean isAdmin);
}