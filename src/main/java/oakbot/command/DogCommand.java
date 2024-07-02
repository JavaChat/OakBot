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
public class DogCommand implements Command {
	private static final Logger logger = LoggerFactory.getLogger(DogCommand.class);

	private final TheCatDogApiClient client;

	public DogCommand(TheCatDogApiClient client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "dog";
	}

	@Override
	public List<String> aliases() {
		return List.of("woof");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays a dog GIF. :3")
			.detail("Images from thedogapi.com.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String url;
		try {
			url = client.getRandomDogGif();
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem getting dog.");
			return error("Error getting dog: ", e, chatCommand);
		}

		//@formatter:off
		return ChatActions.create(
			new PostMessage(url).bypassFilters(true)
		);
		//@formatter:on
	}
}
