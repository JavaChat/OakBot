package oakbot.discord;

import static oakbot.util.StringUtils.plural;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import oakbot.ai.openai.ChatCompletionRequest;
import oakbot.ai.openai.OpenAIClient;
import oakbot.listener.chatgpt.UsageQuota;

/**
 * @author Michael Angstadt
 */
public class ChatGPTListener implements DiscordListener {
	private final OpenAIClient client;
	private final String model;
	private final String prompt;
	private final int maxTokens;
	private final int messageHistoryCount;
	private final UsageQuota usageQuota = new UsageQuota(Duration.ofDays(1), 10);

	public ChatGPTListener(OpenAIClient client, String model, String prompt, int maxTokens, int messageHistoryCount) {
		this.client = client;
		this.model = model;
		this.prompt = prompt;
		this.maxTokens = maxTokens;
		this.messageHistoryCount = messageHistoryCount;
	}

	@Override
	public void onMessage(MessageReceivedEvent event, BotContext context) {
		var userId = event.getMessage().getAuthor().getIdLong();

		Duration timeUntilNextRequest;
		synchronized (usageQuota) {
			timeUntilNextRequest = usageQuota.getTimeUntilUserCanMakeRequest(userId);
		}

		if (!timeUntilNextRequest.isZero()) {
			var hours = timeUntilNextRequest.toHours() + 1;
			event.getMessage().reply("Bad human! You are over quota. Try again in " + hours + " " + plural("hour", hours) + ".").queue();
			return;
		}

		var history = event.getChannel().getHistoryBefore(event.getMessage(), messageHistoryCount - 1).complete();

		var openAIMessages = new ArrayList<ChatCompletionRequest.Message>();

		openAIMessages.add(toChatCompletionMessage(event.getMessage()));

		//@formatter:off
		history.getRetrievedHistory().stream()
			.map(ChatGPTListener::toChatCompletionMessage)
		.forEach(openAIMessages::add);

		openAIMessages.add(new ChatCompletionRequest.Message.Builder()
			.role("system")
			.text(prompt)
		.build());
		//@formatter:on

		Collections.reverse(openAIMessages);

		//@formatter:off
		var chatCompletionRequest = new ChatCompletionRequest.Builder()
			.model(model)
			.maxTokens(maxTokens)
			.messages(openAIMessages)
		.build();
		//@formatter:on

		try {
			var apiResponse = client.chatCompletion(chatCompletionRequest);
			var reply = apiResponse.getChoices().get(0).getContent();
			var action = event.getMessage().reply(reply);

			if (context.authorIsAdmin()) {
				action.queue();
			} else {
				action.queue(m -> {
					synchronized (usageQuota) {
						usageQuota.logRequest(userId);
					}
				});
			}
		} catch (Exception e) {
			event.getMessage().reply("ERROR BEEP BOOP: " + e.getMessage()).queue();
		}
	}

	private static ChatCompletionRequest.Message toChatCompletionMessage(Message message) {
		var role = message.getAuthor().equals(message.getJDA().getSelfUser()) ? "assistant" : "user";
		var name = message.getAuthor().getEffectiveName();
		var text = message.getContentDisplay();
		return new ChatCompletionRequest.Message.Builder().name(name).role(role).text(text).build();
	}
}
