package oakbot.chat;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * A connection to a Stack Exchange chat site.
 * @author Michael Angstadt
 * @see <a href="https://chat.stackoverflow.com">chat.stackoverflow.com</a>
 * @see <a href=
 * "https://github.com/Zirak/SO-ChatBot/blob/master/source/adapter.js">Good
 * explanation of how SO Chat works</a>
 */
public interface IChatClient extends Closeable {
	/**
	 * Logs into the chat system. This should be called before any other method.
	 * @param email the login email
	 * @param password the login password
	 * @throws InvalidCredentialsException if the login credentials are bad
	 * @throws IOException if there's a network problem
	 */
	void login(String email, String password) throws InvalidCredentialsException, IOException;

	/**
	 * Joins a chat room. If the client is already connected to the room, the
	 * existing {@link IRoom} instance is returned.
	 * @param roomId the room ID
	 * @return the connection to the chat room
	 * @throws IllegalStateException if the user hasn't yet logged in
	 * successfully (the {@link #login} method must be called first)
	 * @throws RoomNotFoundException if the room doesn't exist or the user does
	 * not have permission to view the room
	 * @throws IOException if there's a problem connecting to the room
	 */
	IRoom joinRoom(int roomId) throws RoomNotFoundException, IOException;

	/**
	 * Gets all of the rooms the chat client is currently connected to.
	 * @return the rooms
	 */
	List<? extends IRoom> getRooms();

	/**
	 * Gets a room that the chat client is currently connected to
	 * @param roomId the room ID
	 * @return the room or null if the chat client is not connected to that room
	 */
	IRoom getRoom(int roomId);

	/**
	 * Determines if the chat client is currently connected to a room.
	 * @param roomId the room ID
	 * @return true if the chat client is connected to the room, false if not
	 */
	boolean isInRoom(int roomId);

	/**
	 * <p>
	 * Queries the chat service for the original, Markdown-encoded message that
	 * the user actually typed into the chat room (when messages are retrieved
	 * off the web socket, the messages returned as HTML).
	 * </p>
	 * <p>
	 * Note that this will give you EXACTLY what the user typed into the chat.
	 * For example, if they prefixed their message with a single space
	 * character, the space character will NOT appear the HTML-formatted
	 * message, but WILL appear in the string returned by this method.
	 * </p>
	 * @param messageId the message ID
	 * @return the plain text message
	 * @throws IOException if there's a network problem or a non-200 response
	 * was returned
	 */
	String getOriginalMessageContent(long messageId) throws IOException;

	/**
	 * Gets the site that this chat client is connected to.
	 * @return the site
	 */
	Site getSite();
}
