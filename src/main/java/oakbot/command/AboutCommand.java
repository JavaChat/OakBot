package oakbot.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import oakbot.Main;
import oakbot.Statistics;
import oakbot.bot.Bot;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.util.ChatBuilder;
import oakbot.util.RelativeDateFormat;

/**
 * Displays meta information about the bot.
 * @author Michael Angstadt
 */
public class AboutCommand implements Command {
	private final Date startedUp = new Date();
	private final Statistics stats;

	public AboutCommand(Statistics stats) {
		this.stats = stats;
	}

	@Override
	public String name() {
		return "about";
	}

	@Override
	public String description() {
		return "Displays information about this bot.";
	}

	@Override
	public String helpText(String trigger) {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin, Bot bot) {
		RelativeDateFormat relativeDf = new RelativeDateFormat();

		//@formatter:off
		ChatBuilder cb = new ChatBuilder()
		.bold("OakBot").append(" by ").link("Michael", "http://stackoverflow.com/users/13379/michael").append(" | ")
		.link("source code", Main.URL).append(" | ")
		.append("JAR built on: ").append(relativeDf.format(Main.BUILT)).append(" | ")
		.append("started up: ").append(relativeDf.format(startedUp));
		//@formatter:on

		if (stats != null) {
			cb.append(" | ").append("responded to ").append(stats.getMessagesRespondedTo()).append(" commands");

			Date since = stats.getSince();
			if (since != null) {
				DateFormat df = new SimpleDateFormat("MMM d, yyyy");
				cb.append(" since ").append(df.format(since));
			}
		}

		return new ChatResponse(cb, SplitStrategy.WORD);
	}
}
