package oakbot.bot;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Instructs the bot to join a room.
 * @author Michael Angstadt
 */
public class JoinRoom implements ChatAction {
	private int roomId;
	private Supplier<ChatActions> onSuccess, ifRoomDoesNotExist, ifLackingPermissionToPost;
	private Function<Exception, ChatActions> onError;

	/**
	 * @param roomId the ID of the room to join
	 */
	public JoinRoom(int roomId) {
		this.roomId = roomId;
		onSuccess = ifRoomDoesNotExist = ifLackingPermissionToPost = () -> ChatActions.doNothing();
		onError = (e) -> ChatActions.doNothing();
	}

	/**
	 * Gets the ID of the room to join.
	 * @return the room ID
	 */
	public int roomId() {
		return roomId;
	}

	/**
	 * Sets the ID of the room to join.
	 * @param roomId the room ID
	 * @return this
	 */
	public JoinRoom roomId(int roomId) {
		this.roomId = roomId;
		return this;
	}

	/**
	 * Gets the action(s) to perform when the room is joined successfully.
	 * @return the actions
	 */
	public Supplier<ChatActions> onSuccess() {
		return onSuccess;
	}

	/**
	 * Sets the action(s) to perform when the room is joined successfully.
	 * @param actions the actions
	 * @return this
	 */
	public JoinRoom onSuccess(Supplier<ChatActions> actions) {
		onSuccess = actions;
		return this;
	}

	/**
	 * Gets the action(s) to perform if the room does not exist.
	 * @return the actions
	 */
	public Supplier<ChatActions> ifRoomDoesNotExist() {
		return ifRoomDoesNotExist;
	}

	/**
	 * Sets the action(s) to perform if the room does not exist.
	 * @param actions the actions
	 * @return this
	 */
	public JoinRoom ifRoomDoesNotExist(Supplier<ChatActions> actions) {
		ifRoomDoesNotExist = actions;
		return this;
	}

	/**
	 * Gets the action(s) to perform if the bot lacks the appropriate
	 * permissions to post messages in the room.
	 * <p>
	 * Note that the bot will automatically leave a room if it can't post to it
	 * @return the actions
	 */
	public Supplier<ChatActions> ifLackingPermissionToPost() {
		return ifLackingPermissionToPost;
	}

	/**
	 * Sets the action(s) to perform if the bot lacks the appropriate
	 * permissions to post messages in the room.
	 * <p>
	 * Note that the bot will automatically leave a room if it can't post to it
	 * @param actions the actions
	 * @return this
	 */
	public JoinRoom ifLackingPermissionToPost(Supplier<ChatActions> actions) {
		ifLackingPermissionToPost = actions;
		return this;
	}

	/**
	 * Sets the action(s) to perform if there is an error joining the room.
	 * @return the actions
	 */
	public Function<Exception, ChatActions> onError() {
		return onError;
	}

	/**
	 * Gets the action(s) to perform if there is an error joining the room.
	 * @param actions the actions
	 * @return this
	 */
	public JoinRoom onError(Function<Exception, ChatActions> actions) {
		onError = actions;
		return this;
	}
}
