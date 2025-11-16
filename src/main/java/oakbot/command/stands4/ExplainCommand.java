package oakbot.command.stands4;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Explains a common idiom using phrases.com.
 * @author Michael Angstadt
 * @see "https://www.phrases.com/phrases_api.php"
 */
public class ExplainCommand implements Command {
	private final Stands4Client client;

	/**
	 * @param client the STANDS4 API client
	 */
	public ExplainCommand(Stands4Client client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "explain";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Explains a common idiom using phrases.com.")
			.example("eat my shorts", "Displays an explanation of the given phrase.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var phrase = chatCommand.getContent().trim();
		if (phrase.isEmpty()) {
			return reply("Enter a phrase.", chatCommand);
		}

		Explanation result;
		try {
			result = client.explain(phrase);
		} catch (IOException e) {
			return error("Sorry, an unexpected error occurred: ", e, chatCommand);
		}

		var cb = new ChatBuilder();
		if (result == null) {
			cb.append("No explanation found.");
		} else {
			cb.append(result.getExplanation());

			var example = result.getExample();
			if (example != null) {
				cb.append(" ").italic(result.getExample());
			}
		}

		var url = client.getExplainAttributionUrl(phrase);
		cb.append(" (").link("source", url).append(")");

		return reply(cb, chatCommand);
	}
}
