package oakbot.chat;

import java.time.LocalDateTime;

/**
 * Represents a chat message.
 * @author Michael Angstadt
 */
public class ChatMessage {
	private final LocalDateTime timestamp;
	private final String username, content;
	private final int userId, roomId, edits;
	private final long messageId;

	private ChatMessage(Builder builder) {
		timestamp = builder.timestamp;
		username = builder.username;
		content = builder.content;
		userId = builder.userId;
		roomId = builder.roomId;
		edits = builder.edits;
		messageId = builder.messageId;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public String getUsername() {
		return username;
	}

	public String getContent() {
		return content;
	}

	public int getUserId() {
		return userId;
	}

	public long getMessageId() {
		return messageId;
	}

	public int getRoomId() {
		return roomId;
	}

	public int getEdits() {
		return edits;
	}

	public static class Builder {
		private LocalDateTime timestamp = LocalDateTime.now();
		private String username, content;
		private int userId, roomId, edits;
		private long messageId;

		public Builder() {
			//empty
		}

		public Builder(ChatMessage original) {
			timestamp = original.timestamp;
			username = original.username;
			content = original.content;
			userId = original.userId;
			roomId = original.roomId;
			edits = original.edits;
			messageId = original.messageId;
		}

		public Builder timestamp(LocalDateTime timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder username(String username) {
			this.username = username;
			return this;
		}

		public Builder content(String content) {
			this.content = content;
			return this;
		}

		public Builder userId(int userId) {
			this.userId = userId;
			return this;
		}

		public Builder roomId(int roomId) {
			this.roomId = roomId;
			return this;
		}

		public Builder edits(int edits) {
			this.edits = edits;
			return this;
		}

		public Builder messageId(long messageId) {
			this.messageId = messageId;
			return this;
		}

		public ChatMessage build() {
			return new ChatMessage(this);
		}
	}
}
