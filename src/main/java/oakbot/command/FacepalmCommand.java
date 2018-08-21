package oakbot.command;

import static oakbot.command.Command.reply;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.SplitStrategy;
import oakbot.util.ChatBuilder;

/**
 * Displays facepalm GIFs using the Tenor API.
 * @author Michael Angstadt
 * @see "https://tenor.com/gifapi/documentation"
 */
public class FacepalmCommand implements Command {
	private static final Logger logger = Logger.getLogger(FacepalmCommand.class.getName());

	private final ObjectMapper mapper = new ObjectMapper();
	private final URI uri;

	/**
	 * @param key the tenor API key
	 */
	public FacepalmCommand(String key) {
		Objects.requireNonNull(key);

		//see: https://tenor.com/gifapi/documentation#endpoints-random
		try {
			URIBuilder builder = new URIBuilder("https://api.tenor.com/v1/random");
			builder.addParameter("key", key);
			builder.addParameter("q", "facepalm");
			builder.addParameter("media_filter", "minimal");
			builder.addParameter("safesearch", "moderate");
			builder.addParameter("limit", "1");
			uri = builder.build();
		} catch (URISyntaxException ignored) {
			//should never be thrown
			throw new RuntimeException(ignored);
		}
	}

	@Override
	public String name() {
		return "facepalm";
	}

	@Override
	public String description() {
		return "Displays a facepalm gif.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new HelpBuilder(trigger, this)
			.detail("Images from tenor.com.")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String imageUrl, response = null;
		try {
			response = get(uri);
			JsonNode node = mapper.readTree(response);
			imageUrl = node.get("results").get(0).get("media").get(0).get("tinygif").get("url").asText();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem querying Tenor API.\nURI = " + uri + "\nResponse = " + response, e);
			return reply("Sorry, an error occurred. >.>", chatCommand);
		}

		/*
		 * "All content retrieved from Tenor must be properly attributed."
		 * 
		 * https://tenor.com/gifapi/documentation#attribution
		 */
		ChatBuilder condensed = new ChatBuilder().append(imageUrl).append(" (via ").link("Tenor", "https://tenor.com").append(")");

		return new ChatResponse(imageUrl, SplitStrategy.NONE, true, condensed);
	}

	/**
	 * Makes an HTTP GET request to the given URL. This method is
	 * package-private so it can be overridden in unit tests.
	 * @param uri the URL
	 * @return the response body
	 * @throws IOException if there's a network problem
	 */
	String get(URI uri) throws IOException {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(uri);
			try (CloseableHttpResponse response = client.execute(request)) {
				return EntityUtils.toString(response.getEntity());
			}
		}
	}
}
