package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.bot.Shutdown;

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
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		boolean admin = bot.getAdminUsers().contains(chatCommand.getMessage().getUserId());
		if (admin) {
			String message = "Shutting down. See you later.";
			boolean broadcast = chatCommand.getContent().equalsIgnoreCase("broadcast");

			//@formatter:off
			return ChatActions.create(
				new PostMessage(message).broadcast(broadcast),
				new Shutdown()
			);
			//@formatter:on
		}

		return reply("Only admins can shut me down. :P", chatCommand);
	}
}
