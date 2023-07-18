package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatBuilder;
import oakbot.util.Http;
import oakbot.util.HttpFactory;

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
			//@formatter:off
			uri = new URIBuilder("https://api.tenor.com/v1/random")
				.addParameter("key", key)
				.addParameter("q", "facepalm")
				.addParameter("media_filter", "minimal")
				.addParameter("safesearch", "moderate")
				.addParameter("limit", "1")
			.build();
			//@formatter:on
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
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays a facepalm gif.")
			.detail("Images from tenor.com.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String imageUrl, response = null;
		try (Http http = HttpFactory.connect()) {
			response = http.get(uri.toString()).getBody();
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

		//@formatter:off
		return ChatActions.create(
			new PostMessage(imageUrl).bypassFilters(true).condensedMessage(condensed)
		);
		//@formatter:on
	}
}
