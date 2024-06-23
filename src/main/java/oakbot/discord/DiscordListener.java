package oakbot.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import oakbot.command.HelpDoc;

/**
 * @author Michael Angstadt
 */
public interface DiscordListener {
	default String name() {
		return null;
	}

	default HelpDoc help() {
		return null;
	}

	void onMessage(MessageReceivedEvent event, BotContext context);
}
