package oakbot.discord;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import oakbot.ai.openai.ChatCompletionRequest;
import oakbot.ai.openai.OpenAIClient;
import oakbot.util.HttpRequestLogger;

/**
 * Connects to a Discord app.
 * @author Michael Angstadt
 * @see "https://discord.com/developers"
 */
public class MainDiscord {
	private static final String TRIGGER = "!";
	private static OpenAIClient openAIClient;
	private static DiscordProperties properties;
	private static SelfUser selfUser;
	//278175712077414411

	public static void main(String[] args) throws Exception {
		properties = new DiscordProperties(Paths.get(args[0]));

		openAIClient = new OpenAIClient(properties.getOpenAIKey(), new HttpRequestLogger("openai-requests.discord.csv"));

		var intents = EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT);

		var jda = JDABuilder.createLight(properties.getDiscordToken(), intents).addEventListeners(new ListenerAdapter() {
			@Override
			public void onMessageReceived(MessageReceivedEvent event) {
				MainDiscord.onMessageReceived(event);
			}
		}).setActivity(Activity.customStatus(properties.getDiscordStatus())).build();

		jda.getRestPing().queue(ping -> System.out.println("Logged in with ping: " + ping));
		jda.awaitReady();

		selfUser = jda.getSelfUser();

		Runtime.getRuntime().addShutdownHook(new Thread(jda::shutdown));

		System.out.println("Guilds joined: " + jda.getGuildCache().stream().map(Guild::getName).collect(Collectors.joining(",")));

		System.out.println("Bot has launched successfully. To move this process to the background, press Ctrl+Z then type \"bg\".");
	}

	private static void onMessageReceived(MessageReceivedEvent event) {
		var author = event.getAuthor();
		var authorIsAdmin = properties.getAdminUsers().contains(author.getIdLong());

		var messagePostedByBot = author.equals(selfUser);
		if (messagePostedByBot) {
			return;
		}

		var inTextChannel = (event.getChannelType() == ChannelType.TEXT);
		if (!inTextChannel) {
			return;
		}

		var channelId = event.getChannel().getIdLong();
		var inIgnoredChannel = properties.getIgnoredChannels().contains(channelId);
		if (inIgnoredChannel) {
			return;
		}

		/*
		 * Note: If a message only contains 1 word after the mention, it is not
		 * considered a mention for some reason
		 */
		var botMentioned = event.getMessage().getMentions().getUsers().contains(selfUser);
		if (botMentioned) {
			handleMention(event);
			return;
		}

		var message = event.getMessage().getContentDisplay();
		if ("o/".equals(message) || "\\o".equals(message)) {
			event.getMessage().addReaction(Emoji.fromUnicode("U+1F44B")).queue();
			return;
		}

		var command = Command.parse(message);
		if (command.isPresent()) {
			switch (command.get().name) {
			case "shutdown":
				if (authorIsAdmin) {
					event.getChannel().sendMessage("Shutting down...").queue();
					event.getJDA().shutdown();
				} else {
					event.getChannel().sendMessage("Only admins can shut me down.").queue();
				}
				break;
			}
		}
	}

	private static void handleMention(MessageReceivedEvent event) {
		var history = event.getChannel().getHistoryBefore(event.getMessage(), properties.getOpenAIMessageHistoryCount() - 1).complete();

		var openAIMessages = new ArrayList<ChatCompletionRequest.Message>();

		openAIMessages.add(toChatCompletionMessage(event.getMessage()));

		//@formatter:off
		history.getRetrievedHistory().stream()
			.map(MainDiscord::toChatCompletionMessage)
		.forEach(openAIMessages::add);

		openAIMessages.add(new ChatCompletionRequest.Message.Builder()
			.role("system")
			.text(properties.getOpenAIPrompt())
		.build());
		//@formatter:on

		Collections.reverse(openAIMessages);

		//@formatter:off
		var chatCompletionRequest = new ChatCompletionRequest.Builder()
			.model("gpt-4o")
			.maxTokens(2000)
			.messages(openAIMessages)
		.build();
		//@formatter:on

		try {
			var apiResponse = openAIClient.chatCompletion(chatCompletionRequest);
			var reply = apiResponse.getChoices().get(0).getContent();
			event.getMessage().reply(reply).queue();
		} catch (Exception e) {
			event.getMessage().reply("ERROR BEEP BOOP: " + e.getMessage()).queue();
		}
	}

	private static ChatCompletionRequest.Message toChatCompletionMessage(Message message) {
		var role = message.getAuthor().equals(selfUser) ? "assistant" : "user";
		var name = message.getAuthor().getEffectiveName();
		var text = message.getContentDisplay();
		return new ChatCompletionRequest.Message.Builder().name(name).role(role).text(text).build();
	}

	private static record Command(String name, String content) {
		public static Optional<Command> parse(String message) {
			if (!message.startsWith(TRIGGER)) {
				return Optional.empty();
			}

			String name;
			String content;
			var afterTrigger = message.substring(TRIGGER.length()).trim();
			var space = afterTrigger.indexOf(' ');
			if (space < 0) {
				name = afterTrigger;
				content = "";
			} else {
				name = afterTrigger.substring(0, space).toLowerCase();
				content = afterTrigger.substring(space + 1);
			}

			return Optional.of(new Command(name, content));
		}
	}
}
