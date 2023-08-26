package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

	/**
	 * @param apiKey the OpenAI API key
	 */
	public ImagineCommand(OpenAIClient openAIClient) {
		this.openAIClient = openAIClient;
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
			.example("https://example.com/image.png", "Generates an image variation using the given image as input. Image must be a PNG.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String prompt = chatCommand.getContent().trim();
		if (prompt.isEmpty()) {
			return reply("Image prompt or URI is missing.", chatCommand);
		}

		try {
			String url = isUri(prompt) ? openAIClient.createImageVariation(prompt) : openAIClient.createImage(prompt);

			/*
			 * Add a fake parameter onto the end of the URL so SO Chat one-boxes
			 * the image. SO Chat will only one-box an image if the URL ends in
			 * an image extension.
			 */
			URIBuilder urlWithFakeParam = new URIBuilder(url);
			urlWithFakeParam.addParameter("a", ".png");

			return create(new PostMessage(urlWithFakeParam.toString()).bypassFilters(true));
		} catch (URISyntaxException | OpenAIException e) {
			return reply(new ChatBuilder().code().append("ERROR BEEP BOOP: ").append(e.getMessage()).code(), chatCommand);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem communicating with OpenAI.", e);
			return reply(new ChatBuilder().append("Problem communicating with OpenAI: ").code(e.getMessage()), chatCommand);
		}
	}

	private boolean isUri(String s) {
		try {
			URI.create(s);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
