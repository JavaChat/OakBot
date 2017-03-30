package oakbot.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

	/**
	 * <p>
	 * Parses the mentions out of the chat message.
	 * </p>
	 * <p>
	 * "Mentioning" someone in chat will make a "ping" sound on the mentioned
	 * user's computer. A mention consists of an "at" symbol followed by a
	 * username. For example, this chat message contains two mentions:
	 * </p>
	 * 
	 * <pre>
	 * Good morning, {@literal @}Frank and {@literal @}Bob!
	 * </pre>
	 * <p>
	 * Mentions cannot contain spaces, so if a username contains spaces, those
	 * spaces are removed from the mention.
	 * </p>
	 * <p>
	 * A mention does not have to contain a user's entire username. It may only
	 * contain the beginning of the username. For example, if someone's username
	 * is "Bob Smith", then typing "{@literal @}BobS" will ping that user.
	 * </p>
	 * <p>
	 * Because mentions can contain only part of a person's username, and
	 * because usernames are not unique on Stackoverflow, it's possible for a
	 * mention to refer to more than one user.
	 * </p>
	 * <p>
	 * Mentions must be at least 3 characters long (not including the "at"
	 * symbol). Mentions less than 3 characters long are treated as normal text.
	 * </p>
	 * @return the mentions or empty list of none were found. The "at" symbol is
	 * not included in the returned output.
	 */
	public List<String> getMentions() {
		final int minLength = 3;
		List<String> mentions = new ArrayList<>(1);

		boolean inMention = false;
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);

			if (inMention) {
				if (Character.isLetter(c) || Character.isDigit(c)) {
					buffer.append(c);
					continue;
				}

				inMention = false;
				if (buffer.length() >= minLength) {
					mentions.add(buffer.toString());
				}
			}

			if (c == '@') {
				inMention = true;
				buffer.setLength(0);
				continue;
			}
		}

		if (inMention) {
			if (buffer.length() >= minLength) {
				mentions.add(buffer.toString());
			}
		}

		return mentions;
	}

	/**
	 * Determines if a user is mentioned in the chat message.
	 * @param username the username to look for
	 * @return true if the user is mentioned, false if not
	 */
	public boolean isMentioned(String username) {
		List<String> mentions = getMentions();
		if (mentions.isEmpty()) {
			return false;
		}

		username = username.toLowerCase().replace(" ", "");
		for (String mention : mentions) {
			mention = mention.toLowerCase();
			if (username.startsWith(mention)) {
				return true;
			}
		}
		return false;
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
