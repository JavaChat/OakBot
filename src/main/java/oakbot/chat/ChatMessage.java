package oakbot.chat;

import java.time.LocalDateTime;

/**
 * Represents a chat message. Use its {@link Builder} class to construct new
 * instances.
 * @author Michael Angstadt
 */
public class ChatMessage {
	private final LocalDateTime timestamp;

	private final long messageId;
	private final long parentMessageId;

	private final int userId;
	private final String username;
	private final int mentionedUserId;

	private final int roomId;
	private final String roomName;

	private final Content content;

	private final int edits;
	private final int stars;

	private ChatMessage(Builder builder) {
		timestamp = builder.timestamp;

		messageId = builder.messageId;
		parentMessageId = builder.parentMessageId;

		userId = builder.userId;
		username = builder.username;
		mentionedUserId = builder.mentionedUserId;

		roomId = builder.roomId;
		roomName = builder.roomName;

		content = builder.content;

		edits = builder.edits;
		stars = builder.stars;
	}

	/**
	 * Gets the timestamp the message was posted or modified.
	 * @return the timestamp
	 */
	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	/**
	 * Gets the ID of the message. This ID is unique across all chat rooms.
	 * @return the ID
	 */
	public long getMessageId() {
		return messageId;
	}

	/**
	 * Gets the ID of the message that this message is replying to.
	 * @return the parent message ID or 0 if this message is not a reply
	 */
	public long getParentMessageId() {
		return parentMessageId;
	}

	/**
	 * Gets the user ID of the message author.
	 * @return the user ID
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Gets the username of the message author.
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Gets the ID of the user that was mentioned in the message content. If a
	 * message contains multiple mentions, only the ID of the first mentioned
	 * user is returned by the API.
	 * @return the ID of the mentioned user or 0 if nobody was mentioned
	 */
	public int getMentionedUserId() {
		return mentionedUserId;
	}

	/**
	 * Gets the ID of the room the message is currently in.
	 * @return the room ID
	 */
	public int getRoomId() {
		return roomId;
	}

	/**
	 * Gets the name of the room the message is currently in.
	 * @return the room name
	 */
	public String getRoomName() {
		return roomName;
	}

	/**
	 * Gets the message content.
	 * @return the content or null if the author deleted the message
	 */
	public Content getContent() {
		return content;
	}

	/**
	 * Gets the number of times the message was edited.
	 * @return the number of times the message was edited
	 */
	public int getEdits() {
		return edits;
	}

	/**
	 * Gets the number of stars the message has.
	 * @return the number of stars
	 */
	public int getStars() {
		return stars;
	}

	@Override
	public String toString() {
		return "ChatMessage [timestamp=" + timestamp + ", messageId=" + messageId + ", parentMessageId=" + parentMessageId + ", userId=" + userId + ", username=" + username + ", mentionedUserId=" + mentionedUserId + ", roomId=" + roomId + ", roomName=" + roomName + ", content=" + content + ", edits=" + edits + ", stars=" + stars + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + edits;
		result = prime * result + mentionedUserId;
		result = prime * result + (int) (messageId ^ (messageId >>> 32));
		result = prime * result + (int) (parentMessageId ^ (parentMessageId >>> 32));
		result = prime * result + roomId;
		result = prime * result + ((roomName == null) ? 0 : roomName.hashCode());
		result = prime * result + stars;
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + userId;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ChatMessage other = (ChatMessage) obj;
		if (content == null) {
			if (other.content != null) return false;
		} else if (!content.equals(other.content)) return false;
		if (edits != other.edits) return false;
		if (mentionedUserId != other.mentionedUserId) return false;
		if (messageId != other.messageId) return false;
		if (parentMessageId != other.parentMessageId) return false;
		if (roomId != other.roomId) return false;
		if (roomName == null) {
			if (other.roomName != null) return false;
		} else if (!roomName.equals(other.roomName)) return false;
		if (stars != other.stars) return false;
		if (timestamp == null) {
			if (other.timestamp != null) return false;
		} else if (!timestamp.equals(other.timestamp)) return false;
		if (userId != other.userId) return false;
		if (username == null) {
			if (other.username != null) return false;
		} else if (!username.equals(other.username)) return false;
		return true;
	}

	/**
	 * Used for constructing {@link ChatMessage} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private LocalDateTime timestamp;

		private long messageId;
		private long parentMessageId;

		private int userId;
		private String username;
		private int mentionedUserId;

		private int roomId;
		private String roomName;

		private Content content;

		private int edits;
		private int stars;

		/**
		 * Creates an empty builder.
		 */
		public Builder() {
			//empty
		}

		/**
		 * Initializes the builder from an existing chat message.
		 * @param original the original object
		 */
		public Builder(ChatMessage original) {
			timestamp = original.timestamp;

			messageId = original.messageId;
			parentMessageId = original.parentMessageId;

			userId = original.userId;
			username = original.username;
			mentionedUserId = original.mentionedUserId;

			roomId = original.roomId;
			roomName = original.roomName;

			content = original.content;

			edits = original.edits;
			stars = original.stars;
		}

		/**
		 * Sets the time the message was posted or modified.
		 * @param timestamp the timestamp
		 * @return this
		 */
		public Builder timestamp(LocalDateTime timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		/**
		 * Sets the ID of the message. This ID is unique across all chat rooms.
		 * @param messageId the message ID
		 * @return this
		 */
		public Builder messageId(long messageId) {
			this.messageId = messageId;
			return this;
		}

		/**
		 * Sets the ID of the message that this message is replying to. This ID
		 * is unique across all chat rooms.
		 * @param parentMessageId the parent message ID or 0 if this message is
		 * not a reply
		 * @return this
		 */
		public Builder parentMessageId(long parentMessageId) {
			this.parentMessageId = parentMessageId;
			return this;
		}

		/**
		 * Sets the user ID of the message author.
		 * @param userId the user ID
		 * @return this
		 */
		public Builder userId(int userId) {
			this.userId = userId;
			return this;
		}

		/**
		 * Sets the username of the message author.
		 * @param username the username
		 * @return this
		 */
		public Builder username(String username) {
			this.username = username;
			return this;
		}

		/**
		 * Sets the ID of the user that was mentioned in the message content. If
		 * a message contains multiple mentions, only the ID of the first
		 * mentioned user is recorded.
		 * @param mentionedUserId the ID of the mentioned user or 0 if nobody
		 * was mentioned
		 * @return this
		 */
		public Builder mentionedUserId(int mentionedUserId) {
			this.mentionedUserId = mentionedUserId;
			return this;
		}

		/**
		 * Sets the ID of the room the message is currently in.
		 * @param roomId the room ID
		 * @return this
		 */
		public Builder roomId(int roomId) {
			this.roomId = roomId;
			return this;
		}

		/**
		 * Sets the name of the room the message is currently in.
		 * @param roomName the room name
		 * @return this
		 */
		public Builder roomName(String roomName) {
			this.roomName = roomName;
			return this;
		}

		/**
		 * Sets the content of the message.
		 * @param content the content or null if the author deleted the message
		 * @return this
		 */
		public Builder content(Content content) {
			this.content = content;
			return this;
		}

		/**
		 * Sets the content of the message.
		 * @param content the content or null if the author deleted the message
		 * @param fixedFont true if the content is formatted in a fixed font,
		 * false if not
		 * @return this
		 */
		public Builder content(String content) {
			return content(content, false);
		}

		/**
		 * Sets the content of the message.
		 * @param content the content or null if the author deleted the message
		 * @param fixedFont true if the content is formatted in a fixed font,
		 * false if not
		 * @return this
		 */
		public Builder content(String content, boolean fixedFont) {
			return content((content == null) ? null : new Content(content, fixedFont));
		}

		/**
		 * Sets the number of times the message was edited.
		 * @param edits the number of edits
		 * @return this
		 */
		public Builder edits(int edits) {
			this.edits = edits;
			return this;
		}

		/**
		 * Sets the number of stars the message has.
		 * @param stars the number of stars
		 * @return this
		 */
		public Builder stars(int stars) {
			this.stars = stars;
			return this;
		}

		/**
		 * Builds the chat message.
		 * @return the built object
		 */
		public ChatMessage build() {
			return new ChatMessage(this);
		}
	}
}
