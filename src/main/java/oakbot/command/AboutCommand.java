package oakbot.command;

import java.util.Date;

import oakbot.Main;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;
import oakbot.util.RelativeDateFormat;

/**
 * @author Michael Angstadt
 */
public class AboutCommand implements Command {
	private final Date startedUp = new Date();

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
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		RelativeDateFormat df = new RelativeDateFormat();
		ChatBuilder cb = new ChatBuilder();

		cb.bold("OakBot").append(" v").append(Main.VERSION).append(" by ").link("Michael", "http://stackoverflow.com/users/13379/michael").append(" | ");
		cb.link("source code", Main.URL).append(" | ");
		cb.append("built: ").append(df.format(Main.BUILT)).append(" | ");
		cb.append("started up: ").append(df.format(startedUp));

		return new ChatResponse(cb);
	}
}
