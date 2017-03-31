package oakbot.chat;

/**
 * Handles chat messages.
 * @author Michael Angstadt
 */
public interface ChatMessageHandler {
	/**
	 * Handles a chat message.
	 * @param message the chat message
	 */
	void handle(ChatMessage message);
}
