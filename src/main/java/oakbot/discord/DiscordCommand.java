package oakbot.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import oakbot.command.HelpDoc;

/**
 * @author Michael Angstadt
 */
public interface DiscordCommand {
	String name();
	
	HelpDoc help();

	void onMessage(String content, MessageReceivedEvent event, BotContext context);
}
