package oakbot.command;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangstadt.sochat4j.util.Http;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;

/**
 * Gets abbreviation definitions from abbreviations.com.
 * @author Michael Angstadt
 * @see "https://www.abbreviations.com/abbr_api.php"
 * @see "https://www.abbreviations.com/api.php"
 */
public class AbbreviationCommand implements Command {
	private static final Logger logger = Logger.getLogger(AbbreviationCommand.class.getName());

	private final String apiUserId;
	private final String apiToken;
	private final int maxResultsToDisplay = 10;

	/**
	 * @param apiUserId the API user ID
	 * @param apiToken the API token
	 */
	public AbbreviationCommand(String apiUserId, String apiToken) {
		this.apiUserId = apiUserId;
		this.apiToken = apiToken;
	}

	@Override
	public String name() {
		return "abbr";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Retrieves abbreviation definitions from abbreviations.com.")
			.example("asap", "Displays the " + maxResultsToDisplay + " most popular definitions for \"asap\".")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String abbr = chatCommand.getContent().trim();
		if (abbr.isEmpty()) {
			return reply("Enter an abbreviation.", chatCommand);
		}

		String url = apiUrl(abbr);

		Http.Response response;
		try (Http http = HttpFactory.connect()) {
			response = http.get(url);
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem getting abbreviation from STANDS4 API.");
			return error("Sorry, an unexpected error occurred: ", e, chatCommand);
		}

		try {
			JsonNode root = response.getBodyAsJson().get("result");
			if (root == null || root.size() == 0) {
				return reply("No results found.", chatCommand);
			}

			/*
			 * Results often contain duplicate definitions under different
			 * categories--do not display duplicates.
			 */
			List<String> topUniqueResults = getTopUniqueResults(root);

			//@formatter:off
			return reply(new ChatBuilder()
				.append(String.join(" | ", topUniqueResults))
				.append(" (").link("source", websiteUrl(abbr)).append(")"),
			chatCommand);
			//@formatter:on
		} catch (JsonProcessingException | NullPointerException e) {
			logger.log(Level.SEVERE, e, () -> "JSON response was not structured as expected: " + response.getBody());
			return error("Sorry, an unexpected error occurred: ", e, chatCommand);
		}
	}

	private List<String> getTopUniqueResults(JsonNode root) {
		List<String> topUniqueResults = new ArrayList<>(maxResultsToDisplay);

		for (JsonNode result : root) {
			String definition = result.get("definition").asText();
			if (topUniqueResults.contains(definition)) {
				continue;
			}

			topUniqueResults.add(definition);
			if (topUniqueResults.size() >= maxResultsToDisplay) {
				break;
			}
		}

		return topUniqueResults;
	}

	private String apiUrl(String abbr) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("www.stands4.com")
			.setPath("/services/v2/abbr.php")
			.setParameter("uid", apiUserId)
			.setParameter("tokenid", apiToken)
			.setParameter("format", "json")
			.setParameter("term", abbr)
		.toString();
		//@formatter:on
	}

	private String websiteUrl(String abbr) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("www.abbreviations.com")
			.setPathSegments(abbr)
		.toString();
		//@formatter:on
	}

	//@formatter:off
	/*
	Example response:
	{
		"result": [
			{
				"id" : "374690",
				"term" : "ASAP",
				"definition" : "As Soon As Possible",
				"category" : "GENERALBUS",
				"categoryname" : "General Business",
				"parentcategory" : "BUSINESS",
				"parentcategoryname" : "Business",
				"score" : "3.67"
			},
			...
		]
	}
	*/
	//@formatter:on
}
