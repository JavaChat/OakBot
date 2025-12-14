package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;
import static oakbot.util.StringUtils.plural;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.ai.openai.CreateImageResponse;
import oakbot.ai.openai.OpenAIClient;
import oakbot.ai.openai.OpenAIException;
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

	static final String MODEL_DALLE_2 = "dall-e-2";
	static final String MODEL_DALLE_3 = "dall-e-3";
	/*
	 * "Your organization must be verified to use the model `gpt-image-1`.
	 * Please go to: https://platform.openai.com/settings/organization/general
	 * and click on Verify Organization."
	 */
	static final String MODEL_GPT_IMAGE_1 = "gpt-image-1";

	static final String MODEL_STABLE_IMAGE_CORE = "si-core";
	static final String MODEL_STABLE_DIFFUSION = "sd3";
	static final String MODEL_STABLE_DIFFUSION_TURBO = "sd3-turbo";
	static final List<String> supportedModels = List.of(MODEL_DALLE_2, MODEL_DALLE_3, MODEL_STABLE_IMAGE_CORE, MODEL_STABLE_DIFFUSION, MODEL_STABLE_DIFFUSION_TURBO);

	private static final String EXACT_PROMPT_PHRASE = "I NEED to test how the tool works with extremely simple prompts. DO NOT add any detail, just use it AS-IS.";

	private final OpenAIClient openAIClient;
	private final StabilityAIClient stabilityAIClient;
	private final int requestsPer24Hours;
	private final UsageQuota usageQuota;

	/**
	 * @param openAIClient the OpenAI client
	 * @param stabilityAIClient the Stability AI client
	 * @param requestsPer24Hours requests allowed per user per 24 hours, or
	 * {@literal <= 0} for no limit
	 */
	public ImagineCore(OpenAIClient openAIClient, StabilityAIClient stabilityAIClient, int requestsPer24Hours) {
		this.openAIClient = openAIClient;
		this.stabilityAIClient = stabilityAIClient;
		this.requestsPer24Hours = requestsPer24Hours;
		usageQuota = (requestsPer24Hours > 0) ? new UsageQuota(Duration.ofDays(1), requestsPer24Hours) : UsageQuota.allowAll();
	}

	public String helpDetail() {
		var requestLimit = (requestsPer24Hours > 0) ? "Users can make " + requestsPer24Hours + " requests per day. " : "";
		return requestLimit + "Syntax: [model] [input image URL] [prompt]. Supported models: " + supportedModels;
	}

	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		return onMessage(chatCommand, bot, false);
	}

	public ChatActions onMessageExact(ChatCommand chatCommand, IBot bot) {
		return onMessage(chatCommand, bot, true);
	}

	private ChatActions onMessage(ChatCommand chatCommand, IBot bot, boolean useExactPrompt) {
		/*
		 * Check usage quota.
		 */
		var userId = chatCommand.getMessage().userId();
		var timeUntilNextRequest = usageQuota.getTimeUntilUserCanMakeRequest(userId);
		if (!timeUntilNextRequest.isZero()) {
			var hours = timeUntilNextRequest.toHours() + 1;
			return reply("Bad human! You are over quota and can't make any more requests right now. Try again in " + hours + " " + plural("hour", hours) + ".", chatCommand);
		}

		var parameters = parseContent(chatCommand.getContent());
		if (parameters == null) {
			return reply("Imagine what?", chatCommand);
		}

		var inputImage = parameters.inputImage();
		var prompt = parameters.prompt();
		var model = chooseWhichModelToUse(parameters.model(), inputImage, prompt);

		var error = validateParameters(model, inputImage, prompt);
		if (error != null) {
			return reply(error, chatCommand);
		}

		try {
			List<String> messagesToPost;
			if (MODEL_DALLE_2.equals(model) || MODEL_DALLE_3.equals(model) || MODEL_GPT_IMAGE_1.equals(model)) {
				messagesToPost = handleDallE(model, inputImage, prompt, useExactPrompt, bot);
			} else if (MODEL_STABLE_IMAGE_CORE.equals(model)) {
				messagesToPost = List.of(handleStableImageCore(prompt, bot));
			} else if (MODEL_STABLE_DIFFUSION.equals(model) || MODEL_STABLE_DIFFUSION_TURBO.equals(model)) {
				try {
					messagesToPost = List.of(handleStableDiffusion(model, inputImage, prompt, bot));
				} catch (IllegalArgumentException e) {
					return reply(e.getMessage(), chatCommand);
				}
			} else {
				return reply("Unsupported model: " + model, chatCommand);
			}

			/*
			 * Log quota.
			 */
			if (!bot.isAdminUser(userId)) {
				usageQuota.logRequest(userId);
			}

			var actions = new ChatActions();

			//@formatter:off
			messagesToPost.stream()
				.map(message -> new PostMessage(message).bypassFilters(true).splitStrategy(SplitStrategy.WORD))
			.forEach(actions::addAction);
			//@formatter:on

			return actions;
		} catch (IllegalArgumentException | URISyntaxException | OpenAIException | StabilityAIException e) {
			return reply(new ChatBuilder().code().append("ERROR BEEP BOOP: ").append(e.getMessage()).code(), chatCommand);
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Network error.");
			return error("Network error: ", e, chatCommand);
		}
	}

	static String chooseWhichModelToUse(String model, String inputImage, String prompt) {
		/*
		 * Use dall-e-2 if only a URL was provided. Use sd3 if a URL and
		 * prompt was provided.
		 */
		if (model == null && inputImage != null) {
			return (prompt == null) ? MODEL_DALLE_2 : MODEL_STABLE_DIFFUSION;
		}

		/*
		 * Default to dall-e-3 if no model was specified
		 */
		return (model == null) ? MODEL_DALLE_3 : model;
	}

	static String validateParameters(String model, String inputImage, String prompt) {
		if (inputImage == null && prompt == null) {
			return "Imagine what?";
		}

		/*
		 * Dall-e-2 is the only model that supports an input image without a
		 * prompt.
		 */
		if (!MODEL_DALLE_2.equals(model) && prompt == null) {
			return "Imagine what?";
		}

		if (inputImage != null) {
			if (MODEL_DALLE_3.equals(model)) {
				return "Dall·E 3 does not support image variations.";
			}

			if (MODEL_DALLE_2.equals(model) && prompt != null) {
				return "Dall·E 2 does not support image variations with text prompts.";
			}

			if (MODEL_STABLE_IMAGE_CORE.equals(model)) {
				return "Stable Image Core does not support image variations.";
			}
		}

		return null;
	}

	private List<String> handleDallE(String model, String inputImageUrl, String prompt, boolean useExactPrompt, IBot bot) throws OpenAIException, IOException, URISyntaxException {
		CreateImageResponse response;
		if (inputImageUrl == null) {
			var lowestResolutionSupportedByModel = MODEL_DALLE_2.equals(model) ? "256x256" : "1024x1024";

			String promptToSend = prompt;
			if (MODEL_DALLE_3.equals(model) && useExactPrompt) {
				promptToSend += ". " + EXACT_PROMPT_PHRASE;
			}

			response = openAIClient.createImage(model, lowestResolutionSupportedByModel, null, null, promptToSend);
		} else {
			response = openAIClient.createImageVariation(inputImageUrl, "256x256");
		}

		String imageUrl;
		if (MODEL_DALLE_3.equals(model)) {
			/*
			 * 5/1/2024: StackOverflow's new image hosting system has a file
			 * size limit of 2 MiB. The PNGs that Dall-E 3 generates are usually
			 * larger than that, so convert them to JPEGs (side note: it seems
			 * that Stack Overflow's old image system (imgur) converted the PNGs
			 * to JPEGs automatically).
			 */
			var jpegImage = convertToJpeg(response.getUrl());
			imageUrl = uploadImage(bot, jpegImage);
		} else {
			if (response.getUrl() != null) {
				imageUrl = uploadImageFromUrl(bot, response.getUrl());
			} else {
				imageUrl = uploadImage(bot, response.getData());
			}
		}

		var messagesToPost = new ArrayList<String>();
		if (response.getRevisedPrompt() != null && !response.getRevisedPrompt().equals(prompt)) {
			messagesToPost.add("I'm going to use this prompt instead: " + response.getRevisedPrompt());
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

	private String handleStableDiffusion(String model, String inputImage, String prompt, IBot bot) throws IOException {
		//@formatter:off
		var builder = new StableImageDiffusionRequest.Builder()
			.model(model)
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

					/*
					 * Stable Diffusion doesn't support GIF.
					 */
					if (contentType.startsWith("image/gif")) {
						image = ImageUtils.convertToPng(image);
						if (image == null) {
							throw new IllegalArgumentException("GIF image couldn't be converted to PNG.");
						}
					}
				}
			}
			builder.image(image, contentType, 0.5);
		}

		var response = stabilityAIClient.generateImage(builder.build());

		return uploadImage(bot, response.getImage());
	}

	/**
	 * @see "https://stackoverflow.com/q/17108234/13379"
	 */
	private byte[] convertToJpeg(String url) throws IOException {
		var image = downloadImage(url);

		/*
		 * If the image has an alpha channel, an exception is thrown when it
		 * tries to write the image as a JPEG.
		 * 
		 * javax.imageio.IIOException: Bogus input colorspace
		 */
		image = ImageUtils.removeAlphaChannel(image);

		var jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		var jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(0.9f);

		try (var out = new ByteArrayOutputStream()) {
			jpgWriter.setOutput(ImageIO.createImageOutputStream(out));
			jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
			return out.toByteArray();
		} finally {
			jpgWriter.dispose();
		}
	}

	private BufferedImage downloadImage(String url) throws IOException {
		BufferedImage image;
		try (var client = HttpFactory.connect().getClient()) {
			var getRequest = new HttpGet(url);
			try (var response = client.execute(getRequest)) {
				try (var in = response.getEntity().getContent()) {
					image = ImageIO.read(in);
				}
			}
		}

		if (image == null) {
			throw new IOException("Cannot read image data: " + url);
		}

		return image;
	}

	static ImagineCommandParameters parseContent(String content) {
		content = content.trim();
		if (content.isEmpty()) {
			return null;
		}

		var split = content.split("\\s+", 3);
		var token1 = split[0];
		var token2 = (split.length > 1) ? split[1] : null;
		var rest = (split.length > 2) ? split[2] : null;

		String model = null;
		String inputImage = null;
		String prompt = null;
		if (supportedModels.contains(token1.toLowerCase())) {
			model = token1.toLowerCase();
			if (token2 != null) {
				if (isUrl(token2)) {
					inputImage = token2;
					prompt = rest;
				} else {
					prompt = token2;
					if (rest != null) {
						prompt += " " + rest;
					}
				}
			}
		} else if (isUrl(token1)) {
			inputImage = token1;
			if (token2 != null) {
				prompt = token2;
				if (rest != null) {
					prompt += " " + rest;
				}
			}
		} else {
			prompt = content;
		}

		return new ImagineCommandParameters(model, inputImage, prompt);
	}

	private static boolean isUrl(String url) {
		return url.matches("^https?://.*");
	}

	record ImagineCommandParameters(String model, String inputImage, String prompt) {
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
}
