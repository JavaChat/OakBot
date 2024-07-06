package oakbot.discord;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;
import okhttp3.Request;

/**
 * Displays a coffee-related picture.
 * @author Michael Angstadt
 * @see "https://coffee.alexflipnote.dev"
 */
public class CoffeeCommand implements DiscordSlashCommand {
	private static final Logger logger = LoggerFactory.getLogger(CoffeeCommand.class);

	private final Request request = new Request.Builder().url("https://coffee.alexflipnote.dev/random.json").get().build();

	@Override
	public SlashCommandData data() {
		//@formatter:off
		var description = new ChatBuilder()
			.append("Displays a random coffee photo ☕. Images from https://coffee.alexflipnote.dev.")
		.toString();
		//@formatter:on

		return Commands.slash("coffee", description);
	}

	@Override
	public void onMessage(SlashCommandInteractionEvent event, BotContext context) {
		String url;
		try {
			url = getCoffee();
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem getting coffee.");
			event.reply("Error getting coffee: " + e.getMessage()).queue();
			return;
		}

		event.reply(url).queue();
	}

	private String getCoffee() throws IOException {
		var response = HttpFactory.okHttp().newCall(request).execute();

		JsonNode body;
		try (var reader = response.body().charStream()) {
			body = JsonUtils.parse(reader);
		}

		var node = body.get("file");
		if (node == null) {
			throw new IOException("Unexpected JSON structure.");
		}
		return node.asText();
	}
}
