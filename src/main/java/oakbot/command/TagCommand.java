package oakbot.command;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.util.ChatBuilder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

/**
 * Displays StackOverflow tag descriptions (can sort of act like a Computer
 * Science urban dictionary).
 * @author Michael Angstadt
 */
public class TagCommand implements Command {
	private static final Logger logger = Logger.getLogger(TagCommand.class.getName());

	@Override
	public String name() {
		return "tag";
	}

	@Override
	public String description() {
		return "Displays the description of a StackOverflow tag (acts like a Computer Science urban dictionary).";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append("Displays the description of a StackOverflow tag (acts like a Computer Science urban dictionary).").nl()
			.append("Usage: ").append(trigger).append(name()).append(" TAG").nl()
			.append("Example: ").append(trigger).append(name()).append(" functional programming")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		String content = message.getContent().trim();
		if (content.isEmpty()) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("Please specify the tag name.")
			);
			//@formatter:on
		}

		String tag = content.toLowerCase().replace(' ', '-');
		Escaper escaper = UrlEscapers.urlPathSegmentEscaper();
		String url = "http://stackoverflow.com/tags/" + escaper.escape(tag) + "/info";

		Document document;
		try {
			document = Jsoup.parse(get(url), "UTF-8", "");
		} catch (FileNotFoundException e) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("Tag not found. :(")
			);
			//@formatter:on
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error getting tag description.", e);

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("An error occurred retrieving the tag description. D:<")
			);
			//@formatter:on
		}

		Element element = document.getElementById("wiki-excerpt");
		if (element == null) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("Tag not found. :(")
			);
			//@formatter:on
		}

		String definition = element.text();
		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(message)
			.tag(tag)
			.append(' ').append(definition)
		, SplitStrategy.WORD);
		//@formatter:on
	}

	InputStream get(String url) throws IOException {
		URL urlObj = new URL(url);
		return urlObj.openStream();
	}
}
