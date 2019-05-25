package oakbot.listener;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.chat.ChatMessage;
import oakbot.command.HelpDoc;

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
	 * @return the action(s) to perform in response to the message
	 */
	ChatActions onMessage(ChatMessage message, BotContext context);
}