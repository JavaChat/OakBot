package oakbot.discord;

import static oakbot.util.StringUtils.plural;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
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
import oakbot.ai.openai.OpenAIException;
import oakbot.ai.stabilityai.StabilityAIClient;
import oakbot.ai.stabilityai.StabilityAIException;
import oakbot.ai.stabilityai.StableImageCoreRequest;
import oakbot.ai.stabilityai.StableImageDiffusionRequest;
import oakbot.listener.chatgpt.UsageQuota;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;

/**
 * @author Michael Angstadt
 */
public class ImagineCommand implements DiscordSlashCommand {
	private static final String OPT_PROMPT = "prompt";
	private static final String OPT_INPUT_IMAGE = "input_image";
	private static final String OPT_MODEL = "model";

	private static final Model DEFAULT_MODEL = Model.DALLE_3;

	private final OpenAIClient openAIClient;
	private final StabilityAIClient stabilityAIClient;
	private final UsageQuota usageQuota = new UsageQuota(Duration.ofDays(1), 2);
	private final OkHttpClient httpClient = new OkHttpClient();

	public ImagineCommand(OpenAIClient openAIClient, StabilityAIClient stabilityAIClient) {
		this.openAIClient = openAIClient;
		this.stabilityAIClient = stabilityAIClient;
	}

	@Override
	public SlashCommandData data() {
		//@formatter:off
		return Commands.slash("imagine", "Creates images using AI image generators. Users can make 2 requests per day.")
			.addOption(OptionType.STRING, OPT_PROMPT, "Describes what the image should look like.", true)
			.addOption(OptionType.ATTACHMENT, OPT_INPUT_IMAGE, "The input image (only supported by certain models).")
			.addOptions(new OptionData(OptionType.STRING, OPT_MODEL, "Defines which model to use (defaults to \"" + DEFAULT_MODEL.display + "\").")
				.addChoices(Arrays.stream(Model.values()).map(m -> new Choice(m.display, m.id)).toList())
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

		var inputImage = event.getOption(OPT_INPUT_IMAGE, OptionMapping::getAsAttachment);

		var modelOption = event.getOption(OPT_MODEL);
		Model model;
		if (modelOption == null) {
			model = DEFAULT_MODEL;
		} else {
			var id = modelOption.getAsString();
			model = Model.getById(id);
			if (model == null) {
				event.reply("Unknown model: " + id);
				return;
			}
		}

		/*
		 * Log the request now because users can submit multiple requests before
		 * the first image comes back.
		 */
		if (!context.authorIsAdmin()) {
			synchronized (usageQuota) {
				usageQuota.logRequest(userId);
			}
		}

		var cb = new ChatBuilder().append("🎨 Submitted ").bold(model.display).append(" request");
		if (inputImage != null) {
			cb.append(" with an ").bold("input image").append(", and");
		}
		cb.append(" with the following prompt: ").bold(prompt).toString();

		/*
		 * If the bot does not respond within a couple seconds, the slash
		 * command times out and any other files or messages you try to send are
		 * rejected.
		 * 
		 * As a work around, you can chain additional actions using "flatMap".
		 */
		event.reply(cb.toString()).flatMap(m -> {
			try {
				return switch (model) {
				case DALLE_2, DALLE_3 -> handleDallE(event, model, inputImage, prompt);
				case SI_CORE -> handleStableImageCore(event, prompt);
				case SD_3, SD_3_TURBO -> handleStableDiffusion(event, model, inputImage, prompt);
				default -> throw new IllegalArgumentException("Unsupported model: " + model.display);
				};
			} catch (Exception e) {
				if (!context.authorIsAdmin()) {
					synchronized (usageQuota) {
						usageQuota.removeLast(userId);
					}
				}
				return event.getChannel().sendMessage(new ChatBuilder().code().append("ERROR BEEP BOOP: ").append(e.getMessage()).code());
			}
		}).queue();
	}

	private RestAction<Message> handleDallE(SlashCommandInteractionEvent event, Model model, Attachment inputImage, String prompt) throws OpenAIException, IOException {
		CreateImageResponse response;
		if (inputImage == null) {
			var lowestResolutionSupportedByModel = (Model.DALLE_2 == model) ? "256x256" : "1024x1024";
			response = openAIClient.createImage(model.id, lowestResolutionSupportedByModel, prompt);
		} else {
			response = openAIClient.createImageVariation(inputImage.getUrl(), "256x256");
		}

		var revisedPrompt = response.getRevisedPrompt();
		if (revisedPrompt != null) {
			return event.getChannel().sendMessage(new ChatBuilder().bold("Revised prompt: ").append(revisedPrompt)).flatMap(m2 -> sendFile(event, response.getUrl(), prompt));
		}

		return sendFile(event, response.getUrl(), prompt);
	}

	private RestAction<Message> handleStableImageCore(SlashCommandInteractionEvent event, String prompt) throws StabilityAIException, IOException {
		//@formatter:off
		var response = stabilityAIClient.generateImage(new StableImageCoreRequest.Builder()
			.prompt(prompt)
			.outputFormat("jpeg")
		.build());
		//@formatter:on

		return sendFile(event, response.getImage(), "jpg", prompt);
	}

	private RestAction<Message> handleStableDiffusion(SlashCommandInteractionEvent event, Model model, Attachment inputImage, String prompt) throws IOException {
		//@formatter:off
		var builder = new StableImageDiffusionRequest.Builder()
			.model(model.id)
			.prompt(prompt)
			.outputFormat("jpeg");
		//@formatter:on

		if (inputImage != null) {
			if (!inputImage.isImage()) {
				throw new IllegalArgumentException("The provided input image is not an image.");
			}

			byte[] image;
			String contentType;
			try (var client = HttpFactory.connect().getClient()) {
				var getRequest = new HttpGet(inputImage.getUrl());
				try (var response = client.execute(getRequest)) {
					contentType = response.getEntity().getContentType().getValue();
					image = EntityUtils.toByteArray(response.getEntity());

					/*
					 * Stable Diffusion doesn't support GIF.
					 */
					if (contentType.startsWith("image/gif")) {
						image = convertToPng(image);
						if (image == null) {
							throw new IllegalArgumentException("The provided GIF input image could not be converted to PNG.");
						}
					}
				}
			}
			builder.image(image, contentType, 0.5);
		}

		var response = stabilityAIClient.generateImage(builder.build());

		return sendFile(event, response.getImage(), "jpg", prompt);
	}

	private RestAction<Message> sendFile(SlashCommandInteractionEvent event, String url, String prompt) {
		DownloadedFile image;
		try {
			image = download(url);
		} catch (Exception e) {
			return event.getChannel().sendMessage(new ChatBuilder().code().append("ERROR BEEP BOOP: ").append(e.getMessage()).code());
		}

		return sendFile(event, image.data(), image.contentType().subtype(), prompt);
	}

	private RestAction<Message> sendFile(SlashCommandInteractionEvent event, byte[] data, String extension, String prompt) {
		var filename = filename(prompt, extension);
		return event.getChannel().sendFiles(FileUpload.fromData(data, filename));
	}

	private DownloadedFile download(String url) throws IOException {
		var request = new okhttp3.Request.Builder().url(url).get().build();
		var response = httpClient.newCall(request).execute();

		var contentType = response.body().contentType();
		var data = response.body().bytes();
		return new DownloadedFile(contentType, data);
	}

	private String filename(String prompt, String extension) {
		LocalDateTime now = LocalDateTime.now();
		var date = now.format(DateTimeFormatter.BASIC_ISO_DATE);

		var words = 3;

		//@formatter:off
		var name = Arrays.stream(prompt.split("\\s+", words+1))
			.limit(words)
			.map(String::toLowerCase)
		.collect(Collectors.joining("-"));
		//@formatter:on

		return date + "-" + name + "." + extension;
	}

	private byte[] convertToPng(byte[] data) throws IOException {
		BufferedImage image;
		try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
			image = ImageIO.read(in);
		}
		if (image == null) {
			return null;
		}

		try (var out = new ByteArrayOutputStream()) {
			ImageIO.write(image, "PNG", out);
			return out.toByteArray();
		}
	}

	private record DownloadedFile(MediaType contentType, byte[] data) {
	}

	private enum Model {
		//@formatter:off
		DALLE_2("OpenAI DALL·E 2", "dall-e-2"),
		DALLE_3("OpenAI DALL·E 3", "dall-e-3"),
		SI_CORE("Stability Image Core", "si-core"),
		SD_3("Stable Diffusion 3", "sd3"),
		SD_3_TURBO("Stable Diffusion 3 Turbo", "sd3-turbo");
		//@formatter:on

		private final String id;
		private final String display;

		private Model(String display, String id) {
			this.display = display;
			this.id = id;
		}

		public static Model getById(String id) {
			//@formatter:off
			return Arrays.stream(Model.values())
				.filter(m -> m.id.equals(id))
			.findFirst().orElse(null);
			//@formatter:on
		}
	}
}
