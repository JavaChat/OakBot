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
	 * Joins a room. A room should be joined before it is interacted with.
	 * @param roomId the room ID
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if messages cannot be posted to this room
	 * @throws IOException if there's a network problem
	 */
	void joinRoom(int roomId) throws RoomNotFoundException, RoomPermissionException, IOException;

	/**
	 * Leaves a room. This method does nothing if the room was never joined.
	 * @param roomId the room ID
	 * @throws IOException if there's a network problem
	 */
	void leaveRoom(int roomId) throws IOException;

	/**
	 * Posts a message. If the message exceeds the max message size, it will be
	 * truncated.
	 * @param roomId the room ID
	 * @param message the message to post
	 * @return the ID of the new message
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if the message can't be posted to the
	 * room because it doesn't exist or the bot doesn't have permission
	 * @throws IOException if there's a network problem
	 */
	long sendMessage(int roomId, String message) throws RoomNotFoundException, RoomPermissionException, IOException;

	/**
	 * Posts a message.
	 * @param roomId the room ID
	 * @param message the message to post
	 * @param splitStrategy defines how the message should be split up if the
	 * message exceeds the chat connection's max message size
	 * @return the ID(s) of the new message(s). This list will contain multiple
	 * IDs if the message was split up into multiple messages.
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if the message can't be posted to the
	 * room because it doesn't exist or the bot doesn't have permission
	 * @throws IOException if there's a network problem
	 */
	List<Long> sendMessage(int roomId, String message, SplitStrategy splitStrategy) throws RoomNotFoundException, RoomPermissionException, IOException;

	/**
	 * Deletes a message. You can only delete your own messages. Messages older
	 * than two minutes cannot be deleted.
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
	 * Edits an existing message. You can only edit your own messages. Messages
	 * older than two minutes cannot be edited.
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
	 * Listens for new messages that are posted to rooms that were joined with
	 * the {@link joinRoom} method. This method blocks until the chat connection
	 * is closed.
	 * @param handler handles the messages
	 */
	void listen(ChatMessageHandler handler);

	/**
	 * Gets the most recent messages from a room.
	 * @param roomId the room ID
	 * @param count the number of messages to retrieve
	 * @return the messages
	 * @throws IOException if there's a network problem
	 */
	List<ChatMessage> getMessages(int roomId, int count) throws IOException;

	/**
	 * Gets information about room users, such as their reputation and username.
	 * @param roomId the ID of the room the user(s) are in (it is not necessary
	 * to join this room before calling this method)
	 * @param userIds the user ID(s)
	 * @return the user information
	 * @throws IOException if there's a network problem
	 */
	List<UserInfo> getUserInfo(int roomId, List<Integer> userIds) throws IOException;

	/**
	 * Gets the users in a room that are "pingable". Pingable users receive
	 * notifications if they are mentioned. If a user is pingable, it does not
	 * necessarily mean they are currently in the room, although they could be.
	 * @param roomId the room ID (it is not necessary to join this room before
	 * calling this method)
	 * @return the pingable users
	 * @throws IOException if there's a network problem
	 */
	List<PingableUser> getPingableUsers(int roomId) throws IOException;

	/**
	 * Gets information about a room, such as its name and description.
	 * @param roomId the room ID
	 * @return the room info or null if the room doesn't exist
	 * @throws IOException if there's a network problem
	 */
	RoomInfo getRoomInfo(int roomId) throws IOException;
}
