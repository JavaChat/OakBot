package oakbot.bot;

/**
 * Represents an action to perform in response to a chat message.
 * Implementations define specific actions like posting messages, joining rooms, etc.
 * Actions are processed by Bot using conditional logic based on their type.
 * @author Michael Angstadt
 */
public interface ChatAction {
	// Marker interface - no methods required
	// Action processing is handled by Bot.processAction() using instanceof checks
}
