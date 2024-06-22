package oakbot.discord;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Guild;
import oakbot.ai.openai.OpenAIClient;
import oakbot.command.TheCatDogApiClient;
import oakbot.util.HttpRequestLogger;

/**
 * Connects to a Discord app.
 * @author Michael Angstadt
 * @see "https://discord.com/developers"
 */
public class MainDiscord {
	public static void main(String[] args) throws Exception {
		var properties = new DiscordProperties(Paths.get(args[0]));
		var openAIClient = new OpenAIClient(properties.getOpenAIKey(), new HttpRequestLogger("openai-requests.discord.csv"));
		var theCatDogApiClient = new TheCatDogApiClient();
		var trigger = "!";

		//@formatter:off
		var commands = List.of(
			new CatCommand(theCatDogApiClient),
			new DogCommand(theCatDogApiClient),
			new ImagineCommand(openAIClient),
			new ShutdownCommand()
		);
		var listeners = List.of(
			new WaveListener(),
			new CommandListener(trigger, commands)
		);
		var mentionListeners = List.<DiscordListener> of(
			new ChatGPTListener(openAIClient, "gpt-4o", properties.getOpenAIPrompt(), 2000, properties.getOpenAIMessageHistoryCount())
		);

		var bot = new DiscordBot.Builder()
			.adminUsers(properties.getAdminUsers())
			.ignoredChannels(properties.getIgnoredChannels())
			.listeners(listeners)
			.mentionListeners(mentionListeners)
			.status(properties.getDiscordStatus())
			.token(properties.getDiscordToken())
			.trigger(trigger)
		.build();
		//@formatter:on

		Runtime.getRuntime().addShutdownHook(new Thread(bot::shutdown));

		var jda = bot.getJDA();
		System.out.println("Guilds joined: " + jda.getGuildCache().stream().map(Guild::getName).collect(Collectors.joining(", ")));

		System.out.println("Bot has launched successfully. To move this process to the background, press Ctrl+Z then type \"bg\".");
	}
}
