package oakbot.discord;

import static oakbot.util.StringUtils.plural;
import static oakbot.util.StringUtils.possessive;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.FileUpload;
import oakbot.ai.openai.CreateImageResponse;
import oakbot.ai.openai.OpenAIClient;
import oakbot.listener.chatgpt.UsageQuota;
import oakbot.util.ChatBuilder;
import okhttp3.OkHttpClient;

/**
 * @author Michael Angstadt
 */
public class ImagineCommand implements DiscordSlashCommand {
	private static final String OPT_PROMPT = "prompt";

	private final OpenAIClient client;
	private final UsageQuota usageQuota = new UsageQuota(Duration.ofDays(1), 2);
	private final OkHttpClient httpClient = new OkHttpClient();

	public ImagineCommand(OpenAIClient client) {
		this.client = client;
	}

	@Override
	public SlashCommandData data() {
		//@formatter:off
		var description = new ChatBuilder()
			.append("Creates images using OpenAI's DALL·E 3. Users can make 2 requests per day.")
		.toString();

		return Commands.slash("imagine", description)
			.addOption(OptionType.STRING, OPT_PROMPT, "Describes what the image should look like.", true);
		//@formatter:on
	}

	@Override
	public void onMessage(SlashCommandInteractionEvent event, BotContext context) {
		var userId = event.getUser().getIdLong();

		Duration timeUntilNextRequest;
		synchronized (usageQuota) {
			timeUntilNextRequest = usageQuota.getTimeUntilUserCanMakeRequest(userId);
		}

		if (!timeUntilNextRequest.isZero()) {
			var hours = timeUntilNextRequest.toHours() + 1;
			event.reply("Bad human! You are over quota. Try again in " + hours + " " + plural("hour", hours) + ".").queue();
			return;
		}

		var prompt = event.getOption(OPT_PROMPT, OptionMapping::getAsString);

		/*
		 * If the bot doesn't respond within a very short amount of time, the
		 * slash command times out and any files or messages you try to send are
		 * rejected.
		 * 
		 * As a work around, you can chain additional actions using "flatMap".
		 */
		event.reply("Working...").setEphemeral(true).flatMap(m -> {
			CreateImageResponse response;
			try {
				response = client.createImage("dall-e-3", "1024x1024", prompt);
			} catch (Exception e) {
				return event.getChannel().sendMessage(new ChatBuilder().code().append("ERROR BEEP BOOP: " + e.getMessage()).code());
			}

			if (!context.authorIsAdmin()) {
				synchronized (usageQuota) {
					usageQuota.logRequest(userId);
				}
			}

			var username = event.getUser().getEffectiveName();
			var cb = new ChatBuilder().bold().append(possessive(username)).append(" prompt: ").bold().append(prompt);

			var revisedPrompt = response.getRevisedPrompt();
			if (revisedPrompt != null) {
				cb.nl().bold("Revised prompt: ").append(revisedPrompt);
			}

			return event.getChannel().sendMessage(cb).flatMap(m2 -> sendFile(event, response, prompt));
		}).queue();
	}

	private RestAction<Message> sendFile(SlashCommandInteractionEvent event, CreateImageResponse response, String prompt) {
		try {
			var image = download(response.getUrl());
			var filename = filename(prompt);
			var file = FileUpload.fromData(image, filename);

			return event.getChannel().sendFiles(file);
		} catch (IOException e) {
			return event.getChannel().sendMessage(new ChatBuilder().code().append("ERROR BEEP BOOP: " + e.getMessage()).code());
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
