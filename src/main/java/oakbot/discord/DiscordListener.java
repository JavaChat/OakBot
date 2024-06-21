package oakbot.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * @author Michael Angstadt
 */
public interface DiscordListener {
	void onMessage(MessageReceivedEvent event, BotContext context);
}
