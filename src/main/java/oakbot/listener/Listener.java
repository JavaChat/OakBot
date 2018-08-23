package oakbot.listener;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Listens to each new message and optionally responds to it.
 * @author Michael Angstadt
 */
public interface Listener {
	/**
	 * Gets the listener's name to display in the help documentation.
	 * @return the name or null not to display this listener in the help
	 * documentation
	 */
	default String name() {
		return null;
	}

	/**
	 * Gets the listener's help documentation.
	 * @return the help documentation or null if this listener does not have any
	 * help documentation
	 */
	default HelpDoc help() {
		return null;
	}

	/**
	 * Called whenever a new message is received.
	 * @param message the message
	 * @param context the bot context
	 */
	ChatResponse onMessage(ChatMessage message, BotContext context);

	/**
	 * Utility method for creating a simple reply to a message.
	 * @param content the message to put in the response
	 * @param message the message that the response is in reply to
	 * @return the response
	 */
	static ChatResponse reply(String content, ChatMessage message) {
		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(message)
			.append(content)
		);
		//@formatter:on
	}
}