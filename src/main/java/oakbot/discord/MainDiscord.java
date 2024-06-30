package oakbot.discord;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Guild;
import oakbot.ai.openai.OpenAIClient;
import oakbot.ai.stabilityai.StabilityAIClient;
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
		var stabilityAIClient = new StabilityAIClient(properties.getStabilityAIKey());
		var theCatDogApiClient = new TheCatDogApiClient();
		var trigger = "!";

		//@formatter:off
		var slashCommands = List.of(
			new CatCommand(theCatDogApiClient),
			new DogCommand(theCatDogApiClient),
			new ImagineCommand(openAIClient, stabilityAIClient)
		);
		var commands = new ArrayList<>(List.of(
			new AboutCommand(),
			new ShutdownCommand()
		));
		var listeners = List.of(
			new ChatGPTListener(openAIClient, "gpt-4o", properties.getOpenAIPrompt(), 2000, properties.getOpenAIMessageHistoryCount()),
			new WaveListener(),
			new CommandListener(trigger, commands)
		);
		//@formatter:on

		//create help command
		var helpCommand = new HelpCommand(slashCommands, commands, listeners);
		commands.add(helpCommand);

		//@formatter:off
		var bot = new DiscordBot.Builder()
			.adminUsers(properties.getAdminUsers())
			.ignoredChannels(properties.getIgnoredChannels())
			.slashCommands(slashCommands)
			.listeners(listeners)
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
