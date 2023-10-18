package oakbot.command.shibe;

import java.io.IOException;

import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangstadt.sochat4j.util.Http;

import oakbot.util.HttpFactory;

/**
 * Displays random animal pictures using shibe.online.
 * @author Michael Angstadt
 * @see "https://shibe.online/"
 */
public class ShibeOnlineClient {
	public String getBird() throws IOException {
		return getAnimal("birds");
	}

	public String getCat() throws IOException {
		return getAnimal("cats");
	}

	public String getShiba() throws IOException {
		return getAnimal("shibes");
	}

	private String getAnimal(String animal) throws IOException {
		//@formatter:off
		String url = new URIBuilder()
			.setScheme("https")
			.setHost("shibe.online")
			.setPathSegments("api", animal)
		.toString();
		//@formatter:on

		Http.Response response;
		try (Http http = HttpFactory.connect()) {
			response = http.get(url);
		}

		JsonNode node = response.getBodyAsJson().get(0);
		if (node == null) {
			throw new IOException("JSON response not structured as expected.");
		}

		return node.asText();
	}
}
