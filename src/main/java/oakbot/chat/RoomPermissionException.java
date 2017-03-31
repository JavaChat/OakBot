package oakbot.chat;

/**
 * Thrown when an attempt is made to join a room that does not permit the bot to
 * post messages to it. This can either be because the room is inactive or
 * because the bot doesn't have permission to post.
 * @author Michael Angstadt
 * @see ChatConnection#joinRoom(int)
 */
@SuppressWarnings("serial")
public class RoomPermissionException extends RuntimeException {
	public RoomPermissionException(int roomId) {
		super("Cannot post messages to room " + roomId + ". It's either inactive or protected.");
	}
}
