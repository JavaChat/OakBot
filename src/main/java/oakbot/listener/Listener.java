package oakbot.listener;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * Listens to each new message and optionally responds to it.
 * @author Michael Angstadt
 */
public interface Listener {
	/**
	 * Gets the listener's name.
	 * @return the name
	 */
	String name();

	/**
	 * Gets the listener's description. This should be a short, one sentence
	 * description. SO markdown should not be used.
	 * @return the description
	 */
	String description();

	/**
	 * Gets the listener's help text. This is shown when this listener is
	 * queried with the "help" command. SO markdown should not be used.
	 * @return the help text
	 */
	String helpText();

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