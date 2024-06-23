package oakbot.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import oakbot.command.HelpDoc;

/**
 * @author Michael Angstadt
 */
public class ShutdownCommand implements DiscordCommand {
	@Override
	public String name() {
		return "shutdown";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new DiscordHelpDoc.Builder(this)
			.summary("Terminates the bot (admins only).")
		.build();
		//@formatter:on
	}

	@Override
	public void onMessage(String content, MessageReceivedEvent event, BotContext context) {
		if (context.authorIsAdmin()) {
			event.getChannel().sendMessage("Shutting down...").queue();
			event.getJDA().shutdown();
		} else {
			event.getChannel().sendMessage("Only admins can shut me down.").queue();
		}
	}
}
