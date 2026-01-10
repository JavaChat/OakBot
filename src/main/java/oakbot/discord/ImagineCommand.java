package oakbot.discord;

import static oakbot.util.StringUtils.plural;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

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
import oakbot.util.ImageUtils;
import okhttp3.MediaType;

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

	public ImagineCommand(OpenAIClient openAIClient, StabilityAIClient stabilityAIClient) {
		this.openAIClient = openAIClient;
		this.stabilityAIClient = stabilityAIClient;
	}

	@Override
	public SlashCommandData data() {
		var name = "imagine";
		var description = "Creates images using AI image generators. Users can make 2 requests per day.";

		//@formatter:off
		return Commands.slash(name, description)
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

		var hours = hoursUntilUserCanMakeAnotherRequest(userId);
		if (hours > 0) {
			event.reply("Bad human! You are over quota. Try again in " + hours + " " + plural("hour", hours) + ".").queue();
			return;
		}

		var prompt = event.getOption(OPT_PROMPT, OptionMapping::getAsString);
		var inputImage = event.getOption(OPT_INPUT_IMAGE, OptionMapping::getAsAttachment);

		var modelOption = event.getOption(OPT_MODEL);
		var model = determineModel(modelOption);
		if (model == null) {
			event.reply("Unknown model: " + modelOption.getAsString()).queue();
			return;
		}

		var modelDoesNotSupportInputImages = (inputImage != null && !model.supportsInputImages());
		if (modelDoesNotSupportInputImages) {
			event.reply(new ChatBuilder().bold(model.display).append(" model does not support input images.").toString()).setEphemeral(true).queue();
			return;
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

		var reply = buildRequestReceivedReply(model, inputImage, prompt);

		/*
		 * If the bot does not respond within 3 seconds, the slash command times
		 * out and any other files or messages you try to send are rejected.
		 * 
		 * As a work around, you can chain additional actions using "flatMap",
		 * or call "deferReply".
		 */
		event.reply(reply).flatMap(m -> sendImageGenerationRequest(event, context, userId, model, inputImage, prompt)).queue();
	}

	private long hoursUntilUserCanMakeAnotherRequest(long userId) {
		Duration timeUntilNextRequest;
		synchronized (usageQuota) {
			timeUntilNextRequest = usageQuota.getTimeUntilUserCanMakeRequest(userId);
		}

		return timeUntilNextRequest.isZero() ? 0 : timeUntilNextRequest.toHours() + 1;
	}

	private Model determineModel(OptionMapping modelOption) {
		if (modelOption == null) {
			return DEFAULT_MODEL;
		}

		var id = modelOption.getAsString();
		return Model.getById(id);
	}

	private String buildRequestReceivedReply(Model model, Attachment inputImage, String prompt) {
		var reply = new ChatBuilder();
		var inputImageProvided = (inputImage != null);

		reply.append("🎨 Submitted ").bold(model.display).append(" request");
		if (model == Model.DALLE_2 && inputImageProvided) {
			reply.append(" with an ").bold("input image");
			reply.append(". The prompt will be ignored because the model does not support requests that contain both a prompt and an input image.");
		} else {
			if (inputImageProvided) {
				reply.append(" with an ").bold("input image").append(", and");
			}
			reply.append(" with the following prompt: ").bold(prompt);
		}

		return reply.toString();
	}

	private RestAction<Message> sendImageGenerationRequest(SlashCommandInteractionEvent event, BotContext context, long userId, Model model, Attachment inputImage, String prompt) {
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
	}

	private RestAction<Message> handleDallE(SlashCommandInteractionEvent event, Model model, Attachment inputImage, String prompt) throws OpenAIException, IOException {
		CreateImageResponse response;
		if (inputImage == null) {
			var lowestResolutionSupportedByModel = (Model.DALLE_2 == model) ? "256x256" : "1024x1024";
			response = openAIClient.createImage(model.id, lowestResolutionSupportedByModel, null, null, prompt);
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

			var image = download(inputImage.getUrl());

			/*
			 * Stable Diffusion does not support GIF.
			 */
			byte[] data;
			String contentType;
			if (image.contentType.type().equals("image") && image.contentType.subtype().equals("gif")) {
				data = ImageUtils.convertToPng(image.data());
				contentType = "image/png";
			} else {
				data = image.data();
				contentType = image.contentType().toString();
			}
			builder.image(data, contentType, 0.5);
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
		var response = HttpFactory.okHttp().newCall(request).execute();

		var contentType = response.body().contentType();
		var data = response.body().bytes();
		return new DownloadedFile(contentType, data);
	}

	private String filename(String prompt, String extension) {
		var now = LocalDateTime.now();
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

		public boolean supportsInputImages() {
			return switch (this) {
			case DALLE_2, SD_3, SD_3_TURBO -> true;
			default -> false;
			};
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
