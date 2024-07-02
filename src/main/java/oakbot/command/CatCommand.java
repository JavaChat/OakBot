package oakbot.command;

import static oakbot.bot.ChatActions.error;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;

/**
 * Displays a random cat picture.
 * @author Michael Angstadt
 * @see "https://thecatapi.com/"
 */
public class CatCommand implements Command {
	private static final Logger logger = LoggerFactory.getLogger(CatCommand.class);

	private final TheCatDogApiClient client;

	public CatCommand(TheCatDogApiClient client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "cat";
	}

	@Override
	public List<String> aliases() {
		return List.of("meow");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays a cat GIF. :3")
			.detail("Images from thecatapi.com.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String url;
		try {
			url = client.getRandomCatGif();
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem getting cat.");
			return error("Error getting cat: ", e, chatCommand);
		}

		//@formatter:off
		return ChatActions.create(
			new PostMessage(url).bypassFilters(true)
		);
		//@formatter:on
	}
}
