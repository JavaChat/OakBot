package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import oakbot.util.Now;

/**
 * Generates images using OpenAI's DALL·E.
 * @author Michael Angstadt
 * @see "https://platform.openai.com/docs/api-reference/images"
 */
public class ImagineCommand implements Command {
	private static final Logger logger = Logger.getLogger(ImagineCommand.class.getName());

	private final OpenAIClient openAIClient;
	private final int requestsPer24Hours;

	private final Map<Integer, List<Instant>> requestTimesByUser = new HashMap<>();

	/**
	 * @param apiKey the OpenAI API key
	 * @param requestsPer24Hours requests allowed per user per 24 hours, or
	 * {@literal <= 0} for no limit
	 */
	public ImagineCommand(OpenAIClient openAIClient, int requestsPer24Hours) {
		this.openAIClient = openAIClient;
		this.requestsPer24Hours = requestsPer24Hours;
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

		int userId = chatCommand.getMessage().getUserId();
		if (isUserOverQuota(userId)) {
			return reply("Bad human! You are over quota and can't make any more requests right now.", chatCommand);
		}

		try {
			String url = isUri(prompt) ? openAIClient.createImageVariation(prompt) : openAIClient.createImage(prompt);

			logQuota(userId);

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

	private boolean isUserOverQuota(int userId) {
		if (requestsPer24Hours <= 0) {
			return false;
		}

		Instant now = Now.instant();
		List<Instant> times = getRequestTimes(userId);
		times.removeIf(instant -> Duration.between(instant, now).toHours() >= 24);
		return times.size() >= requestsPer24Hours;
	}

	private void logQuota(int userId) {
		if (requestsPer24Hours <= 0) {
			return;
		}

		List<Instant> times = getRequestTimes(userId);
		times.add(Now.instant());
	}

	private List<Instant> getRequestTimes(int userId) {
		return requestTimesByUser.computeIfAbsent(userId, key -> new ArrayList<Instant>());
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
