package oakbot.bot;

/**
 * Represents an action to perform in response to a chat message.
 * Implementations define specific actions like posting messages, joining rooms, etc.
 * @author Michael Angstadt
 */
public interface ChatAction {
	/**
	 * Executes this action.
	 * @param context the execution context containing bot and message information
	 * @return additional actions to be performed, or empty if none
	 */
	ChatActions execute(ActionContext context);
}
