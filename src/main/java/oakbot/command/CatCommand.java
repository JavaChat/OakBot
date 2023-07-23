package oakbot.command;

import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.xml.sax.SAXException;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatBuilder;
import oakbot.util.Http;
import oakbot.util.HttpFactory;
import oakbot.util.Leaf;

/**
 * Displays a random cat picture.
 * @author Michael Angstadt
 * @see <a href=
 * "http://thecatapi.com/docs.html">http://thecatapi.com/docs.html</a>
 */
public class CatCommand implements Command {
	private static final Logger logger = Logger.getLogger(CatCommand.class.getName());

	private final String requestUrl;

	public CatCommand(String key) {
		//@formatter:off
		URIBuilder ub = new URIBuilder()
			.setScheme("http")
			.setHost("thecatapi.com")
			.setPath("/api/images/get")
			.setParameter("size", "small")
			.setParameter("format", "xml")
			.setParameter("type", "gif");
		//@formatter:on

		if (key != null && !key.isEmpty()) {
			ub.setParameter("api_key", key);
		}

		requestUrl = ub.toString();
	}

	@Override
	public String name() {
		return "cat";
	}

	@Override
	public List<String> aliases() {
		return Arrays.asList("meow");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays a random cat picture. :3")
			.detail("Images from thecatapi.com.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		int repeats = 0;
		try (Http http = HttpFactory.connect()) {
			while (repeats < 5) {
				String catUrl = nextCat(http);
				if (isCatThere(http, catUrl)) {
					//@formatter:off
					return ChatActions.create(
						new PostMessage(catUrl).bypassFilters(true)
					);
					//@formatter:on
				}

				repeats++;
			}
		} catch (IOException | SAXException e) {
			logger.log(Level.SEVERE, "Problem getting cat.", e);

			//@formatter:off
			return post(new ChatBuilder()
				.reply(chatCommand)
				.append("Error getting cat: ")
				.code(e.getMessage())
			);
			//@formatter:on
		}

		return reply("No cats found. Try again. :(", chatCommand);
	}

	/**
	 * Gets a random cat picture.
	 * @param http the HTTP client
	 * @return the URL to the cat picture
	 * @throws IOException if there's a network error
	 * @throws SAXException if there's a problem parsing the XML response
	 */
	private String nextCat(Http http) throws IOException, SAXException {
		Leaf document = http.get(requestUrl).getBodyAsXml();
		Leaf urlElement = document.selectFirst("/response/data/images/image/url");
		return urlElement.text();
	}

	/**
	 * Checks to see if the given image exists. Some of the URLs that the API
	 * returns don't work anymore.
	 * @param client the HTTP client
	 * @param url the URL to the image
	 * @return true if the image exists, false if not
	 */
	private boolean isCatThere(Http http, String url) {
		try {
			Http.Response response = http.head(url);
			return response.getStatusCode() == HttpStatus.SC_OK;
		} catch (IOException e) {
			return false;
		}
	}
}
