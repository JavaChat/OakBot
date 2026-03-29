package oakbot.command;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.Objects;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangstadt.sochat4j.util.Http;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;

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
		Http.Response response;
		try (var http = HttpFactory.connect()) {
			response = http.get(uri);
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem querying API.\nURI = " + uri);
			return error("Problem querying API: ", e, chatCommand);
		}

		JsonNode node;
		try {
			node = response.getBodyAsJson();
		} catch (JsonProcessingException e) {
			logger.atError().setCause(e).log(() -> "Cannot parse response as JSON.\nURI = " + uri + "\nResponse = " + response.getBody());
			return reply("API response not valid JSON.", chatCommand);
		}

		var imageUrlOpt = JsonUtils.extractField(node, "results", 0, "media", 0, "tinygif", "url");
		if (imageUrlOpt.isEmpty()) {
			logger.atError().log(() -> "Unexpected JSON structure.\nURI = " + uri + "\nResponse = " + response.getBody());
			return reply("Unexpected JSON structure in response.", chatCommand);
		}

		var imageUrl = imageUrlOpt.get();

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
