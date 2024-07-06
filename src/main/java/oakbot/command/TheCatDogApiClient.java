package oakbot.command;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;
import okhttp3.HttpUrl;
import okhttp3.Request;

/**
 * Client for interacting with The Cat API.
 * @author Michael Angstadt
 * @see "https://thecatapi.com/"
 */
public class TheCatDogApiClient {
	private final String apiKey;

	public TheCatDogApiClient() {
		this(null);
	}

	/**
	 * @param apiKey the API key (can be null)
	 */
	public TheCatDogApiClient(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Gets a random cat GIF.
	 * @return the URL to the image
	 * @throws IOException if there's a network problem
	 */
	public String getRandomCatGif() throws IOException {
		return getRandomGif("cat");
	}

	/**
	 * Gets a random dog GIF.
	 * @return the image URL
	 * @throws IOException if there's a network problem
	 */
	public String getRandomDogGif() throws IOException {
		return getRandomGif("dog");
	}

	/**
	 * Gets a random GIF
	 * @param animal the animal ("cat" or "dog")
	 * @return the image URL
	 * @throws IOException if there's a network problem
	 */
	private String getRandomGif(String animal) throws IOException {
		//@formatter:off
		var url = baseUrl(animal)
			.addPathSegments("/v1/images/search")
			.setQueryParameter("size", "small")
			.setQueryParameter("mime_types", "gif")
		.build();
		//@formatter:on

		var request = requestWithApiKey().url(url).get().build();

		var response = HttpFactory.okHttp().newCall(request).execute();
		JsonNode body;
		try (var reader = response.body().charStream()) {
			body = JsonUtils.parse(reader);
		}

		var gifUrl = body.path(0).path("url").asText();
		if (gifUrl.isEmpty()) {
			throw new IOException("Unexpected JSON structure.");
		}

		return gifUrl;
	}

	private HttpUrl.Builder baseUrl(String animal) {
		//@formatter:off
		return new HttpUrl.Builder()
			.scheme("https")
			.host("api.the" + animal + "api.com");
		//@formatter:on
	}

	private Request.Builder requestWithApiKey() {
		var builder = new Request.Builder();
		if (apiKey != null) {
			builder.addHeader("x-api-key", apiKey);
		}
		return builder;
	}
}
