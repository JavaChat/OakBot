package oakbot.chat;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.List;

/**
 * Represents a connection to a chat system.
 * @author Michael Angstadt
 */
public interface ChatConnection extends Closeable, Flushable {
	/**
	 * Logs into the chat system. This should be called before any other method.
	 * @param email the login email
	 * @param password the login password
	 * @throws InvalidCredentialsException if the login credentials are bad
	 * @throws IOException if there's a network problem
	 */
	void login(String email, String password) throws InvalidCredentialsException, IOException;

	/**
	 * Joins a chat room. A room should be joined before it is interacted with.
	 * A room should be joined only once.
	 * @param roomId the room ID
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if messages cannot be posted to this room
	 * @throws IOException if there's a problem connecting to the room
	 */
	void joinRoom(int roomId) throws RoomNotFoundException, RoomPermissionException, IOException;

	/**
	 * Leaves a chat room. This method does nothing if the room was never
	 * joined.
	 * @param roomId the room ID
	 * @throws IOException if there's network problem
	 */
	void leaveRoom(int roomId) throws IOException;

	/**
	 * Posts a message to a chat room. If the message exceeds the max message
	 * size, it will be truncated.
	 * @param roomId the ID of the chat room
	 * @param message the message to post
	 * @throws IOException if there's network problem
	 */
	void sendMessage(int roomId, String message) throws IOException;

	/**
	 * Posts a message to a chat room.
	 * @param roomId the ID of the chat room
	 * @param message the message to post
	 * @param splitStragey defines how the message should be split up if the
	 * chat connect has a max message size
	 * @throws IOException if there's network problem
	 */
	void sendMessage(int roomId, String message, SplitStrategy splitStragey) throws IOException;

	/**
	 * Respond to new messages as they become available. This method blocks
	 * until the chat connection is closed.
	 * @param handler handles the messages
	 */
	void listen(ChatMessageHandler handler) throws IOException;

	/**
	 * Handles a {@link ChatMessage}.
	 * @author Michael Angstadt
	 */

	/**
	 * Gets the most recent messages from a chat room.
	 * @param roomId the chat room ID
	 * @param count the number of messages to retrieve
	 * @return the messages
	 * @throws IOException if there's network problem
	 */
	List<ChatMessage> getMessages(int roomId, int count) throws IOException;
}
