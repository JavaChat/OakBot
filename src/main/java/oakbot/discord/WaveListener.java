package oakbot.discord;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import oakbot.command.HelpDoc;

/**
 * @author Michael Angstadt
 */
public class WaveListener implements DiscordListener {
	@Override
	public String name() {
		return "wave";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new DiscordHelpDoc.Builder(this)
			.summary("Waves back at you. o/")
			.detail("Adds a \"wave\" reaction to your post when you post o/ or \\o.")
			.includeSummaryWithDetail(false)
		.build();
		//@formatter:on
	}

	@Override
	public void onMessage(MessageReceivedEvent event, BotContext context) {
		var message = event.getMessage().getContentDisplay();
		if ("o/".equals(message) || "\\o".equals(message)) {
			event.getMessage().addReaction(Emoji.fromUnicode("U+1F44B")).queue();
		}
	}
}
