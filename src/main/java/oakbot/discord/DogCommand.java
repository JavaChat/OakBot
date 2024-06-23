package oakbot.discord;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import oakbot.command.HelpDoc;
import oakbot.command.TheCatDogApiClient;
import oakbot.util.ChatBuilder;

/**
 * Displays a random dog picture.
 * @author Michael Angstadt
 * @see "https://thecatapi.com/"
 */
public class DogCommand implements DiscordCommand {
	private static final Logger logger = Logger.getLogger(DogCommand.class.getName());

	private final TheCatDogApiClient client;

	public DogCommand(TheCatDogApiClient client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "dog";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new DiscordHelpDoc.Builder(this)
			.summary("Displays a dog GIF. 🐶")
			.detail(new ChatBuilder().append("Images from ").link("thedogapi.com", "https://thedogapi.com").append(".").toString())
		.build();
		//@formatter:on
	}

	@Override
	public void onMessage(String content, MessageReceivedEvent event, BotContext context) {
		String url;
		try {
			url = client.getRandomDogGif();
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem getting dog.");
			event.getChannel().sendMessage("Error getting dog: `" + e.getMessage() + "`").queue();
			return;
		}

		event.getChannel().sendMessage(url).queue();
	}
}
