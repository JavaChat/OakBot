package oakbot.command;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.GiphyClient.Rating;
import oakbot.util.ChatBuilder;

/**
 * Displays reaction GIFs of human emotions.
 * @author Michael Angstadt
 */
public class ReactGiphyCommand implements Command {
	private static final Logger logger = LoggerFactory.getLogger(ReactGiphyCommand.class);

	private final GiphyClient giphyClient;

	/**
	 * @param apiKey the GIPHY API key
	 */
	public ReactGiphyCommand(String apiKey) {
		giphyClient = new GiphyClient(apiKey);
	}

	@Override
	public String name() {
		return "react";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays a random GIF.")
			.detail("Images from giphy.com.")
			.example("happy", "Displays a \"happy\" GIF.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			return reply("Please specify a human emotion.", chatCommand);
		}

		try {
			var imageUrl = giphyClient.random(content, Rating.G);
			if (imageUrl == null) {
				return reply("404 human emotion not found.", chatCommand);
			}

			/*
			 * URL must end in a file extension in order for chat to display the
			 * image.
			 */
			var imageUrlForChat = imageUrl + "&.gif";

			//@formatter:off
			var condensedMessage = new ChatBuilder()
				.append("Reaction: ")
				.link(content, imageUrl)
				.append(" (powered by ").link("GIPHY", "https://giphy.com").append(")");

			return ChatActions.create(
				new PostMessage(imageUrlForChat).bypassFilters(true).condensedMessage(condensedMessage)
			);
			//@formatter:on
		} catch (Exception e) {
			logger.atError().setCause(e).log(() -> "Problem querying GIPHY API.");
			return error("Unable to process human emotion: ", e, chatCommand);
		}
	}
}