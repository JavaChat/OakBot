package oakbot.discord;

import java.time.LocalDateTime;
import java.time.ZoneId;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import oakbot.Main;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.RelativeDateFormat;

/**
 * Displays meta information about the bot.
 * @author Michael Angstadt
 */
public class AboutCommand implements DiscordCommand {
	private final LocalDateTime startedUp = LocalDateTime.now();

	@Override
	public String name() {
		return "about";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new DiscordHelpDoc.Builder(this)
			.summary("Displays information about this bot.")
		.build();
		//@formatter:on
	}

	@Override
	public void onMessage(String content, MessageReceivedEvent event, BotContext context) {
		var relativeDf = new RelativeDateFormat();
		var built = LocalDateTime.ofInstant(Main.getBuilt(), ZoneId.systemDefault());

		//@formatter:off
		var cb = new ChatBuilder()
		.bold("OakBot").append(" by ").link("Michael", "https://stackoverflow.com/users/13379/michael").nl()
		.link("source code", Main.getUrl()).nl()
		.append("JAR built on: ").append(relativeDf.format(built)).append(" | ")
		.append("started up: ").append(relativeDf.format(startedUp));
		//@formatter:on

		event.getMessage().reply(cb).queue();
	}
}
