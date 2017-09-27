package oakbot.chat;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import oakbot.chat.event.Event;

/**
 * Represents the connection to a room the user has joined. Use the
 * {@link IChatClient#joinRoom} method to properly create an instance of this
 * class.
 * @author Michael Angstadt
 */
public interface IRoom extends Closeable {
	/**
	 * Gets the ID of this room.
	 * @return the room ID
	 */
	int getRoomId();

	/**
	 * Gets this room's fkey. The fkey is a 32 character, hexadecimal sequence
	 * that is required to interact with a chat room. It changes at every login.
	 * @return the fkey
	 */
	String getFkey();

	/**
	 * Determines if the user can post messages to the room.
	 * @return true if the user can post messages, false if not
	 */
	boolean canPost();

	/**
	 * Gets the time that the room last received a "message posted" event (for
	 * debugging).
	 * @return the time
	 */
	Instant getTimeOfLastReceivedMessagePostedEvent();

	/**
	 * Adds a listener which receives all events.
	 * @param listener the listener
	 */
	void addEventListener(Consumer<Event> listener);

	/**
	 * Adds a listener which receives a specific type of event.
	 * @param clazz the event class
	 * @param listener the listener
	 */
	<T extends Event> void addEventListener(Class<T> clazz, Consumer<T> listener);

	/**
	 * Gets the most recent messages from the room.
	 * @param count the number of messages to retrieve
	 * @return the messages
	 * @throws IOException if there's a problem executing the request
	 */
	List<ChatMessage> getMessages(int count) throws IOException;

	/**
	 * Posts a message to the room. If the message exceeds the max message size,
	 * it will be truncated.
	 * @param message the message to post
	 * @return the ID of the new message
	 * @throws RoomPermissionException if the user doesn't have permission to
	 * post messages to the room
	 * @throws IOException if there's a problem executing the request
	 */
	long sendMessage(String message) throws RoomPermissionException, IOException;

	/**
	 * Posts a message to the room.
	 * @param message the message to post
	 * @param splitStrategy defines how the message should be split up if the
	 * message exceeds the chat connection's max message size
	 * @return the ID(s) of the new message(s). This list will contain multiple
	 * IDs if the message was split up into multiple messages.
	 * @throws RoomPermissionException if the user doesn't have permission to
	 * post messages to the room
	 * @throws IOException if there's a problem executing the request
	 */
	List<Long> sendMessage(String message, SplitStrategy splitStrategy) throws RoomPermissionException, IOException;

	/**
	 * <p>
	 * Edits a message.
	 * </p>
	 * <p>
	 * Note that you can only edit your own messages. Also, messages older than
	 * two minutes cannot be edited.
	 * </p>
	 * @param messageId the ID of the message to edit
	 * @param content the updated message content
	 * @throws IOException if there's a problem executing the request
	 */
	void editMessage(long messageId, String content) throws IOException;

	/**
	 * <p>
	 * Deletes a message.
	 * </p>
	 * <p>
	 * Note that you can only delete your own messages. Also, messages older
	 * than two minutes cannot be deleted.
	 * </p>
	 * @param messageId the ID of the message to delete
	 * @throws IOException if there's a problem executing the request
	 */
	void deleteMessage(long messageId) throws IOException;

	/**
	 * Gets information about room users, such as their reputation and username.
	 * @param userIds the user ID(s)
	 * @return the user information
	 * @throws IOException if there's a problem executing the request
	 */
	List<UserInfo> getUserInfo(List<Integer> userIds) throws IOException;

	/**
	 * Gets the users in a room that are "pingable". Pingable users receive
	 * notifications if they are mentioned. If a user is pingable, it does not
	 * mean they are currently in the room (although they could be).
	 * @return the pingable users
	 * @throws IOException if there's a problem executing the request
	 */
	List<PingableUser> getPingableUsers() throws IOException;

	/**
	 * Gets information about the room, such as its name and description.
	 * @return the room info
	 * @throws IOException if there's a problem executing the request
	 */
	RoomInfo getRoomInfo() throws IOException;

	/**
	 * Leaves the room and closes all of its network connections.
	 */
	void leave();

	/**
	 * This method <i>only</i> closes the room's network connections. To
	 * properly leave a room, call the {@link #leave} method instead.
	 */
	void close() throws IOException;
}
