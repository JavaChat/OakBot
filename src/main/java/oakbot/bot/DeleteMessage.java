package oakbot.bot;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Instructs the bot to delete a message. It can only delete messages that it
 * has authored and that have been posted within the last 2 minutes.
 * @author Michael Angstadt
 */
public class DeleteMessage implements ChatAction {
	private long messageId;
	private Supplier<ChatActions> onSuccess;
	private Function<Exception, ChatActions> onError;

	/**
	 * @param messageId the ID of the message to delete
	 */
	public DeleteMessage(long messageId) {
		this.messageId = messageId;
		onSuccess = ChatActions::doNothing;
		onError = e -> ChatActions.doNothing();
	}

	/**
	 * Gets the ID of the message to delete.
	 * @return the message ID
	 */
	public long messageId() {
		return messageId;
	}

	/**
	 * Sets the ID of the message to delete
	 * @param messageId the message ID
	 * @return this
	 */
	public DeleteMessage messageId(int messageId) {
		this.messageId = messageId;
		return this;
	}

	/**
	 * Gets the action(s) to perform when the message is deleted successfully.
	 * @return the actions
	 */
	public Supplier<ChatActions> onSuccess() {
		return onSuccess;
	}

	/**
	 * Sets the action(s) to perform when the message is deleted successfully.
	 * @param actions the actions
	 * @return this
	 */
	public DeleteMessage onSuccess(Supplier<ChatActions> actions) {
		onSuccess = actions;
		return this;
	}

	/**
	 * Sets the action(s) to perform if there is an error deleting the message.
	 * @return the actions
	 */
	public Function<Exception, ChatActions> onError() {
		return onError;
	}

	/**
	 * Gets the action(s) to perform if there is an error deleting the message.
	 * @param actions the actions
	 * @return this
	 */
	public DeleteMessage onError(Function<Exception, ChatActions> actions) {
		onError = actions;
		return this;
	}
}
