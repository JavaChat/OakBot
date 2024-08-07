package oakbot.discord;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import oakbot.command.TheCatDogApiClient;

/**
 * Displays a random dog picture.
 * @author Michael Angstadt
 * @see "https://thecatapi.com/"
 */
public class DogCommand implements DiscordSlashCommand {
	private static final Logger logger = LoggerFactory.getLogger(DogCommand.class);

	private final TheCatDogApiClient client;

	public DogCommand(TheCatDogApiClient client) {
		this.client = client;
	}

	@Override
	public SlashCommandData data() {
		var name = "dog";
		var description = "Displays a dog GIF 🐶. Images from thedogapi.com.";

		return Commands.slash(name, description);
	}

	@Override
	public void onMessage(SlashCommandInteractionEvent event, BotContext context) {
		String url;
		try {
			url = client.getRandomDogGif();
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem getting dog.");
			event.getChannel().sendMessage("Error getting dog: `" + e.getMessage() + "`").queue();
			return;
		}

		event.reply(url).queue();
	}
}
