package oakbot.command;

import static oakbot.command.Command.reply;

import com.google.common.net.UrlEscapers;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;

/**
 * Displays on-boxed Wikipedia pages.
 * @author Michael Angstadt
 */
public class WikiCommand implements Command {
	@Override
	public String name() {
		return "wiki";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays a one-box for a Wikipedia page.")
			.example("James Gosling", "Displays a one-box for the \"James Gosling\" Wikipedia page.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			return reply("Please specify the term you'd like to display.", chatCommand);
		}

		content = content.replace(' ', '_');
		content = UrlEscapers.urlPathSegmentEscaper().escape(content);

		String url = "http://en.wikipedia.org/wiki/" + content;
		return new ChatResponse(url);
	}
}
