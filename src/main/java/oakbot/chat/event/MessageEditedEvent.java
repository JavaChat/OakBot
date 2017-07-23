package oakbot.chat.event;

import oakbot.chat.ChatMessage;

/**
 * Represents an event that is triggered when a message is edited.
 * @author Michael Angstadt
 */
public class MessageEditedEvent extends Event {
	private final ChatMessage message;

	private MessageEditedEvent(Builder builder) {
		super(builder);
		message = builder.message;
	}

	/**
	 * Gets the chat message that was edited.
	 * @return the message
	 */
	public ChatMessage getMessage() {
		return message;
	}

	/**
	 * Used for constructing {@link MessageEditedEvent} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder extends Event.Builder<MessageEditedEvent, Builder> {
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
		public Builder(MessageEditedEvent original) {
			super(original);
			message = original.message;
		}

		/**
		 * Sets the message that was edited.
		 * @param message the message
		 * @return this
		 */
		public Builder message(ChatMessage message) {
			this.message = message;
			this.timestamp = message.getTimestamp();
			return this;
		}

		@Override
		public MessageEditedEvent build() {
			return new MessageEditedEvent(this);
		}
	}
}
