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
 * Checks a sentence for grammar using grammar.com.
 * @author Michael Angstadt
 * @see "https://www.grammar.com/grammar_api.php"
 */
public class GrammarCommand implements Command {
	private final Stands4Client client;

	/**
	 * @param client the STANDS4 API client
	 */
	public GrammarCommand(Stands4Client client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "grammar";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Checks a sentence for grammar using grammar.com")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var sentence = chatCommand.getContent().trim();
		if (sentence.isEmpty()) {
			return reply("Specify the sentence to check.", chatCommand);
		}

		List<String> results;
		try {
			results = client.checkGrammar(sentence);
		} catch (IOException e) {
			return error("Sorry, an unexpected error occurred: ", e, chatCommand);
		}

		var url = client.getGrammarAttributionUrl();

		if (results.isEmpty()) {
			//@formatter:off
			return reply(new ChatBuilder()
				.append("No errors found.")
				.append(" (checked by ").append(url).append(")"), chatCommand
			);
			//@formatter:on
		}

		//@formatter:off
		return reply(new ChatBuilder()
			.append(String.join("\n", results))
			.nl().append("checked by: ").append(url), chatCommand
		);
		//@formatter:on
	}
}
