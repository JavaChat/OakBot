package oakbot.chat.event;

import oakbot.chat.ChatMessage;

/**
 * Represents an event that is triggered when a message is starred or unstarred.
 * @author Michael Angstadt
 */
public class MessageStarredEvent extends Event {
	private final ChatMessage message;

	private MessageStarredEvent(Builder builder) {
		super(builder);
		message = builder.message;
	}

	/**
	 * <p>
	 * Gets the chat message that was starred (or unstarred).
	 * </p>
	 * <p>
	 * Note that the returned object will contain null or otherwise non-existent
	 * values for the following fields:
	 * </p>
	 * <ul>
	 * <li>{@link ChatMessage#getUserId() userId}</li>
	 * <li>{@link ChatMessage#getUsername() userName}</li>
	 * </ul>
	 * @return
	 */
	public ChatMessage getMessage() {
		return message;
	}

	/**
	 * Used for constructing {@link ChatMessage} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder extends Event.Builder<MessageStarredEvent, Builder> {
		private ChatMessage message;

		/**
		 * Creates an empty builder.
		 */
		public Builder() {
			super();
		}

		/**
		 * Initializes the builder from an existing event.
		 * @param original the original event
		 */
		public Builder(MessageStarredEvent original) {
			super(original);
			message = original.message;
		}

		/**
		 * Sets the message that was deleted.
		 * @param message the message
		 * @return this
		 */
		public Builder message(ChatMessage message) {
			this.message = message;
			this.timestamp = message.getTimestamp();
			return this;
		}

		@Override
		public MessageStarredEvent build() {
			return new MessageStarredEvent(this);
		}
	}
}
