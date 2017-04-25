package oakbot.command;

import static oakbot.command.Command.reply;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.SplitStrategy;
import oakbot.util.ChatBuilder;

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
		return "Displays the description of a StackOverflow tag.";
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
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			return reply("Please specify the tag name (e.g. \"java\").", chatCommand);
		}

		String tag = content.toLowerCase().replace(' ', '-');
		Escaper escaper = UrlEscapers.urlPathSegmentEscaper();
		String url = "http://stackoverflow.com/tags/" + escaper.escape(tag) + "/info";

		String response;
		try {
			response = get(url);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error getting tag description.", e);

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("An error occurred retrieving the tag description: ")
				.code(e.getMessage())
			);
			//@formatter:on
		}

		Document document = Jsoup.parse(response);
		Element element = document.getElementById("wiki-excerpt");
		if (element == null) {
			return reply("Tag not found. :(", chatCommand);
		}

		String definition = element.text();
		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(chatCommand)
			.tag(tag)
			.append(' ').append(definition)
		, SplitStrategy.WORD);
		//@formatter:on
	}

	/**
	 * Sends an HTTP GET request.
	 * @param url the URL
	 * @return the response body
	 * @throws IOException if there's a problem sending the request
	 */
	String get(String url) throws IOException {
		HttpUriRequest request = new HttpGet(url);
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpResponse response = client.execute(request);
			return EntityUtils.toString(response.getEntity());
		}
	}
}
