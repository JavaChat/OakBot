package oakbot.command.stands4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangstadt.sochat4j.util.Http;

import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;

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
		var url = baseUri("abbr.php")
			.setParameter("term", abbr)
		.toString();
		//@formatter:on

		var response = send(url);

		try {
			var results = response.get("result");
			if (results == null || results.isEmpty()) {
				return List.of();
			}

			var topUniqueResults = new ArrayList<String>(maxResults);

			for (var result : results) {
				/*
				 * Results often contain duplicate definitions under different
				 * categories--do not display duplicates.
				 */
				var definition = result.get("definition").asText();
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

	/**
	 * Checks a sentence for grammar.
	 * @param sentence the sentence
	 * @return the grammatical errors
	 * @throws IOException if there's a problem querying the API
	 */
	public List<String> checkGrammar(String sentence) throws IOException {
		//@formatter:off
		var url = baseUri("grammar.php")
			.setParameter("text", sentence)
		.toString();
		//@formatter:on

		var response = send(url);

		try {
			var matches = response.path("matches");

			//@formatter:off
			return JsonUtils.streamArray(matches)
				.map(result -> result.get("message").asText())
			.toList();
			//@formatter:on
		} catch (NullPointerException e) {
			logBadStructure(response, e);
			throw badStructure(e);
		}
	}

	/**
	 * Gets the attribution URL to use for a grammar check.
	 * @return the URL
	 */
	public String getGrammarAttributionUrl() {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("www.grammar.com")
		.toString();
		//@formatter:on
	}

	/**
	 * Performs a unit conversion.
	 * @param query the conversion query (e.g. "5 km in miles")
	 * @return the result
	 * @throws ConvertException if the given query wasn't understood
	 * @throws IOException if there's a problem querying the API
	 */
	public String convert(String query) throws ConvertException, IOException {
		//@formatter:off
		var url = baseUri("conv.php")
			.setParameter("expression", query)
		.toString();
		//@formatter:on

		var response = send(url);

		try {
			var errorCode = response.get("errorCode").asInt();
			if (errorCode != 0) {
				var errorMessage = response.get("errorMessage").asText();
				throw new ConvertException(errorCode, errorMessage);
			}

			return response.get("result").asText();
		} catch (NullPointerException e) {
			logBadStructure(response, e);
			throw badStructure(e);
		}
	}

	/**
	 * Gets the attribution URL to use conversions.
	 * @return the URL
	 */
	public String getConvertAttributionUrl() {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("www.convert.net")
		.toString();
		//@formatter:on
	}

	/**
	 * Explains a common phrase, idiom, or casual expression.
	 * @param phrase the phrase to explain
	 * @return the result or null if none were found
	 * @throws IOException if there's a problem querying the API
	 */
	public Explanation explain(String phrase) throws IOException {
		//@formatter:off
		var url = baseUri("phrases.php")
			.setParameter("phrase", phrase)
		.toString();
		//@formatter:on

		var response = send(url);

		try {
			var results = response.get("result");
			if (results == null || results.isEmpty()) {
				return null;
			}

			/*
			 * If the response only contains one result, the value of the
			 * "results" field will be an object. If there are multiple results,
			 * the field will be an array.
			 */
			var firstResult = results.isArray() ? results.get(0) : results;

			var explanation = firstResult.get("explanation").asText();

			/*
			 * The example field will be set to an empty object if there is no
			 * example.
			 */
			var example = firstResult.get("example").asText();
			if (example.isEmpty()) {
				example = null;
			}

			return new Explanation(explanation, example);
		} catch (NullPointerException e) {
			logBadStructure(response, e);
			throw badStructure(e);
		}
	}

	/**
	 * Gets the attribution URL for phrase explanation.
	 * @param phrase the phrase
	 * @return the URL
	 */
	public String getExplainAttributionUrl(String phrase) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("www.phrases.com")
			.setPathSegments("psearch", phrase)
		.toString();
		//@formatter:on
	}

	/**
	 * Gets words that rhyme with the given word.
	 * @param word the word
	 * @return the results
	 * @throws IOException if there's a problem querying the API
	 */
	public List<String> getRhymes(String word) throws IOException {
		//@formatter:off
		var url = baseUri("rhymes.php")
			.setParameter("term", word)
		.toString();
		//@formatter:on

		var response = send(url);

		try {
			var value = response.get("rhymes").asText();
			return value.isEmpty() ? List.of() : List.of(value.split(", "));
		} catch (NullPointerException e) {
			logBadStructure(response, e);
			throw badStructure(e);
		}
	}

	/**
	 * Gets the attribution URL for rhymes.
	 * @param word the word
	 * @return the URL
	 */
	public String getRhymesAttributionUrl(String word) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("www.rhymes.com")
			.setPathSegments("rhyme", word)
		.toString();
		//@formatter:on
	}

	private JsonNode send(String url) throws IOException {
		Http.Response response;
		try (var http = HttpFactory.connect()) {
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
