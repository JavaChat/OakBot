package oakbot.command;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatBuilder;

/**
 * Shuts down the bot.
 * @author Michael Angstadt
 */
public class ShutdownCommand implements Command {

	@Override
	public String name() {
		return "shutdown";
	}

	@Override
	public String description() {
		return "Terminates the bot (admins only).";
	}

	@Override
	public String helpText(String trigger) {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		if (context.isAuthorAdmin()) {
			context.shutdownBot("Shutting down.  See you later.");
			return null;
		}

		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(chatCommand)
			.append("Only admins can shut me down. :P")
		);
		//@formatter:on
	}
}
