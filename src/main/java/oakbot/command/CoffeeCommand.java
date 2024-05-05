package oakbot.command;

import static oakbot.bot.ChatActions.error;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.HttpFactory;

/**
 * Displays a coffee-related picture.
 * @author Michael Angstadt
 * @see "https://coffee.alexflipnote.dev"
 */
public class CoffeeCommand implements Command {
	private static final Logger logger = Logger.getLogger(CoffeeCommand.class.getName());

	private final String apiUrl = "https://coffee.alexflipnote.dev/random.json";

	@Override
	public String name() {
		return "coffee";
	}

	@Override
	public List<String> aliases() {
		return List.of("KAFFEEZEIT");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays a coffee-related image.")
			.detail("Images from coffee.alexflipnote.dev.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String url;
		try {
			url = getCoffee();
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem getting coffee.");
			return error("Error getting coffee: ", e, chatCommand);
		}

		//@formatter:off
		return ChatActions.create(
			new PostMessage(url).bypassFilters(true)
		);
		//@formatter:on
	}

	private String getCoffee() throws IOException {
		try (var http = HttpFactory.connect()) {
			var node = http.get(apiUrl).getBodyAsJson().get("file");
			if (node == null) {
				throw new IOException("Unexpected JSON structure.");
			}
			return node.asText();
		}
	}
}
