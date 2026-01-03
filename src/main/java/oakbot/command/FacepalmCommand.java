package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import java.util.Objects;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.util.Http;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;

/**
 * Displays facepalm GIFs using the Tenor API.
 * @author Michael Angstadt
 * @see "https://tenor.com/gifapi/documentation"
 */
public class FacepalmCommand implements Command {
	private static final Logger logger = LoggerFactory.getLogger(FacepalmCommand.class);

	private final String uri;

	/**
	 * @param key the tenor API key
	 */
	public FacepalmCommand(String key) {
		Objects.requireNonNull(key);

		//see: https://tenor.com/gifapi/documentation#endpoints-random
		//@formatter:off
		uri = new URIBuilder()
			.setScheme("https")
			.setHost("api.tenor.com")
			.setPath("/v1/random")
			.setParameter("key", key)
			.setParameter("q", "facepalm")
			.setParameter("media_filter", "minimal")
			.setParameter("safesearch", "moderate")
			.setParameter("limit", "1")
		.toString();
		//@formatter:on
	}

	@Override
	public String name() {
		return "facepalm";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays a facepalm GIF.")
			.detail("Images from tenor.com.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String imageUrl;
		Http.Response response = null;
		try (var http = HttpFactory.connect()) {
			response = http.get(uri);
			var node = response.getBodyAsJson();
			imageUrl = node.get("results").get(0).get("media").get(0).get("tinygif").get("url").asText();
		} catch (Exception e) {
			var body = (response == null) ? null : response.getBody();
			logger.atError().setCause(e).log(() -> "Problem querying Tenor API.\nURI = " + uri + "\nResponse = " + body);
			return reply("Sorry, an error occurred. >.>", chatCommand);
		}

		/*
		 * "All content retrieved from Tenor must be properly attributed."
		 * 
		 * https://tenor.com/gifapi/documentation#attribution
		 */
		var condensed = new ChatBuilder().append(imageUrl).append(" (via ").link("Tenor", "https://tenor.com").append(")");

		//@formatter:off
		return ChatActions.create(
			new PostMessage(imageUrl).bypassFilters(true).condensedMessage(condensed)
		);
		//@formatter:on
	}
}
