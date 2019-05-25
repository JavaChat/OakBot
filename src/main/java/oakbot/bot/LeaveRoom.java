package oakbot.bot;

/**
 * Instructs the bot to leave a room.
 * @author Michael Angstadt
 */
public class LeaveRoom implements ChatAction {
	private int roomId;

	/**
	 * @param roomId the ID of the room to leave
	 */
	public LeaveRoom(int roomId) {
		this.roomId = roomId;
	}

	/**
	 * Gets the ID of the room to leave.
	 * @return the room ID
	 */
	public int roomId() {
		return roomId;
	}

	/**
	 * Sets the ID of the room to leave.
	 * @param roomId the room ID
	 * @return this
	 */
	public LeaveRoom roomId(int roomId) {
		this.roomId = roomId;
		return this;
	}
}
