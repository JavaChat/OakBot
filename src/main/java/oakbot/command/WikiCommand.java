package oakbot.command;

import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import org.apache.http.client.utils.URIBuilder;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;

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
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			return reply("Please specify the term you'd like to display.", chatCommand);
		}

		String keyword = content.replace(' ', '_');

		//@formatter:off
		String url = new URIBuilder()
			.setScheme("https")
			.setHost("en.wikipedia.org")
			.setPathSegments("wiki", keyword)
		.toString();
		//@formatter:on

		return post(url);
	}
}
