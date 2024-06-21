package oakbot.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * @author Michael Angstadt
 */
public class ShutdownCommand implements DiscordCommand {
	@Override
	public String name() {
		return "shutdown";
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
