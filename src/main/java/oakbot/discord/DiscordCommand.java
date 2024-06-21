package oakbot.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * @author Michael Angstadt
 */
public interface DiscordCommand {
	String name();

	void onMessage(String content, MessageReceivedEvent event, BotContext context);
}
