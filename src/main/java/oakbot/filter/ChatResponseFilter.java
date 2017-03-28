package oakbot.filter;

import java.util.HashSet;
import java.util.Set;

/**
 * Modifies the content of a chat message before it is sent.
 * @author Michael Angstadt
 */
public abstract class ChatResponseFilter {
	/**
	 * Is the filter enabled in all rooms?
	 */
	protected boolean globallyEnabled = false;

	/**
	 * The rooms the filter is enabled in.
	 */
	protected Set<Integer> enabledRooms = new HashSet<>();

	/**
	 * Determines if the filter is enabled. It only processes messages if it is
	 * enabled.
	 * @param roomId the roomID
	 * @return true if enabled, false if not
	 */
	public boolean isEnabled(int roomId) {
		return globallyEnabled ? true : enabledRooms.contains(roomId);
	}

	/**
	 * Enables or disables the filter in all rooms.
	 * @param enabled true to enable, false to disable
	 */
	public void setGloballyEnabled(boolean enabled) {
		globallyEnabled = enabled;
	}

	/**
	 * Enables or disables the filter in a specific room.
	 * @param roomId the room ID
	 * @param enabled true to enable, false to disable
	 */
	public void setEnabled(int roomId, boolean enabled) {
		if (enabled) {
			enabledRooms.add(roomId);
		} else {
			enabledRooms.remove(roomId);
		}
	}

	/**
	 * Performs the filter operation. This method should not be responsible for
	 * checking if the filter is enabled or not.
	 * @param message the message to filter
	 * @return the filtered message
	 */
	public abstract String filter(String message);
}
