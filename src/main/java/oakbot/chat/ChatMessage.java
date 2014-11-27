package oakbot.chat;

import java.time.LocalDateTime;

/**
 * Represents a chat message.
 * @author Michael Angstadt
 */
public class ChatMessage {
	private LocalDateTime timestamp;
	private String username, content;
	private int userId, roomId, edits;
	private long messageId;

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String userName) {
		this.username = userName;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public int getEdits() {
		return edits;
	}

	public void setEdits(int edits) {
		this.edits = edits;
	}
}
