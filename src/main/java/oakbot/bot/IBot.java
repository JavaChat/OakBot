package oakbot.bot;

import java.io.IOException;
import java.util.List;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.IRoom;
import com.github.mangstadt.sochat4j.RoomNotFoundException;
import com.github.mangstadt.sochat4j.RoomPermissionException;

/**
 * Represents a chat bot.
 * @author Michael Angstadt
 */
public interface IBot {
	/**
	 * Gets the command trigger.
	 * @return the trigger (e.g. "/")
	 */
	String getTrigger();

	/**
	 * Gets the user ID of the bot's account.
	 * @return the user ID
	 */
	Integer getUserId();

	/**
	 * Gets the username of the bot's account.
	 * @return the username
	 */
	String getUsername();

	/**
	 * Gets the users who have admin permissions over the bot.
	 * @return the user IDs of the admin users
	 */
	List<Integer> getAdminUsers();

	/**
	 * Of all the rooms the bot is connected to, this method returns the "home"
	 * rooms. Users cannot make the bot leave home rooms.
	 * @return the room IDs
	 */
	List<Integer> getHomeRooms();

	/**
	 * Of all the rooms the bot is connected to, this method returns the "quiet"
	 * rooms. Quiet rooms receive fewer posts (e.g. "fact of the day" or awkward
	 * silence posts are not posted to quiet rooms).
	 * @return the room IDs
	 */
	List<Integer> getQuietRooms();

	/**
	 * Gets the rooms that the bot is connected to.
	 * @return the room IDs
	 */
	List<Integer> getRooms();

	/**
	 * Gets the maximum number of rooms the bot can be in at once.
	 * @return the max rooms or null for no limit
	 */
	Integer getMaxRooms();

	/**
	 * Gets the network connection to a room.
	 * @param roomId the room ID
	 * @return the network connection or null if the bot is not connected to the
	 * room
	 */
	IRoom getRoom(int roomId);

	/**
	 * Joins a room.
	 * @param roomId the room ID
	 * @return the connection to the room
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if the bot can't post messages to the
	 * room (e.g. if it doesn't have permission to post or the room is frozen)
	 * @throws IOException if there's a problem connecting to the room
	 */
	void join(int roomId) throws IOException;

	/**
	 * Leaves a room.
	 * @param roomId the room ID
	 * @throws IOException if there's a problem leaving the room
	 */
	void leave(int roomId) throws IOException;

	/**
	 * Gets the latest messages from a room.
	 * @param roomId the room ID
	 * @param count the number of messages to retrieve
	 * @return the messages in chronological order
	 * @throws if there's a problem getting the messages
	 */
	List<ChatMessage> getLatestMessages(int roomId, int count) throws IOException;

	/**
	 * <p>
	 * Queries the chat service for the original, Markdown-encoded message that
	 * the user actually typed into the chat room (when messages are retrieved
	 * off the web socket, the messages returned as HTML).
	 * </p>
	 * <p>
	 * Note that this involves sending an HTTP GET request to the server.
	 * </p>
	 * <p>
	 * Note that this will give you EXACTLY what the user typed into the chat.
	 * For example, if they typed a single space character before their message,
	 * the space character will NOT appear the HTML-formatted message, but WILL
	 * appear in the string returned by this method.
	 * </p>
	 * @param messageId the message ID
	 * @return the plain text message
	 * @throws IOException if there's a problem getting the message
	 */
	String getOriginalMessageContent(long messageId) throws IOException;

	/**
	 * Uploads an image to Stack Overflow's imgur.com service.
	 * @param url the image URL
	 * @return the imgur.com URL
	 * @throws IOException if there's a problem uploading the image
	 */
	String uploadImage(String url) throws IOException;

	/**
	 * Posts a message to a room. If the bot has not joined the given room, then
	 * this method will not do anything.
	 * @param roomId the room ID
	 * @param message the message to post
	 * @throws IOException if there's a problem sending the message
	 */
	void sendMessage(int roomId, PostMessage message) throws IOException;

	/**
	 * Sends a message to all non-quiet chat rooms the bot is connected to.
	 * @param message the message to send
	 * @throws IOException if there's a problem sending the message
	 */
	void broadcastMessage(PostMessage message) throws IOException;
}
