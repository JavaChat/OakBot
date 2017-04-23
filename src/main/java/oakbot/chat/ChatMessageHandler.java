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

	/**
	 * Called when there's an error retrieving messages from a room. This could
	 * happen if the bot is kicked from a room, if a room is suddenly frozen, or
	 * if there's a network error.
	 * @param roomId the room ID
	 * @param thrown the thrown exception
	 */
	void onError(int roomId, Exception thrown);
}
