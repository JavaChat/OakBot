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
			boolean broadcast = chatCommand.getContent().equals("broadcast");
			context.shutdownBot("Shutting down. See you later.", broadcast);
			return null;
		}

		return reply("Only admins can shut me down. :P", chatCommand);
	}
}
