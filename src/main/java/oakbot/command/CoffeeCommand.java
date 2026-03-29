package oakbot.command;

import static oakbot.bot.ChatActions.error;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.util.Http;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;

/**
 * Displays a coffee-related picture.
 * @author Michael Angstadt
 * @see "https://coffee.alexflipnote.dev"
 */
public class CoffeeCommand implements Command {
	private static final Logger logger = LoggerFactory.getLogger(CoffeeCommand.class);
	private static final String API_URL = "https://coffee.alexflipnote.dev/random.json";

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
			logger.atError().setCause(e).log(() -> "Problem getting coffee.");
			return error("Error getting coffee: ", e, chatCommand);
		}

		//@formatter:off
		return ChatActions.create(
			new PostMessage(url).bypassFilters(true)
		);
		//@formatter:on
	}

	private String getCoffee() throws IOException {
		Http.Response response;
		try (var http = HttpFactory.connect()) {
			response = http.get(API_URL);
		}

		var node = response.getBodyAsJson();

		//@formatter:off
		return JsonUtils.extractField(node, "file")
		.orElseThrow(() -> new IOException("Unexpected JSON structure."));
		//@formatter:on
	}
}
