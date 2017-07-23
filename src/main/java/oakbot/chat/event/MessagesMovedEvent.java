package oakbot.chat.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oakbot.chat.ChatMessage;

/**
 * Represents an event that is triggered when one or more messages are moved
 * into the room or out of the room.
 * @author Michael Angstadt
 */
public class MessagesMovedEvent extends Event {
	private final List<ChatMessage> messages;
	private final int sourceRoomId;
	private final String sourceRoomName;
	private final int destRoomId;
	private final String destRoomName;
	private final int moverUserId;
	private final String moverUsername;

	private MessagesMovedEvent(Builder builder) {
		super(builder);
		messages = builder.messages;
		sourceRoomId = builder.sourceRoomId;
		sourceRoomName = builder.sourceRoomName;
		destRoomId = builder.destRoomId;
		destRoomName = builder.destRoomName;
		moverUserId = builder.moverUserId;
		moverUsername = builder.moverUsername;
	}

	/**
	 * Gets the messages that were moved.
	 * @return the moved messages
	 */
	public List<ChatMessage> getMessages() {
		return messages;
	}

	/**
	 * Gets the ID of the room the messages came from.
	 * @return the source room ID
	 */
	public int getSourceRoomId() {
		return sourceRoomId;
	}

	/**
	 * Gets the name of the room the messages came from.
	 * @return the source room name
	 */
	public String getSourceRoomName() {
		return sourceRoomName;
	}

	/**
	 * Gets the ID of the room the messages were moved to.
	 * @return the destination room ID
	 */
	public int getDestRoomId() {
		return destRoomId;
	}

	/**
	 * Gets the ID of the room the messages were moved to.
	 * @return the destination room name
	 */
	public String getDestRoomName() {
		return destRoomName;
	}

	/**
	 * Gets the ID of the user who moved the messages.
	 * @return the user ID
	 */
	public int getMoverUserId() {
		return moverUserId;
	}

	/**
	 * Gets the name of the user who moved the messages.
	 * @return the username
	 */
	public String getMoverUsername() {
		return moverUsername;
	}

	public static class Builder extends Event.Builder<MessagesMovedEvent, Builder> {
		private List<ChatMessage> messages;
		private int sourceRoomId;
		private String sourceRoomName;
		private int destRoomId;
		private String destRoomName;
		private int moverUserId;
		private String moverUsername;

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
		public Builder(MessagesMovedEvent original) {
			messages = original.messages;
			sourceRoomId = original.sourceRoomId;
			sourceRoomName = original.sourceRoomName;
			destRoomId = original.destRoomId;
			destRoomName = original.destRoomName;
			moverUserId = original.moverUserId;
			moverUsername = original.moverUsername;
		}

		/**
		 * Sets the messages that were moved.
		 * @param messages the moved messages
		 * @return this
		 */
		public Builder messages(List<ChatMessage> messages) {
			this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
			return this;
		}

		/**
		 * Sets the ID of the room the messages came from.
		 * @param sourceRoomId the source room ID
		 * @return this
		 */
		public Builder sourceRoomId(int sourceRoomId) {
			this.sourceRoomId = sourceRoomId;
			return this;
		}

		/**
		 * Sets the name of the room the messages came from.
		 * @param sourceRoomName the source room name
		 * @return this
		 */
		public Builder sourceRoomName(String sourceRoomName) {
			this.sourceRoomName = sourceRoomName;
			return this;
		}

		/**
		 * Sets the ID of the room the messages were moved to.
		 * @param destRoomId the destination room ID
		 * @return this
		 */
		public Builder destRoomId(int destRoomId) {
			this.destRoomId = destRoomId;
			return this;
		}

		/**
		 * Sets the ID of the room the messages were moved to.
		 * @param destRoomName the destination room name
		 * @return this
		 */
		public Builder destRoomName(String destRoomName) {
			this.destRoomName = destRoomName;
			return this;
		}

		/**
		 * Sets the ID of the user who moved the messages.
		 * @param moverUserId the user ID
		 * @return this
		 */
		public Builder moverUserId(int moverUserId) {
			this.moverUserId = moverUserId;
			return this;
		}

		/**
		 * Sets the name of the user who moved the messages.
		 * @param moverUsername the username
		 * @return this
		 */
		public Builder moverUsername(String moverUsername) {
			this.moverUsername = moverUsername;
			return this;
		}

		@Override
		public MessagesMovedEvent build() {
			return new MessagesMovedEvent(this);
		}
	}
}
