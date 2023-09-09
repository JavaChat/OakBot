package oakbot.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangstadt.sochat4j.util.Http;
import com.github.mangstadt.sochat4j.util.JsonUtils;

import oakbot.util.HttpFactory;

/**
 * Interfaces with the STANDS4 API.
 * @author Michael Angstadt
 * @see "https://www.abbreviations.com/api.php"
 */
public class Stands4Client {
	private static final Logger logger = Logger.getLogger(Stands4Client.class.getName());

	private final String apiUserId;
	private final String apiToken;

	/**
	 * @param apiUserId the API user ID
	 * @param apiToken the API token
	 */
	public Stands4Client(String apiUserId, String apiToken) {
		this.apiUserId = apiUserId;
		this.apiToken = apiToken;
	}

	/**
	 * Gets abbreviation definitions
	 * @param abbr the abbreviation
	 * @param maxResults how many results to return
	 * @return the results
	 * @throws IOException if there's a problem querying the API
	 */
	public List<String> getAbbreviations(String abbr, int maxResults) throws IOException {
		//@formatter:off
		String url = baseUri("abbr.php")
			.setParameter("term", abbr)
		.toString();
		//@formatter:on

		JsonNode response = send(url);

		try {
			JsonNode results = response.get("result");
			if (results == null || results.size() == 0) {
				return List.of();
			}

			List<String> topUniqueResults = new ArrayList<>(maxResults);

			for (JsonNode result : results) {
				/*
				 * Results often contain duplicate definitions under different
				 * categories--do not display duplicates.
				 */
				String definition = result.get("definition").asText();
				if (topUniqueResults.contains(definition)) {
					continue;
				}

				topUniqueResults.add(definition);
				if (topUniqueResults.size() >= maxResults) {
					break;
				}
			}

			return topUniqueResults;
		} catch (NullPointerException e) {
			logBadStructure(response, e);
			throw badStructure(e);
		}
	}

	/**
	 * Gets the attribution URL to use for an abbreviation.
	 * @param abbr the abbreviation
	 * @return the URL
	 */
	public String getAbbreviationsAttributionUrl(String abbr) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("www.abbreviations.com")
			.setPathSegments(abbr)
		.toString();
		//@formatter:on
	}

	private JsonNode send(String url) throws IOException {
		Http.Response response;
		try (Http http = HttpFactory.connect()) {
			response = http.get(url);
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem sending STANDS4 API request: " + url);
			throw e;
		}

		try {
			return response.getBodyAsJson();
		} catch (JsonProcessingException e) {
			logger.log(Level.SEVERE, e, () -> "Response could not be parsed as JSON: " + response.getBody());
			throw e;
		}
	}

	private void logBadStructure(JsonNode response, NullPointerException e) {
		logger.log(Level.SEVERE, e, () -> "JSON response was not structured as expected: " + JsonUtils.prettyPrint(response));
	}

	private IOException badStructure(NullPointerException e) {
		return new IOException("JSON response was not structured as expected.", e);
	}

	private URIBuilder baseUri(String file) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("www.stands4.com")
			.setPathSegments("services", "v2", file)
			.setParameter("uid", apiUserId)
			.setParameter("tokenid", apiToken)
			.setParameter("format", "json");
		//@formatter:on
	}
}
