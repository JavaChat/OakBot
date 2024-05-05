package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;
import static oakbot.util.StringUtils.plural;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;

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
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;

/**
 * Generates images using various AI image models.
 * @author Michael Angstadt
 */
public class ImagineCommand implements Command {
	private static final Logger logger = Logger.getLogger(ImagineCommand.class.getName());

	static final String MODEL_DALLE_2 = "dall-e-2";
	static final String MODEL_DALLE_3 = "dall-e-3";
	static final String MODEL_STABLE_IMAGE_CORE = "si-core";
	static final String MODEL_STABLE_DIFFUSION = "sd3";
	static final String MODEL_STABLE_DIFFUSION_TURBO = "sd3-turbo";
	static final List<String> supportedModels = List.of(MODEL_DALLE_2, MODEL_DALLE_3, MODEL_STABLE_IMAGE_CORE, MODEL_STABLE_DIFFUSION, MODEL_STABLE_DIFFUSION_TURBO);

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
	public ImagineCommand(OpenAIClient openAIClient, StabilityAIClient stabilityAIClient, int requestsPer24Hours) {
		this.openAIClient = openAIClient;
		this.stabilityAIClient = stabilityAIClient;
		this.requestsPer24Hours = requestsPer24Hours;
		usageQuota = (requestsPer24Hours > 0) ? new UsageQuota(Duration.ofDays(1), requestsPer24Hours) : UsageQuota.allowAll();
	}

	@Override
	public String name() {
		return "imagine";
	}

	@Override
	public HelpDoc help() {
		var requestLimit = (requestsPer24Hours > 0) ? "Users can make " + requestsPer24Hours + " requests per day. " : "";

		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Creates images using OpenAI's DALL·E and Stability.ai.")
			.detail(requestLimit + "Syntax: [model] [input image URL] [prompt]. Supported models: " + supportedModels)
			.example("a cute Java programmer", "Generates an image using DALL·E 3.")
			.example("https://example.com/image.png", "Generates a variation of the given image using DALL·E 2. Image must be a PNG, JPEG, or GIF.")
			.example("https://example.com/sheep.png A sheep wearing sunglasses", "Modifies an image using Stable Diffusion 3.0.")
			.example("si-core A funny cat", "Include the model ID at the beginning of the message to define which model to use.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		/*
		 * Check usage quota.
		 */
		var userId = chatCommand.getMessage().getUserId();
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
			if (MODEL_DALLE_2.equals(model) || MODEL_DALLE_3.equals(model)) {
				messagesToPost = handleDallE(model, inputImage, prompt, bot);
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
			var isAdmin = bot.getAdminUsers().contains(userId);
			if (!isAdmin) {
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
			return post(new ChatBuilder().reply(chatCommand).code().append("ERROR BEEP BOOP: ").append(e.getMessage()).code());
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Network error.");
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

	private List<String> handleDallE(String model, String inputImageUrl, String prompt, IBot bot) throws OpenAIException, IOException, URISyntaxException {
		CreateImageResponse response;
		if (inputImageUrl == null) {
			var lowestResolutionSupportedByModel = MODEL_DALLE_2.equals(model) ? "256x256" : "1024x1024";
			response = openAIClient.createImage(model, lowestResolutionSupportedByModel, prompt);
		} else {
			response = openAIClient.createImageVariation(inputImageUrl, "256x256");
		}

		String imageUrl;
		if (MODEL_DALLE_3.equals(model)) {
			/*
			 * 5/1/2024: StackOverflow's new image hosting system has a file
			 * size limit of 2 MiB. The PNGs that Dall-E 3 generates are usually
			 * larger than that, so convert them to JPEGs (side note: it seems
			 * that the old system (imgur) converted the PNGs to JPEGs
			 * automatically).
			 */
			var jpegImage = convertToJpeg(response.getUrl());
			imageUrl = uploadImage(bot, jpegImage);
		} else {
			imageUrl = uploadImageFromUrl(bot, response.getUrl());
		}

		var messagesToPost = new ArrayList<String>();
		if (response.getRevisedPrompt() != null) {
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
						image = convertToPng(image);
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
		image = removeAlphaChannel(image);

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

	/**
	 * Removes the alpha channel from the image, if present.
	 * @param image the image
	 * @return the image with alpha channel removed
	 * @see "https://stackoverflow.com/a/72135983/13379"
	 */
	private BufferedImage removeAlphaChannel(BufferedImage image) {
		if (!image.getColorModel().hasAlpha()) {
			return image;
		}

		var target = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

		var g = target.createGraphics();
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return target;
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
			logger.log(Level.SEVERE, e, () -> "Problem uploading image to imgur.");

			/*
			 * Add a fake parameter onto the end of the URL so SO Chat one-boxes
			 * the image. SO Chat will only one-box an image if the URL ends in
			 * an image extension.
			 */
			return new URIBuilder(imageUrl).addParameter("a", ".png").toString();
		}
	}

	private String uploadImage(IBot bot, byte[] imageData) throws IOException {
		return bot.uploadImage(imageData);
	}
}
