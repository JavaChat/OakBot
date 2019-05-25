package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.chat.ChatMessage;
import oakbot.command.javadoc.JavadocCommand;

/**
 * Listens for numbers for {@link JavadocCommand} when it returns multiple
 * classes/methods.
 * @author Michael Angstadt
 */
public class JavadocListener implements Listener {
	private final JavadocCommand command;

	public JavadocListener(JavadocCommand command) {
		this.command = command;
	}

	@Override
	public ChatActions onMessage(ChatMessage message, BotContext context) {
		String content = message.getContent().getContent();
		try {
			int num = Integer.parseInt(content);
			return command.showChoice(message, num);
		} catch (NumberFormatException e) {
			return doNothing();
		}
	}
}
