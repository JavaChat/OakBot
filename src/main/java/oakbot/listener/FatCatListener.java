package oakbot.listener;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.command.FatCatCommand;

/**
 * Listens for responses to questions posed by {@link FatCatCommand}.
 * @author Michael Angstadt
 */
public class FatCatListener implements Listener {
	private final FatCatCommand command;

	public FatCatListener(FatCatCommand command) {
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
	public ChatResponse onMessage(ChatMessage message, BotContext context) {
		String reply = command.handleResponse(message);
		return (reply == null) ? null : Listener.reply(reply, message);
	}
}
