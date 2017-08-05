package oakbot.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Represents a chat message. Use its {@link Builder} class to construct new
 * instances.
 * @author Michael Angstadt
 */
public class ChatMessage {
	private static final Predicate<String> oneboxRegex = Pattern.compile("^<div class=\"([^\"]*?)onebox([^\"]*?)\"[^>]*?>").asPredicate();

	private final LocalDateTime timestamp;

	private final long messageId;
	private final long parentMessageId;

	private final int userId;
	private final String username;
	private final int mentionedUserId;

	private final int roomId;
	private final String roomName;

	private final String content;
	private final boolean fixedFont;

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
		fixedFont = builder.fixedFont;

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

	/**
	 * Gets whether the entire message is formatted in a monospace font.
	 * @return true if it's formatted in a fixed font, false if not
	 */
	public boolean isFixedFont() {
		return fixedFont;
	}

	/**
	 * Determines if the message content is a onebox.
	 * @return true if the message content is a onebox, false if not
	 */
	public boolean isOnebox() {
		return (content == null) ? false : oneboxRegex.test(content);
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
		if (content == null) {
			return new ArrayList<>(0);
		}

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
		return "ChatMessage [timestamp=" + timestamp + ", messageId=" + messageId + ", parentMessageId=" + parentMessageId + ", userId=" + userId + ", username=" + username + ", mentionedUserId=" + mentionedUserId + ", roomId=" + roomId + ", roomName=" + roomName + ", content=" + content + ", fixedFont=" + fixedFont + ", edits=" + edits + ", stars=" + stars + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + edits;
		result = prime * result + (fixedFont ? 1231 : 1237);
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
		if (fixedFont != other.fixedFont) return false;
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

		private String content;
		private boolean fixedFont;

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
			fixedFont = original.fixedFont;

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
