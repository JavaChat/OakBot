package oakbot.command;

import static oakbot.command.Command.reply;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;

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
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		if (context.isAuthorAdmin()) {
			boolean broadcast = chatCommand.getContent().equals("broadcast");
			context.shutdownBot("Shutting down. See you later.", broadcast);
			return null;
		}

		return reply("Only admins can shut me down. :P", chatCommand);
	}
}
