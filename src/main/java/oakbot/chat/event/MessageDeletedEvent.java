package oakbot.chat.event;

import oakbot.chat.ChatMessage;

/**
 * Represents an event that is triggered when a message is deleted.
 * @author Michael Angstadt
 */
public class MessageDeletedEvent extends Event {
	private final ChatMessage message;

	private MessageDeletedEvent(Builder builder) {
		super(builder);
		message = builder.message;
	}

	/**
	 * <p>
	 * Gets the chat message that was deleted.
	 * </p>
	 * <p>
	 * Note that the returned object will contain null or otherwise non-existent
	 * values for the following fields:
	 * </p>
	 * <ul>
	 * <li>{@link ChatMessage#getContent() content}</li>
	 * <li>{@link ChatMessage#getMentionedUserId() mentionedUserId}</li>
	 * <li>{@link ChatMessage#getParentMessageId() parentMessageId}</li>
	 * </ul>
	 * @return the message
	 */
	public ChatMessage getMessage() {
		return message;
	}

	/**
	 * Used for constructing {@link MessageDeletedEvent} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder extends Event.Builder<MessageDeletedEvent, Builder> {
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
		public Builder(MessageDeletedEvent original) {
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
		public MessageDeletedEvent build() {
			return new MessageDeletedEvent(this);
		}
	}
}
