package oakbot.command.shibe;

import static oakbot.bot.ChatActions.error;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;

/**
 * Displays a shiba dog picture.
 * @author Michael Angstadt
 * @see "https://shibe.online/"
 */
public class ShibaCommand implements Command {
	private static final Logger logger = LoggerFactory.getLogger(ShibaCommand.class);

	private final ShibeOnlineClient client;

	public ShibaCommand(ShibeOnlineClient client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "shiba";
	}

	@Override
	public List<String> aliases() {
		return List.of("woof");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays an image of a shiba inu dog.")
			.detail("Images from shibe.online.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String url;
		try {
			url = client.getShiba();
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem getting shiba.");
			return error("Error getting shiba: ", e, chatCommand);
		}

		//@formatter:off
		return ChatActions.create(
			new PostMessage(url).bypassFilters(true)
		);
		//@formatter:on
	}
}
