package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Generates images using OpenAI's DALL·E.
 * @author Michael Angstadt
 * @see "https://platform.openai.com/docs/api-reference/images"
 */
public class ImagineCommand implements Command {
	private static final Logger logger = Logger.getLogger(ImagineCommand.class.getName());

	private final OpenAIClient openAIClient;
	private final String imageGenerationModel;
	private final String imageGenerationSize;
	private final UsageQuota usageQuota;

	/**
	 * @param apiKey the OpenAI API key
	 * @param imageGenerationModel the model to use for generating images (e.g.
	 * "dall-e-2")
	 * @param imageGenerationSize the size of the images to generate (e.g.
	 * "256x256")
	 * @param requestsPer24Hours requests allowed per user per 24 hours, or
	 * {@literal <= 0} for no limit
	 */
	public ImagineCommand(OpenAIClient openAIClient, String imageGenerationModel, String imageGenerationSize, int requestsPer24Hours) {
		this.openAIClient = openAIClient;
		this.imageGenerationModel = imageGenerationModel;
		this.imageGenerationSize = imageGenerationSize;
		usageQuota = (requestsPer24Hours > 0) ? new UsageQuota(Duration.ofDays(1), requestsPer24Hours) : UsageQuota.allowAll();
	}

	@Override
	public String name() {
		return "imagine";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Creates images using OpenAI's DALL·E.")
			.example("a cute Java programmer", "Generates an image using the given prompt.")
			.example("https://example.com/image.png", "Generates an image variation using the given image as input. Image must be a PNG or JPEG.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String prompt = chatCommand.getContent().trim();
		if (prompt.isEmpty()) {
			return reply("Image prompt or URI is missing.", chatCommand);
		}

		int userId = chatCommand.getMessage().getUserId();
		Duration timeUntilNextRequest = usageQuota.getTimeUntilUserCanMakeRequest(userId);
		if (!timeUntilNextRequest.isZero()) {
			long hours = timeUntilNextRequest.toHours() + 1;
			return reply("Bad human! You are over quota and can't make any more requests right now. Try again in " + hours + " " + plural("hour", hours) + ".", chatCommand);
		}

		try {
			boolean isUri = prompt.matches("^https?://.*");
			String openAiImageUrl = isUri ? openAIClient.createImageVariation(prompt) : openAIClient.createImage(imageGenerationModel, imageGenerationSize, prompt);

			boolean isAdmin = bot.getAdminUsers().contains(userId);
			if (!isAdmin) {
				usageQuota.logRequest(userId);
			}

			String urlToPost = getUrlToPost(bot, openAiImageUrl);

			return create(new PostMessage(urlToPost).bypassFilters(true));
		} catch (IllegalArgumentException | URISyntaxException | OpenAIException e) {
			return post(new ChatBuilder().reply(chatCommand).code().append("ERROR BEEP BOOP: ").append(e.getMessage()).code());
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem communicating with OpenAI.");
			return error("Problem communicating with OpenAI: ", e, chatCommand);
		}
	}

	private String getUrlToPost(IBot bot, String openAiImageUrl) throws URISyntaxException {
		try {
			return bot.uploadImage(openAiImageUrl);
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem uploading image to imgur.");

			/*
			 * Add a fake parameter onto the end of the URL so SO Chat one-boxes
			 * the image. SO Chat will only one-box an image if the URL ends in
			 * an image extension.
			 */
			return new URIBuilder(openAiImageUrl).addParameter("a", ".png").toString();
		}
	}

	/**
	 * Determines if a word should be plural.
	 * @param word the singular version of the word
	 * @param number the number
	 * @return the plural or singular version of the word, depending on the
	 * provided number
	 */
	private static String plural(String word, long number) {
		if (number == 1) {
			return word;
		}

		return word + (word.endsWith("s") ? "es" : "s");
	}
}
