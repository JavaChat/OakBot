package oakbot.command.stands4;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Performs unit conversions.
 * @author Michael Angstadt
 * @see "https://www.convert.net/conv_api.php"
 */
public class ConvertCommand implements Command {
	private final Stands4Client client;

	/**
	 * @param client the STANDS4 API client
	 */
	public ConvertCommand(Stands4Client client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "convert";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Performs a unit conversion.")
			.example("5 km in miles", "Displays how many miles are in 5 kilometers.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var query = chatCommand.getContent().trim();
		if (query.isEmpty()) {
			return reply("Specify what you want to convert.", chatCommand);
		}

		String result;
		try {
			result = client.convert(query);
		} catch (ConvertException e) {
			return error("What kind of garbage did you enter? ", e, chatCommand);
		} catch (IOException e) {
			return error("Sorry, an unexpected error occurred: ", e, chatCommand);
		}

		var url = client.getConvertAttributionUrl();

		//@formatter:off
		return post(new ChatBuilder()
			.reply(chatCommand)
			.append(result)
			.append(" (").link("source", url).append(")") 
		);
		//@formatter:on
	}
}
