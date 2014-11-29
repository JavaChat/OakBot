package oakbot.util;

import oakbot.chat.ChatMessage;

/**
 * Utility methods for building chat messages.
 * @author Michael Angstadt
 */
public class ChatUtils {
	/**
	 * Builds a "reply" message.
	 * @param replyTo the message to reply to
	 * @param text the reply
	 * @return the message text
	 */
	public static String reply(ChatMessage replyTo, String text) {
		return ":" + replyTo.getMessageId() + " " + text;
	}

	private ChatUtils() {
		//hide
	}
}
