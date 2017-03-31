package oakbot.chat;

/**
 * Thrown when an attempt is made to join a non-existent chat room.
 * @author Michael Angstadt
 * @see ChatConnection#joinRoom(int)
 */
@SuppressWarnings("serial")
public class RoomNotFoundException extends RuntimeException {
	public RoomNotFoundException(int roomId) {
		super("Room " + roomId + " does not exist.");
	}
}
