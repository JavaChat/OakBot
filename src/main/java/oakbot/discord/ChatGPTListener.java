package oakbot.discord;

import static oakbot.util.StringUtils.plural;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import oakbot.ai.openai.OpenAIClient;
import oakbot.ai.openai.ResponsesApiRequest;
import oakbot.command.HelpDoc;
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
	public String name() {
		return "chatgpt";
	}

	@Override
	public boolean onlyRespondWhenMentioned() {
		return true;
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new DiscordHelpDoc.Builder(this)
			.summary("Allows the user to have a conversation with ChatGPT by mentioning the bot.")
			.detail("The last 10 messages in the chat room are sent to the ChatGPT servers for context.")
		.build();
		//@formatter:on
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

		var openAIMessages = new ArrayList<ResponsesApiRequest.Input>();

		openAIMessages.add(toResponsesApiInput(event.getMessage()));

		//@formatter:off
		history.getRetrievedHistory().stream()
			.map(ChatGPTListener::toResponsesApiInput)
		.forEach(openAIMessages::add);
		//@formatter:on

		Collections.reverse(openAIMessages);

		//@formatter:off
		var responsesApiRequest = new ResponsesApiRequest.Builder()
			.model(model)
			.instructions(prompt)
			.inputs(openAIMessages)
			.maxOutputTokens(maxTokens)
			.reasoningEffort("low")
			.verbosity("low")
		.build();
		//@formatter:on

		try {
			var apiResponse = client.responsesApi(responsesApiRequest);
			var completedOutput = apiResponse.getOutput().stream().filter(o -> "completed".equals(o.status())).findFirst();
			var reply = completedOutput.orElseThrow(() -> new NoSuchElementException("No completed response returned.")).content();
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

	private static ResponsesApiRequest.Input toResponsesApiInput(Message message) {
		var role = message.getAuthor().equals(message.getJDA().getSelfUser()) ? "assistant" : "user";
		var text = message.getContentDisplay();

		return new ResponsesApiRequest.Input.Builder().role(role).text(text).build();
	}
}
