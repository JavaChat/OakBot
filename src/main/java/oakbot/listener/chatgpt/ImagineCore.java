package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;
import static oakbot.util.StringUtils.plural;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.ai.openai.CreateGptImageRequest;
import oakbot.ai.openai.CreateImageResponse;
import oakbot.ai.openai.OpenAIClient;
import oakbot.ai.openai.OpenAIException;
import oakbot.ai.openai.ResponsesApiRequest;
import oakbot.ai.stabilityai.StabilityAIClient;
import oakbot.ai.stabilityai.StabilityAIException;
import oakbot.ai.stabilityai.StableImageCoreRequest;
import oakbot.ai.stabilityai.StableImageDiffusionRequest;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;
import oakbot.util.ImageUtils;

/**
 * Generates images using various AI image models. This class is used by the
 * "/imagine" and "/imagine-exact" commands.
 * @author Michael Angstadt
 */
public class ImagineCore {
	private static final Logger logger = LoggerFactory.getLogger(ImagineCore.class);

	private final OpenAIClient openAIClient;
	private final StabilityAIClient stabilityAIClient;
	private final ChatGPT chatGPT;
	private final int requestsPer24Hours;
	private final UsageQuota usageQuota;

	/**
	 * @param openAIClient the OpenAI client
	 * @param stabilityAIClient the Stability AI client
	 * @param chatGPT the ChatGPT listener
	 * @param requestsPer24Hours requests allowed per user per 24 hours, or
	 * {@literal <= 0} for no limit
	 */
	public ImagineCore(OpenAIClient openAIClient, StabilityAIClient stabilityAIClient, ChatGPT chatGPT, int requestsPer24Hours) {
		this.openAIClient = openAIClient;
		this.stabilityAIClient = stabilityAIClient;
		this.chatGPT = chatGPT;
		this.requestsPer24Hours = requestsPer24Hours;
		usageQuota = (requestsPer24Hours > 0) ? new UsageQuota(Duration.ofDays(1), requestsPer24Hours) : UsageQuota.allowAll();
	}

	public String helpDetail() {
		var requestLimit = (requestsPer24Hours > 0) ? "Users can make " + requestsPer24Hours + " requests per day. " : "";
		return requestLimit + "Syntax: [model] [input image URL] [prompt]. Supported models: " + Arrays.toString(Model.values());
	}

	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		return onMessage(chatCommand, bot, false);
	}

	public ChatActions onMessageEmbellish(ChatCommand chatCommand, IBot bot) {
		return onMessage(chatCommand, bot, true);
	}

	private ChatActions onMessage(ChatCommand chatCommand, IBot bot, boolean embellish) {
		var quotaReached = checkQuota(chatCommand);
		if (quotaReached != null) {
			return quotaReached;
		}

		var content = chatCommand.getContent();
		if (content.isBlank()) {
			return reply("Imagine what?", chatCommand);
		}

		var parameters = new ImagineParameterParser(content).parse();

		var inputImage = parameters.inputImage();
		var prompt = parameters.prompt();
		var model = chooseWhichModelToUse(parameters.model(), inputImage, prompt);

		var error = validateParameters(model, inputImage, prompt);
		if (error != null) {
			return reply(error, chatCommand);
		}

		try {
			var promptToSend = embellish ? embellish(prompt) : prompt;

			var messagesToPost = new ArrayList<String>();
			if (embellish) {
				messagesToPost.add("Embellished prompt: " + promptToSend);
			}

			messagesToPost.addAll(generateImage(model, inputImage, promptToSend, bot));

			logQuota(chatCommand, bot);

			var actions = new ChatActions();

			//@formatter:off
			messagesToPost.stream()
				.map(message -> new PostMessage(message).bypassFilters(true).splitStrategy(SplitStrategy.WORD))
			.forEach(actions::addAction);
			//@formatter:on

			return actions;
		} catch (IllegalArgumentException | URISyntaxException | OpenAIException | StabilityAIException | TranscoderException e) {
			return reply(new ChatBuilder().code().append("ERROR BEEP BOOP: ").append(e.getMessage()).code(), chatCommand);
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Network error.");
			return error("Network error: ", e, chatCommand);
		}
	}

	/**
	 * Checks if the user's quota has been reached.
	 * @param chatCommand
	 * @return the message to post if the user's quota has been reached, or null
	 * if the quota has not been reached
	 */
	public ChatActions checkQuota(ChatCommand chatCommand) {
		var userId = chatCommand.getMessage().userId();
		var timeUntilNextRequest = usageQuota.getTimeUntilUserCanMakeRequest(userId);
		if (!timeUntilNextRequest.isZero()) {
			var hours = timeUntilNextRequest.toHours() + 1;
			return reply("Bad human! You are over quota and can't make any more requests right now. Try again in " + hours + " " + plural("hour", hours) + ".", chatCommand);
		}

		return null;
	}

	public void logQuota(ChatCommand chatCommand, IBot bot) {
		var userId = chatCommand.getMessage().userId();
		if (!bot.isAdminUser(userId)) {
			usageQuota.logRequest(userId);
		}
	}

	/**
	 * Sends the image generation request, downloads the generated image, and
	 * uploads it to SO Chat.
	 * @param model the model
	 * @param inputImage the input image or null if there is no input iamge
	 * @param prompt the prompt
	 * @param bot
	 * @return the response(s) to post to the chat. Includes the image URL if an
	 * image was generated.
	 * @throws OpenAIException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public List<String> generateImage(Model model, String inputImage, String prompt, IBot bot) throws OpenAIException, IOException, URISyntaxException, TranscoderException {
		return switch (model) {
		case GPT_IMAGE_1, GPT_IMAGE_1_MINI, GPT_IMAGE_15, GPT_IMAGE_2 -> handleOpenAi(model, inputImage, prompt, bot);
		case STABLE_IMAGE_CORE -> List.of(handleStableImageCore(prompt, bot));
		case STABLE_DIFFUSION, STABLE_DIFFUSION_TURBO -> List.of(handleStableDiffusion(model, inputImage, prompt, bot));
		case SVG -> handleSvg(prompt, bot);
		};
	}

	private String embellish(String prompt) throws OpenAIException, IOException {
		String model = (chatGPT == null) ? null : chatGPT.getModel();

		//@formatter:off
		var apiRequest = new ResponsesApiRequest.Builder()
			.model(model)
			.reasoningEffort("none")
			.verbosity("low")
			.inputs(List.of(
				new ResponsesApiRequest.Input.Builder()
					.role("user")
					.text("Compose a single image generation prompt based off of the following: " + prompt)
				.build()
			))
		.build();
		//@formatter:on

		var apiResponse = openAIClient.responsesApi(apiRequest);
		return apiResponse.getOutput().get(0).content();
	}

	static Model chooseWhichModelToUse(Model model, String inputImage, String prompt) {
		if (model != null) {
			return model;
		}

		if (inputImage != null && prompt != null) {
			return Model.STABLE_DIFFUSION;
		}

		return Model.GPT_IMAGE_1_MINI;
	}

	static String validateParameters(Model model, String inputImage, String prompt) {
		if (inputImage == null && prompt == null) {
			return "Imagine what?";
		}

		if (inputImage != null) {
			return switch (model) {
			case GPT_IMAGE_1, GPT_IMAGE_1_MINI, GPT_IMAGE_15, GPT_IMAGE_2, STABLE_IMAGE_CORE, SVG -> "Model does not support image variations.";
			case STABLE_DIFFUSION, STABLE_DIFFUSION_TURBO -> (prompt == null) ? "Prompt missing for image variation." : null;
			};
		}

		return null;
	}

	private List<String> handleOpenAi(Model model, String inputImageUrl, String prompt, IBot bot) throws OpenAIException, IOException, URISyntaxException {
		CreateImageResponse response;
		if (inputImageUrl == null) {
			var lowestResolutionSupportedByModel = "1024x1024";

			//@formatter:off
			CreateGptImageRequest apiRequest = new CreateGptImageRequest.Builder()
				.model(model.id)
				.prompt(prompt)
				.size(lowestResolutionSupportedByModel)
				.outputCompression(90)
				.outputFormat("jpeg")
			.build();
			//@formatter:on

			response = openAIClient.createImage(apiRequest);
		} else {
			response = openAIClient.createImageVariation(inputImageUrl, "256x256");
		}

		String imageUrl;
		if (response.url() != null) {
			imageUrl = uploadImageFromUrl(bot, response.url());
		} else {
			imageUrl = uploadImage(bot, response.data());
		}

		var messagesToPost = new ArrayList<String>();
		if (response.revisedPrompt() != null && !response.revisedPrompt().equals(prompt)) {
			messagesToPost.add("I'm going to use this prompt instead: " + response.revisedPrompt());
		}
		messagesToPost.add(imageUrl);

		return messagesToPost;
	}

	private String handleStableImageCore(String prompt, IBot bot) throws StabilityAIException, IOException {
		//@formatter:off
		var response = stabilityAIClient.generateImage(new StableImageCoreRequest.Builder()
			.prompt(prompt)
			.outputFormat("jpeg")
		.build());
		//@formatter:on

		return uploadImage(bot, response.getImage());
	}

	private String handleStableDiffusion(Model model, String inputImage, String prompt, IBot bot) throws IOException {
		//@formatter:off
		var builder = new StableImageDiffusionRequest.Builder()
			.model(model.id)
			.prompt(prompt)
			.outputFormat("jpeg");
		//@formatter:on

		if (inputImage != null) {
			byte[] image;
			String contentType;
			try (var client = HttpFactory.connect().getClient()) {
				var getRequest = new HttpGet(inputImage);
				try (var response = client.execute(getRequest)) {
					contentType = response.getEntity().getContentType().getValue();
					if (!contentType.startsWith("image/")) {
						throw new IllegalArgumentException("The provided input image URL is not an image.");
					}
					image = EntityUtils.toByteArray(response.getEntity());
				}
			}

			/*
			 * Stable Diffusion doesn't support GIF.
			 */
			if (contentType.startsWith("image/gif")) {
				image = ImageUtils.convertToPng(image);
				contentType = "image/png";
			}

			builder.image(image, contentType, 0.5);
		}

		var response = stabilityAIClient.generateImage(builder.build());

		return uploadImage(bot, response.getImage());
	}

	private List<String> handleSvg(String prompt, IBot bot) throws OpenAIException, TranscoderException, IOException {
		/*
		 * feDropShadow: Throws an exception when converting to PNG.
		 */
		var featuresToExclude = List.of("feDropShadow");

		String model = (chatGPT == null) ? null : chatGPT.getModel();

		//@formatter:off
		var apiRequest = new ResponsesApiRequest.Builder()
			.model(model)
			.reasoningEffort("none")
			.verbosity("low")
			.inputs(List.of(
				new ResponsesApiRequest.Input.Builder()
					.role("user")
					.text("Generate SVG code representing the scene below. Do not use these SVG features: " + featuresToExclude + "\n\n" + prompt)
				.build()
			))
		.build();
		//@formatter:on

		final var MAX_TRIES = 3;
		String content = null;
		for (var tries = 0; tries < MAX_TRIES; tries++) {
			var apiResponse = openAIClient.responsesApi(apiRequest);
			content = apiResponse.getOutput().get(0).content();

			var svg = extractSvgFromResponse(content);
			if (svg != null) {
				var pngBytes = ImageUtils.svgToPng(svg);
				var imageUrl = uploadImage(bot, pngBytes);

				return List.of(imageUrl, new ChatBuilder().fixedWidth().append(svg).toString());
			}
		}

		return List.of(content);
	}

	private String extractSvgFromResponse(String response) {
		final var END_TAG = "</svg>";
		var startOfXml = response.indexOf("<svg");
		var startOfEndTag = response.indexOf(END_TAG);
		if (startOfXml < 0 || startOfEndTag < 0) {
			return null;
		}

		var endOfXml = startOfEndTag + END_TAG.length();
		return response.substring(startOfXml, endOfXml);
	}

	static class ImagineParameterParser {
		private final String content;
		private final String token1;
		private final String token2;
		private final String rest;

		public ImagineParameterParser(String content) {
			this.content = content.trim();

			var split = this.content.split("\\s+", 3);
			token1 = split[0];
			token2 = (split.length > 1) ? split[1] : null;
			rest = (split.length > 2) ? split[2] : null;
		}

		public ImagineCommandParameters parse() {
			var model = Model.find(token1);
			if (model != null) {
				return token1IsModel(model);
			}

			if (isUrl(token1)) {
				return token1IsUrl();
			}

			return new ImagineCommandParameters(null, null, content);
		}

		private ImagineCommandParameters token1IsModel(Model model) {
			String inputImage = null;
			String prompt = null;

			if (token2 != null) {
				if (isUrl(token2)) {
					inputImage = token2;
					prompt = rest;
				} else {
					prompt = promptIsAfterToken1(token2, rest);
				}
			}

			return new ImagineCommandParameters(model, inputImage, prompt);
		}

		private ImagineCommandParameters token1IsUrl() {
			var inputImage = token1;
			Model model = null;
			String prompt = null;

			if (token2 != null) {
				prompt = promptIsAfterToken1(token2, rest);
			}

			return new ImagineCommandParameters(model, inputImage, prompt);
		}

		private String promptIsAfterToken1(String token2, String rest) {
			return token2 + ((rest == null) ? "" : " " + rest);
		}

		private boolean isUrl(String url) {
			return url.toLowerCase().matches("^https?://.*");
		}
	}

	record ImagineCommandParameters(Model model, String inputImage, String prompt) {
	}

	private String uploadImageFromUrl(IBot bot, String imageUrl) throws URISyntaxException {
		try {
			return bot.uploadImage(imageUrl);
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem uploading image to chat room. URL: " + imageUrl);

			/*
			 * Add a fake parameter onto the end of the URL so SO Chat one-boxes
			 * the image. SO Chat will only one-box an image if the URL ends in
			 * an image extension.
			 */
			return new URIBuilder(imageUrl).addParameter("a", ".png").toString();
		}
	}

	private String uploadImage(IBot bot, byte[] imageData) throws IOException {
		try {
			return bot.uploadImage(imageData);
		} catch (IOException e) {
			var kb = imageData.length / 1024;
			throw new IOException("Problem uploading image to chat room. File size: " + kb + " KB", e);
		}
	}

	public UsageQuota getUsageQuota() {
		return usageQuota;
	}

	public OpenAIClient getOpenAIClient() {
		return openAIClient;
	}

	enum Model {
		//@formatter:off
		GPT_IMAGE_1("gpt-image-1"), 
		GPT_IMAGE_1_MINI("gpt-image-1-mini"), 
		GPT_IMAGE_15("gpt-image-1.5"), 
		GPT_IMAGE_2("gpt-image-2"), 
		STABLE_IMAGE_CORE("si-core"), 
		STABLE_DIFFUSION("sd3"), 
		STABLE_DIFFUSION_TURBO("sd3-turbo"),
		SVG("svg");
		//@formatter:on

		private final String id;

		private Model(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return id;
		}

		public static Model find(String id) {
			//@formatter:off
			return Arrays.stream(Model.values())
				.filter(model -> model.id.equalsIgnoreCase(id))
			.findFirst().orElse(null);
			//@formatter:on
		}
	}
}
