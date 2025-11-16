package oakbot.command.stands4;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.List;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Gets abbreviation definitions from abbreviations.com.
 * @author Michael Angstadt
 * @see "https://www.abbreviations.com/abbr_api.php"
 */
public class AbbreviationCommand implements Command {
	private final Stands4Client client;
	private final int maxResultsToDisplay = 10;

	/**
	 * @param client the STANDS4 API client
	 */
	public AbbreviationCommand(Stands4Client client) {
		this.client = client;
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
		var abbr = chatCommand.getContent().trim();
		if (abbr.isEmpty()) {
			return reply("Enter an abbreviation.", chatCommand);
		}

		List<String> results;
		try {
			results = client.getAbbreviations(abbr, maxResultsToDisplay);
		} catch (IOException e) {
			return error("Sorry, an unexpected error occurred: ", e, chatCommand);
		}

		var url = client.getAbbreviationsAttributionUrl(abbr);

		//@formatter:off
		return reply(new ChatBuilder()
			.append(results.isEmpty() ? "No results found." : String.join(" | ", results))
			.append(" (").link("source", url).append(")"), chatCommand
		);
		//@formatter:on
	}
}
