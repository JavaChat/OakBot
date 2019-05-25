package oakbot.bot;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import oakbot.chat.IChatClient;
import oakbot.chat.IRoom;
import oakbot.command.Command;
import oakbot.listener.Listener;

/**
 * Used to interact with the bot from inside of a {@link Command} or
 * {@link Listener}.
 * @author Michael Angstadt
 */
public class BotContext {
	private final boolean authorAdmin;
	private final String trigger;
	private final IChatClient connection;

	private final List<Integer> currentRooms, homeRooms;
	private final Integer maxRooms;

	/**
	 * @param authorAdmin true if the incoming message author is an admin, false
	 * if not
	 * @param trigger the bot's command trigger
	 * @param connection the connection to the chat system
	 * @param currentRooms the rooms the bot is currently in
	 * @param homeRooms the bot's home rooms
	 * @param maxRooms the maximum number of rooms the bot can be in at a time
	 * or null for no limit
	 */
	public BotContext(boolean authorAdmin, String trigger, IChatClient connection, List<Integer> currentRooms, List<Integer> homeRooms, Integer maxRooms) {
		this.authorAdmin = authorAdmin;
		this.trigger = trigger;
		this.connection = connection;
		this.currentRooms = Collections.unmodifiableList(currentRooms);
		this.homeRooms = Collections.unmodifiableList(homeRooms);
		this.maxRooms = maxRooms;
	}

	/**
	 * Gets the bot's command trigger.
	 * @return the command trigger
	 */
	public String getTrigger() {
		return trigger;
	}

	/**
	 * Is the user who posted the incoming message an admin?
	 * @return true if the user is an admin, false if not
	 */
	public boolean isAuthorAdmin() {
		return authorAdmin;
	}

	/**
	 * Gets the connection to a chat room.
	 * @param roomId the room ID
	 * @return the connection or null if the chat client is not connected to
	 * that room
	 */
	public IRoom getRoom(int roomId) {
		return connection.getRoom(roomId);
	}

	/**
	 * Gets the rooms the bot is currently in.
	 * @return the room IDs
	 */
	public List<Integer> getCurrentRooms() {
		return currentRooms;
	}

	/**
	 * Gets the bot's home rooms.
	 * @return the room IDs
	 */
	public List<Integer> getHomeRooms() {
		return homeRooms;
	}

	/**
	 * Gets the maximum number of rooms the bot can be in at once.
	 * @return the max rooms or null for no limit
	 */
	public Integer getMaxRooms() {
		return maxRooms;
	}

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
	 * @throws IOException if there's a network problem or a non-200 response
	 * was returned
	 */
	public String getOriginalMessageContent(long messageId) throws IOException {
		return connection.getOriginalMessageContent(messageId);
	}
}
