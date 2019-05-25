package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
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
	public ChatActions onMessage(ChatMessage message, BotContext context) {
		String reply = command.handleResponse(message);
		return (reply == null) ? doNothing() : reply(reply, message);
	}
}
