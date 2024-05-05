package oakbot.command.stands4;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.post;
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
 * Gets words that rhyme from rhymes.com.
 * @author Michael Angstadt
 * @see "https://www.rhymes.com/rhymes_api.php"
 */
public class RhymeCommand implements Command {
	private final Stands4Client client;

	/**
	 * @param client the STANDS4 API client
	 */
	public RhymeCommand(Stands4Client client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "rhyme";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Finds words that rhyme with the given word.")
			.detail("Results are from rhymes.com.")
			.example("code", "Displays words that rhyme with \"code\".")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var word = chatCommand.getContent().trim();
		if (word.isEmpty()) {
			return reply("Enter a word.", chatCommand);
		}

		List<String> results;
		try {
			results = client.getRhymes(word);
		} catch (IOException e) {
			return error("Sorry, an unexpected error occurred: ", e, chatCommand);
		}

		var url = client.getRhymesAttributionUrl(word);

		//@formatter:off
		return post(new ChatBuilder()
			.reply(chatCommand)
			.append(results.isEmpty() ? "No results found." : String.join(", ", results))
			.append(" (").link("source", url).append(")")
		);
		//@formatter:on
	}
}
