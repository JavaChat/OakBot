package oakbot.chat;

/**
 * Handles chat messages.
 * @author Michael Angstadt
 */
public interface ChatMessageHandler {
	/**
	 * Called when a new chat message is posted.
	 * @param message the chat message
	 */
	void onMessage(ChatMessage message);

	/**
	 * Called when a chat message is edited.
	 * @param message the chat message
	 */
	void onMessageEdited(ChatMessage message);
}
