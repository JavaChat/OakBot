package oakbot.chat.event;

import oakbot.chat.ChatMessage;

/**
 * Represents an event that is triggered when a new message is posted.
 * @author Michael Angstadt
 */
public class MessagePostedEvent extends Event {
	private final ChatMessage message;

	private MessagePostedEvent(Builder builder) {
		super(builder);
		message = builder.message;
	}

	/**
	 * Gets the chat message that was posted.
	 * @return the message
	 */
	public ChatMessage getMessage() {
		return message;
	}

	/**
	 * Used for constructing {@link MessagePostedEvent} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder extends Event.Builder<MessagePostedEvent, Builder> {
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
		public Builder(MessagePostedEvent original) {
			super(original);
			message = original.message;
		}

		/**
		 * Sets the message that was posted.
		 * @param message the message
		 * @return this
		 */
		public Builder message(ChatMessage message) {
			this.message = message;
			this.timestamp = message.getTimestamp();
			return this;
		}

		@Override
		public MessagePostedEvent build() {
			return new MessagePostedEvent(this);
		}
	}
}
