package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.ShutdownException;

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
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Terminates the bot (admins only).")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, BotContext context) {
		if (context.isAuthorAdmin()) {
			String message = "Shutting down. See you later.";
			boolean broadcast = chatCommand.getContent().equalsIgnoreCase("broadcast");
			throw new ShutdownException(message, broadcast);
		}

		return reply("Only admins can shut me down. :P", chatCommand);
	}
}
