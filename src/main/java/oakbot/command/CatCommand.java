package oakbot.command;

import static oakbot.command.Command.reply;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.net.UrlEscapers;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatBuilder;
import oakbot.util.XPathWrapper;

/**
 * Displays a random cat picture.
 * @author Michael Angstadt
 * @see <a href=
 * "http://thecatapi.com/docs.html">http://thecatapi.com/docs.html</a>
 */
public class CatCommand implements Command {
	private static final Logger logger = Logger.getLogger(CatCommand.class.getName());

	private final DocumentBuilder docBuilder;
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setIgnoringElementContentWhitespace(true);
		try {
			docBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			//shouldn't be thrown
			throw new RuntimeException(e);
		}

		docBuilder.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException exception) throws SAXException {
				//ignore
			}

			@Override
			public void error(SAXParseException exception) throws SAXException {
				//ignore
			}

			@Override
			public void fatalError(SAXParseException exception) throws SAXException {
				//ignore
			}
		});
	}

	private final XPathWrapper xpath = new XPathWrapper();
	private final String requestUrl;

	public CatCommand(String key) {
		String url = "http://thecatapi.com/api/images/get?size=small&format=xml&type=gif";
		if (key != null) {
			url += "&api_key=" + UrlEscapers.urlFormParameterEscaper().escape(key);
		}
		requestUrl = url;
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
	public String description() {
		return "Displays a random cat picture. :3";
	}

	@Override
	public String helpText(String trigger) {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		int repeats = 0;
		try (CloseableHttpClient client = createClient()) {
			while (repeats < 5) {
				String catUrl = nextCat(client);
				if (isCatThere(client, catUrl)) {
					return new ChatResponse(catUrl);
				}

				repeats++;
			}
		} catch (IOException | SAXException e) {
			logger.log(Level.SEVERE, "Problem getting cat.", e);

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
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
	 * @param client the HTTP client
	 * @return the URL to the cat picture
	 * @throws IOException if there's a network error
	 * @throws SAXException if there's a problem parsing the XML response
	 */
	private String nextCat(CloseableHttpClient client) throws IOException, SAXException {
		HttpGet request = new HttpGet(requestUrl);
		try (CloseableHttpResponse response = client.execute(request)) {
			Document document;
			try (InputStream in = response.getEntity().getContent()) {
				document = docBuilder.parse(in);
			}

			Element urlElement = xpath.element("/response/data/images/image/url", document);
			return urlElement.getTextContent();
		}
	}

	/**
	 * Checks to see if the given image exists. Some of the URLs that the API
	 * returns don't work anymore.
	 * @param client the HTTP client
	 * @param url the URL to the image
	 * @return true if the image exists, false if not
	 */
	private boolean isCatThere(CloseableHttpClient client, String url) {
		HttpHead request = new HttpHead(url);
		try (CloseableHttpResponse response = client.execute(request)) {
			return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Creates an HTTP client. This method is for unit testing.
	 * @return the HTTP client
	 */
	CloseableHttpClient createClient() {
		return HttpClients.createDefault();
	}
}
