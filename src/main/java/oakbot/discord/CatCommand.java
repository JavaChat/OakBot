package oakbot.discord;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import oakbot.command.HelpDoc;
import oakbot.command.TheCatDogApiClient;
import oakbot.util.ChatBuilder;

/**
 * Displays a random cat picture.
 * @author Michael Angstadt
 * @see "https://thecatapi.com/"
 */
public class CatCommand implements DiscordCommand {
	private static final Logger logger = Logger.getLogger(CatCommand.class.getName());

	private final TheCatDogApiClient client;

	public CatCommand(TheCatDogApiClient client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "cat";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new DiscordHelpDoc.Builder(this)
			.summary("Displays a cat GIF. 🐱")
			.detail(new ChatBuilder().append("Images from ").link("thecatapi.com", "https://thecatapi.com").append(".").toString())
		.build();
		//@formatter:on
	}

	@Override
	public void onMessage(String content, MessageReceivedEvent event, BotContext context) {
		String url;
		try {
			url = client.getRandomCatGif();
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem getting cat.");
			event.getChannel().sendMessage("Error getting cat: `" + e.getMessage() + "`").queue();
			return;
		}

		event.getChannel().sendMessage(url).queue();
	}
}
