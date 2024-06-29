package oakbot.discord;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import oakbot.command.TheCatDogApiClient;
import oakbot.util.ChatBuilder;

/**
 * Displays a random cat picture.
 * @author Michael Angstadt
 * @see "https://thecatapi.com/"
 */
public class CatCommand implements DiscordSlashCommand {
	private static final Logger logger = Logger.getLogger(CatCommand.class.getName());

	private final TheCatDogApiClient client;

	public CatCommand(TheCatDogApiClient client) {
		this.client = client;
	}

	@Override
	public SlashCommandData data() {
		//@formatter:off
		var description = new ChatBuilder()
			.append("Displays a cat GIF 🐱. Images from thecatapi.com.")
		.toString();
		//@formatter:on

		return Commands.slash("cat", description);
	}

	@Override
	public void onMessage(SlashCommandInteractionEvent event, BotContext context) {
		String url;
		try {
			url = client.getRandomCatGif();
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem getting cat.");
			event.getChannel().sendMessage("Error getting cat: `" + e.getMessage() + "`").queue();
			return;
		}

		event.reply(url).queue();
	}
}
