package oakbot.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	private boolean shutdown = false, shutdownMessageBroadcast;
	private String shutdownMessage;

	private final List<Integer> currentRooms, homeRooms;
	private final Map<Integer, JoinRoomCallback> roomsToJoin = new LinkedHashMap<>(0);
	private final List<Integer> roomsToLeave = new ArrayList<>(0);
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
	 * Joins a room once all commands and listeners have had a chance to respond
	 * to the incoming message.
	 * @param roomId the room to join
	 * @param callback what to do if the join operation was successful or a
	 * failure
	 */
	public void joinRoom(int roomId, JoinRoomCallback callback) {
		roomsToJoin.put(roomId, callback);
	}

	/**
	 * Gets the rooms the bot will join once all commands and listeners have had
	 * a chance to respond to the incoming message.
	 * @return the join events
	 */
	public Map<Integer, JoinRoomCallback> getRoomsToJoin() {
		return roomsToJoin;
	}

	/**
	 * Leaves a room once all commands and listeners have had a chance to
	 * respond to the incoming message.
	 * @param roomId the room ID
	 */
	public void leaveRoom(int roomId) {
		roomsToLeave.add(roomId);
	}

	/**
	 * Gets the rooms the bot will leave once all commands and listeners have
	 * had a chance to respond to the incoming message.
	 * @return the room IDs
	 */
	public List<Integer> getRoomsToLeave() {
		return roomsToLeave;
	}

	/**
	 * Shutdown the bot once all commands and listeners have had a chance to
	 * respond to the incoming message.
	 * @param message the message to send before shutting down or null not to
	 * send a message
	 * @param broadcast true to broadcast the message to all chat rooms, false
	 * to only send the message to the room that the shutdown command came from
	 */
	public void shutdownBot(String message, boolean broadcast) {
		shutdown = true;
		shutdownMessage = message;
		shutdownMessageBroadcast = broadcast;
	}

	/**
	 * Gets whether the bot was told to shutdown.
	 * @return true to shutdown the bot, false not to
	 */
	public boolean isShutdown() {
		return shutdown;
	}

	/**
	 * Gets the bot's shutdown message.
	 * @return the shutdown message or null not to send a shutdown message
	 */
	public String getShutdownMessage() {
		return shutdownMessage;
	}

	/**
	 * Gets whether to broadcast the bot's shutdown message.
	 * @return true to broadcast, false not to
	 */
	public boolean isShutdownMessageBroadcast() {
		return shutdownMessageBroadcast;
	}

	/**
	 * Used to join a room.
	 * @author Michael Angstadt
	 */
	public interface JoinRoomCallback {
		/**
		 * Gets the message to send if the join was successful.
		 * @return the message or null not to send a message
		 */
		ChatResponse success();

		/**
		 * Gets the message to send if the room does not exist.
		 * @return the message or null not to send a message
		 */
		ChatResponse ifRoomDoesNotExist();

		/**
		 * Gets the message to send if the bot cannot post messages to the room.
		 * @return the message or null not to send a message
		 */
		ChatResponse ifBotDoesNotHavePermission();

		/**
		 * Gets the message to send if another error occurs.
		 * @param thrown the thrown exception
		 * @return the message or null not to send a message
		 */
		ChatResponse ifOther(IOException thrown);
	}
}
