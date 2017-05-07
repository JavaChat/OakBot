package oakbot.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a chat message. Use its {@link Builder} class to construct new
 * instances.
 * @author Michael Angstadt
 */
public class ChatMessage {
	private final long messageId;
	private final LocalDateTime timestamp;
	private final int userId;
	private final String username;
	private final int roomId;
	private final String content;
	private final boolean fixedFont;

	private ChatMessage(Builder builder) {
		messageId = builder.messageId;
		timestamp = builder.timestamp;
		userId = builder.userId;
		username = builder.username;
		roomId = builder.roomId;
		content = builder.content;
		fixedFont = builder.fixedFont;
	}

	/**
	 * Gets the ID of the message. This ID is unique across all chat rooms.
	 * @return the ID
	 */
	public long getMessageId() {
		return messageId;
	}

	/**
	 * Gets the timestamp the message was posted.
	 * @return the timestamp
	 */
	public LocalDateTime getTimestamp() {
		return timestamp;
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
	 * Gets the room the message was posted in.
	 * @return the room ID
	 */
	public int getRoomId() {
		return roomId;
	}

	/**
	 * <p>
	 * Gets the message content.
	 * </p>
	 * <p>
	 * Messages that consist of a single line of content may contain basic HTML
	 * formatting (even though Stack Overflow Chat only accepts messages
	 * formatted in Markdown syntax, when chat messages are retrieved from the
	 * API, they are formatted in HTML).
	 * </p>
	 * <p>
	 * Messages that contain multiple lines of text will not contain any
	 * formatting because Stack Overflow Chat does not allow multi-lined
	 * messages to contain formatting.
	 * </p>
	 * <p>
	 * Messages that are formatted using a fixed font will not contain any
	 * formatting either. Fixed font messages may contain multiple lines. If a
	 * message is formatted in fixed font, the {@link #isFixedFont} method will
	 * return true.
	 * </p>
	 * <p>
	 * Messages that contain a onebox will contain significant HTML code.
	 * </p>
	 * 
	 * @return the content or null if the author deleted the message
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Gets whether the entire message is formatted in a monospace font.
	 * @return true if it's formatted in a fixed font, false if not
	 */
	public boolean isFixedFont() {
		return fixedFont;
	}

	/**
	 * <p>
	 * Parses any mentions out of the message.
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
	 * because usernames are not unique on Stack Overflow, it's possible for a
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
	 * Determines if a user is mentioned in the message.
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

	@Override
	public String toString() {
		return "ChatMessage [messageId=" + messageId + ", timestamp=" + timestamp + ", userId=" + userId + ", username=" + username + ", roomId=" + roomId + ", content=" + content + ", fixedFont=" + fixedFont + "]";
	}

	/**
	 * Used for constructing {@link ChatMessage} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private long messageId;
		private LocalDateTime timestamp;
		private int userId;
		private String username;
		private int roomId;
		private String content;
		private boolean fixedFont;

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
			messageId = original.messageId;
			timestamp = original.timestamp;
			userId = original.userId;
			username = original.username;
			roomId = original.roomId;
			content = original.content;
			fixedFont = original.fixedFont;
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
		 * Sets the timestamp the message was posted.
		 * @param timestamp the timestamp
		 * @return this
		 */
		public Builder timestamp(LocalDateTime timestamp) {
			this.timestamp = timestamp;
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
		 * Sets the room the message was posted in.
		 * @param roomId the room ID
		 * @return this
		 */
		public Builder roomId(int roomId) {
			this.roomId = roomId;
			return this;
		}

		/**
		 * Sets the content of the message.
		 * @param content the content or null if the author deleted the message
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
			this.content = content;
			this.fixedFont = fixedFont;
			return this;
		}

		/**
		 * Builds the object.
		 * @return the built object
		 */
		public ChatMessage build() {
			return new ChatMessage(this);
		}
	}
}
