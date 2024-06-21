package oakbot.discord;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * @author Michael Angstadt
 */
public class WaveListener implements DiscordListener {
	@Override
	public void onMessage(MessageReceivedEvent event, BotContext context) {
		var message = event.getMessage().getContentDisplay();
		if ("o/".equals(message) || "\\o".equals(message)) {
			event.getMessage().addReaction(Emoji.fromUnicode("U+1F44B")).queue();
		}
	}
}
