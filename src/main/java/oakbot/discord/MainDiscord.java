package oakbot.discord;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
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
	private static OpenAIClient openAIClient;
	private static DiscordProperties properties;

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

		Runtime.getRuntime().addShutdownHook(new Thread(jda::shutdown));

		System.out.println("Guilds joined: " + jda.getGuildCache().stream().map(Guild::getName).collect(Collectors.joining(",")));

		System.out.println("Bot has launched successfully. To move this process to the background, press Ctrl+Z then type \"bg\".");
	}

	private static void onMessageReceived(MessageReceivedEvent event) {
		var botUser = event.getJDA().getSelfUser();

		var author = event.getAuthor();
		var messagePostedByBot = author.equals(botUser);
		if (messagePostedByBot) {
			return;
		}

		var inTextChannel = (event.getChannelType() == ChannelType.TEXT);
		if (!inTextChannel) {
			return;
		}

		var botMentioned = event.getMessage().getMentions().getUsers().contains(botUser);
		if (botMentioned) {
			handleMention(event);
			return;
		}

		var message = event.getMessage().getContentDisplay();
		if ("o/".equals(message)) {
			event.getMessage().addReaction(Emoji.fromUnicode("🙋‍♀️"));
			event.getChannel().sendMessage("\\o").queue();
		} else if ("\\o".equals(message)) {
			event.getMessage().addReaction(Emoji.fromUnicode("🙋‍♀️"));
			event.getChannel().sendMessage("o/").queue();
		}

		if ("Oak, shutdown".equals(message)) {
			event.getChannel().sendMessage("Shutting down...").queue();
			event.getJDA().shutdown();
		}
	}

	private static void handleMention(MessageReceivedEvent event) {
		var botUser = event.getJDA().getSelfUser();
		var history = event.getChannel().getHistoryBefore(event.getMessage(), properties.getOpenAIMessageHistoryCount() - 1).complete();

		var openAIMessages = new ArrayList<ChatCompletionRequest.Message>();
		openAIMessages.add(toApiMessage(event.getMessage(), botUser));
		history.getRetrievedHistory().stream().map(m -> toApiMessage(m, botUser)).forEach(openAIMessages::add);
		openAIMessages.add(new ChatCompletionRequest.Message.Builder().role("system").text(properties.getOpenAIPrompt()).build());
		Collections.reverse(openAIMessages);

		var apiRequest = new ChatCompletionRequest.Builder().model("gpt-4o").maxTokens(2000).messages(openAIMessages).build();

		try {
			var apiResponse = openAIClient.chatCompletion(apiRequest);
			var reply = apiResponse.getChoices().get(0).getContent();
			event.getMessage().reply(reply).queue();
		} catch (Exception e) {
			event.getMessage().reply("ERROR BEEP BOOP: " + e.getMessage()).queue();
		}
	}

	private static ChatCompletionRequest.Message toApiMessage(Message message, User botUser) {
		var role = message.getAuthor().equals(botUser) ? "assistant" : "user";
		var name = message.getAuthor().getEffectiveName();
		var text = message.getContentDisplay();
		return new ChatCompletionRequest.Message.Builder().name(name).role(role).text(text).build();
	}
}
