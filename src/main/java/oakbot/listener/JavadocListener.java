package oakbot.listener;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.command.javadoc.JavadocCommand;

/**
 * Listens for numbers for {@link JavabotCommand} when it returns multiple
 * classes/methods.
 * @author Michael Angstadt
 */
public class JavadocListener implements Listener {
	private final JavadocCommand command;

	public JavadocListener(JavadocCommand command) {
		this.command = command;
	}

	@Override
	public String name() {
		return null;
	}

	@Override
	public String description() {
		return null;
	}

	@Override
	public String helpText() {
		return null;
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		String content = message.getContent();
		try {
			int num = Integer.parseInt(content);
			return command.showChoice(message, num);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
