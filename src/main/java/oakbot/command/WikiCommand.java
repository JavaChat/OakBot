package oakbot.command;

import com.google.common.net.UrlEscapers;

import oakbot.bot.Bot;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatBuilder;

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
	public String description() {
		return "Displays a one-box for a Wikipedia page.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append("Displays a one-box for a Wikipedia page.").nl()
			.append("Usage: ").append(trigger).append(name()).append(" TERM").nl()
			.append("Example: ").append(trigger).append(name()).append(" functional programming")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, boolean isAdmin, Bot bot) {
		String content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("Please specify the term you'd like to display.")
			);
			//@formatter:on
		}

		String url = "http://en.wikipedia.org/wiki/" + UrlEscapers.urlPathSegmentEscaper().escape(content);

		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(chatCommand)
			.append(url)
		);
		//@formatter:on
	}
}
