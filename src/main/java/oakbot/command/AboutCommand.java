package oakbot.command;

import static oakbot.bot.ChatActions.post;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import oakbot.Main;
import oakbot.Statistics;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.util.ChatBuilder;
import oakbot.util.RelativeDateFormat;

/**
 * Displays meta information about the bot.
 * @author Michael Angstadt
 */
public class AboutCommand implements Command {
	private final LocalDateTime startedUp = LocalDateTime.now();
	private final Statistics stats;
	private final String host;

	public AboutCommand(Statistics stats, String host) {
		this.stats = stats;
		this.host = host;
	}

	@Override
	public String name() {
		return "about";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays information about this bot.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		RelativeDateFormat relativeDf = new RelativeDateFormat();
		LocalDateTime built = LocalDateTime.ofInstant(Main.BUILT, ZoneId.systemDefault());

		//@formatter:off
		ChatBuilder cb = new ChatBuilder()
		.bold("OakBot").append(" by ").link("Michael", "http://stackoverflow.com/users/13379/michael").append(" | ")
		.link("source code", Main.URL).append(" | ")
		.append("JAR built on: ").append(relativeDf.format(built)).append(" | ")
		.append("started up: ").append(relativeDf.format(startedUp));
		//@formatter:on

		if (host != null) {
			cb.append(" | ").append("hosted by: ").append(host);
		}

		if (stats != null) {
			cb.append(" | ").append("responded to ").append(stats.getMessagesRespondedTo()).append(" commands");

			LocalDateTime since = stats.getSince();
			if (since != null) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
				cb.append(" since ").append(since.format(formatter));
			}
		}

		return post(cb);
	}
}
