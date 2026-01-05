package oakbot.filter;

import java.util.regex.Pattern;

import oakbot.util.ChatBuilder;

/**
 * Parses out the different parts of the content of a chat message.
 * @param fixedWidth true if the message is fixed width, false if not
 * @param replyPrefix the reply prefix (including colon and space, e.g.
 * ":1234 ") or null if not present
 * @param messageContent the message content
 * @param rawMessage the full message content
 * @author Michael Angstadt
 */
public record MessageParts(boolean fixedWidth, String replyPrefix, String messageContent, String rawMessage) {
	private static final Pattern REPLY_REGEX = Pattern.compile("^:\\d+\\s");

	public static MessageParts parse(String message) {
		var fixedWidth = message.startsWith(ChatBuilder.FIXED_WIDTH_PREFIX);

		String replyPrefix;
		String messageContent;
		var m = REPLY_REGEX.matcher(message);
		if (m.find()) {
			replyPrefix = m.group();
			messageContent = message.substring(m.end());
		} else {
			replyPrefix = null;
			messageContent = message;
		}

		return new MessageParts(fixedWidth, replyPrefix, messageContent, message);
	}
}