package oakbot.command;

import java.io.IOException;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangstadt.sochat4j.util.Http;

import oakbot.util.HttpFactory;

/**
 * Interfaces with the GIPHY API.
 * @author Michael Angstadt
 * @see "https://developers.giphy.com"
 */
public class GiphyClient {
	private static final Logger logger = LoggerFactory.getLogger(GiphyClient.class);

	private final String apiKey;

	/**
	 * @param apiKey the API key
	 */
	public GiphyClient(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Retrieves a random GIF.
	 * @param tag filters result by tag, can be null
	 * @param rating filters result by content rating, can be null
	 * @return the URL of the image or null if no images were found
	 * @throws IOException if an unexpected response was returned or if there
	 * was a network problem
	 * @see "https://developers.giphy.com/docs/api/endpoint#random"
	 */
	public String random(String tag, Rating rating) throws IOException {
		var url = baseUrl("random");
		if (tag != null) {
			url.addParameter("tag", tag);
		}
		if (rating != null) {
			url.addParameter("rating", rating.value);
		}

		var response = send(url.toString());

		var data = response.path("data");
		if (data.isArray() && data.size() == 0) {
			return null;
		}

		var node = data.path("images").path("original").path("url");
		if (node.isMissingNode()) {
			throw new IOException("Unexpected JSON structure in response.");
		}

		return node.asText();
	}

	private JsonNode send(String url) throws IOException {
		Http.Response response;
		try (var http = HttpFactory.connect()) {
			response = http.get(url);
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem sending Giphy API request: " + url);
			throw e;
		}

		try {
			return response.getBodyAsJson();
		} catch (JsonProcessingException e) {
			logger.atError().setCause(e).log(() -> "Response could not be parsed as JSON: " + response.getBody());
			throw e;
		}
	}

	private URIBuilder baseUrl(String endpoint) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("api.giphy.com")
			.setPathSegments("v1", "gifs", endpoint)
			.addParameter("api_key", apiKey);
		//@formatter:on
	}

	public enum Rating {
		G("g"), PG("pg"), PG_13("pg-13"), R("r");

		private final String value;

		private Rating(String value) {
			this.value = value;
		}
	}
}
