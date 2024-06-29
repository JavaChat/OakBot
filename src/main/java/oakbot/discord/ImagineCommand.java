package oakbot.discord;

import static oakbot.util.StringUtils.plural;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
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
	private static final String OPT_MODEL = "model";

	private static final Model DEFAULT_MODEL = new Model("OpenAI DALL·E 3", "dall-e-3");

	//@formatter:off
	private static final List<Model> MODELS = List.of(
		new Model("OpenAI DALL·E 2", "dall-e-2"),
		DEFAULT_MODEL
		//new Model("Stable Image Core", "si-core"),
		//new Model("Stable Diffusion 3", "sd3"),
		//new Model("Stable Diffusion 3 Turbo", "sd3-turbo")
	);
	//@formatter:on

	private final OpenAIClient client;
	private final UsageQuota usageQuota = new UsageQuota(Duration.ofDays(1), 2);
	private final OkHttpClient httpClient = new OkHttpClient();

	public ImagineCommand(OpenAIClient client) {
		this.client = client;
	}

	@Override
	public SlashCommandData data() {
		//@formatter:off
		return Commands.slash("imagine", "Creates images using AI image generators. Users can make 2 requests per day.")
			.addOption(OptionType.STRING, OPT_PROMPT, "Describes what the image should look like.", true)
			.addOptions(new OptionData(OptionType.STRING, OPT_MODEL, "Defines which model to use (defaults to \"" + DEFAULT_MODEL.display() + "\").")
				.addChoices(MODELS.stream().map(m -> new Choice(m.display(), m.id())).toList())
			);
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

		var modelOption = event.getOption(OPT_MODEL);
		var modelId = (modelOption == null) ? DEFAULT_MODEL.id() : modelOption.getAsString();
		var modelDisplay = (modelOption == null) ? DEFAULT_MODEL.display() : getModelById(modelId).display();

		var size = "dall-e-3".equals(modelId) ? "1024x1024" : "256x256";

		/*
		 * If the bot doesn't respond within a very short amount of time, the
		 * slash command times out and any files or messages you try to send are
		 * rejected.
		 * 
		 * As a work around, you can chain additional actions using "flatMap".
		 */
		var message = new ChatBuilder().append("🎨 Submitted ").bold(modelDisplay).append(" request with the following prompt: ").bold(prompt).toString();
		event.reply(message.toString()).flatMap(m -> {
			CreateImageResponse response;
			try {
				response = client.createImage(modelId, size, prompt);
			} catch (Exception e) {
				return event.getChannel().sendMessage(new ChatBuilder().code().append("ERROR BEEP BOOP: " + e.getMessage()).code());
			}

			if (!context.authorIsAdmin()) {
				synchronized (usageQuota) {
					usageQuota.logRequest(userId);
				}
			}

			var revisedPrompt = response.getRevisedPrompt();
			if (revisedPrompt != null) {
				return event.getChannel().sendMessage(new ChatBuilder().bold("Revised prompt: ").append(revisedPrompt)).flatMap(m2 -> sendFile(event, response, prompt));
			}

			return sendFile(event, response, prompt);
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

	private record Model(String display, String id) {
	}

	private static Model getModelById(String id) {
		//@formatter:off
		return MODELS.stream()
			.filter(m -> m.id().equals(id))
		.findFirst().orElse(null);
		//@formatter:on
	}
}
