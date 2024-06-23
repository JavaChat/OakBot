package oakbot.discord;

import static oakbot.util.StringUtils.plural;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import oakbot.ai.openai.OpenAIClient;
import oakbot.command.HelpDoc;
import oakbot.listener.chatgpt.UsageQuota;
import okhttp3.OkHttpClient;

/**
 * @author Michael Angstadt
 */
public class ImagineCommand implements DiscordCommand {
	private final OpenAIClient client;
	private final UsageQuota usageQuota = new UsageQuota(Duration.ofDays(1), 2);
	private final OkHttpClient httpClient = new OkHttpClient();

	public ImagineCommand(OpenAIClient client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "imagine";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new DiscordHelpDoc.Builder(this)
			.summary("Creates images using OpenAI's DALL·E.")
			.detail("Users can make 2 requests per day.")
			.example("a cute Java programmer", "Generates an image of a cute Java programmer using DALL·E 3.")
		.build();
		//@formatter:on
	}

	@Override
	public void onMessage(String content, MessageReceivedEvent event, BotContext context) {
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

		try {
			var response = client.createImage("dall-e-3", "1024x1024", content);

			if (!context.authorIsAdmin()) {
				synchronized (usageQuota) {
					usageQuota.logRequest(userId);
				}
			}

			var revisedPrompt = response.getRevisedPrompt();
			if (revisedPrompt != null) {
				event.getChannel().sendMessage("I'm going to use this prompt instead:\n" + revisedPrompt).queue();
			}

			var image = download(response.getUrl());
			var filename = filename(content);
			var file = FileUpload.fromData(image, filename);
			event.getChannel().sendFiles(file).queue();
		} catch (Exception e) {
			event.getChannel().sendMessage("`ERROR BEEP BOOP: " + e.getMessage() + "`").queue();
			return;
		}
	}

	private byte[] download(String url) throws IOException {
		var request = new okhttp3.Request.Builder().url(url).get().build();
		var response = httpClient.newCall(request).execute();
		return response.body().bytes();
	}

	private String filename(String prompt) {
		LocalDateTime now = LocalDateTime.now();
		var date = now.format(DateTimeFormatter.BASIC_ISO_DATE);

		var words = 3;

		//@formatter:off
		var name = Arrays.stream(prompt.split("\\s+", words+1))
			.limit(words)
			.map(String::toLowerCase)
		.collect(Collectors.joining("-"));
		//@formatter:on

		return date + "-" + name + ".png";
	}
}
