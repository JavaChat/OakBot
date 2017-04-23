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
	 * @throws IOException if there's a network problem
	 */
	void joinRoom(int roomId) throws RoomNotFoundException, RoomPermissionException, IOException;

	/**
	 * Leaves a chat room. This method does nothing if the room was never
	 * joined.
	 * @param roomId the room ID
	 * @throws IOException if there's a network problem
	 */
	void leaveRoom(int roomId) throws IOException;

	/**
	 * Posts a message to a chat room. If the message exceeds the max message
	 * size, it will be truncated.
	 * @param roomId the ID of the chat room
	 * @param message the message to post
	 * @return the ID of the new message
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if the message can't be posted to the
	 * room because it doesn't exist or the bot doesn't have permission
	 * @throws IOException if there's a network problem
	 */
	long sendMessage(int roomId, String message) throws RoomNotFoundException, RoomPermissionException, IOException;

	/**
	 * Posts a message to a chat room.
	 * @param roomId the ID of the chat room
	 * @param message the message to post
	 * @param splitStrategy defines how the message should be split up if the
	 * message exceeds the chat connection's a max message size
	 * @return the ID(s) of the new message(s). This list will contain multiple
	 * IDs if the message was split up into multiple messages
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if the message can't be posted to the
	 * room because it doesn't exist or the bot doesn't have permission
	 * @throws IOException if there's a network problem
	 */
	List<Long> sendMessage(int roomId, String message, SplitStrategy splitStrategy) throws RoomNotFoundException, RoomPermissionException, IOException;

	/**
	 * Deletes a message.
	 * @param roomId the ID of the room that the message was posted to
	 * @param messageId the ID of the message to delete
	 * @return true if it was successfully deleted, false if not
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if the room because it doesn't exist or
	 * the bot doesn't have permission to post in that room anymore
	 * @throws IOException if there's a network problem
	 */
	boolean deleteMessage(int roomId, long messageId) throws RoomNotFoundException, RoomPermissionException, IOException;

	/**
	 * Edits a message that was already posted
	 * @param roomId the ID of the room that the message was posted to
	 * @param messageId the ID of the message to edit
	 * @param updatedMessage the updated message
	 * @return boolean if the edit was successful, false if not
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if the room because it doesn't exist or
	 * the bot doesn't have permission to post in that room anymore
	 * @throws IOException if there's a network problem
	 */
	boolean editMessage(int roomId, long messageId, String updatedMessage) throws RoomNotFoundException, RoomPermissionException, IOException;

	/**
	 * Respond to new messages as they become available. This method blocks
	 * until the chat connection is closed. Use the {@link #joinRoom(int)}
	 * method to listen for messages in a room.
	 * @param handler handles the messages
	 */
	void listen(ChatMessageHandler handler);

	/**
	 * Gets the most recent messages from a chat room.
	 * @param roomId the chat room ID
	 * @param count the number of messages to retrieve
	 * @return the messages
	 * @throws IOException if there's a network problem
	 */
	List<ChatMessage> getMessages(int roomId, int count) throws IOException;

	/**
	 * Gets additional information about a chat user.
	 * @param userId the user ID
	 * @param roomId the ID of the room the user is in
	 * @return the user information or null if the user can't be found
	 * @throws IOException if there's a network problem
	 */
	UserInfo getUserInfo(int userId, int roomId) throws IOException;
}
