package oakbot.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oakbot.chat.ChatConnection;
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
	private final ChatConnection connection;

	private boolean shutdown = false, shutdownMessageBroadcast;
	private String shutdownMessage;

	private final List<Integer> currentRooms, homeRooms;
	private final List<JoinRoomEvent> roomsToJoin = new ArrayList<>(0);
	private final List<Integer> roomsToLeave = new ArrayList<>(0);

	/**
	 * @param authorAdmin true if the incoming message author is an admin, false
	 * if not
	 * @param trigger the bot's command trigger
	 * @param connection the connection to the chat system
	 * @param currentRooms the rooms the bot is currently in
	 * @param homeRooms the bot's home rooms
	 */
	public BotContext(boolean authorAdmin, String trigger, ChatConnection connection, List<Integer> currentRooms, List<Integer> homeRooms) {
		this.authorAdmin = authorAdmin;
		this.trigger = trigger;
		this.connection = connection;
		this.currentRooms = Collections.unmodifiableList(currentRooms);
		this.homeRooms = Collections.unmodifiableList(homeRooms);
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
	 * Joins a room once all commands and listeners have had a chance to respond
	 * to the incoming message.
	 * @param event the join event
	 */
	public void joinRoom(JoinRoomEvent event) {
		roomsToJoin.add(event);
	}

	/**
	 * Gets the rooms the bot will join once all commands and listeners have had
	 * a chance to respond to the incoming message.
	 * @return the join events
	 */
	public List<JoinRoomEvent> getRoomsToJoin() {
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
	 * Gets the connection to the chat system.
	 * @return the chat connection
	 */
	public ChatConnection getConnection() {
		return connection;
	}

	/**
	 * Used to join a room.
	 * @author Michael Angstadt
	 */
	public static abstract class JoinRoomEvent {
		private final int roomId;

		/**
		 * @param roomId the ID of the room to join
		 */
		public JoinRoomEvent(int roomId) {
			this.roomId = roomId;
		}

		/**
		 * Gets the room to join.
		 * @return the room ID
		 */
		public int getRoomId() {
			return roomId;
		}

		/**
		 * Gest the message to send if the join was successful.
		 * @return the message or null not to send a message
		 */
		public abstract ChatResponse success();

		/**
		 * Gets the message to send if the room does not exist.
		 * @return the message or null not to send a message
		 */
		public abstract ChatResponse ifRoomDoesNotExist();

		/**
		 * Gets the message to send if the bot cannot post messages to the room.
		 * @return the message or null not to send a message
		 */
		public abstract ChatResponse ifBotDoesNotHavePermission();

		/**
		 * Gets the message to send if another error occurs.
		 * @param thrown the thrown exception
		 * @return the message or null not to send a message
		 */
		public abstract ChatResponse ifOther(IOException thrown);
	}
}
