package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.HttpFactory;
import oakbot.util.Rng;

/**
 * Displays reaction gifs of human emotions.
 * @author Michael Angstadt
 */
public class ReactCommand implements Command {
	private static final Logger logger = LoggerFactory.getLogger(ReactCommand.class);

	private final URIBuilder uriBuilder;

	public ReactCommand(String key) {
		//@formatter:off
		uriBuilder = new URIBuilder()
			.setScheme("http")
			.setHost("replygif.net")
			.setPath("/api/gifs")
			.setParameter("tag-operator", "and");
		//@formatter:on

		if (key != null && !key.isEmpty()) {
			uriBuilder.setParameter("api-key", key);
		}
	}

	@Override
	public String name() {
		return "react";
	}

	@Override
	public List<String> aliases() {
		return List.of("reaction");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays a reaction GIF.")
			.detail("Images from replygif.net.")
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

		var url = url(content);

		try (var http = HttpFactory.connect()) {
			var node = http.get(url).getBodyAsJson();
			if (node.isEmpty()) {
				return reply("Unknown human emotion. Please visit http://replygif.net/t for a list of emotions.", chatCommand);
			}

			var index = Rng.next(node.size());
			var imageUrl = node.get(index).get("file").asText();

			//@formatter:off
			return ChatActions.create(
				new PostMessage(imageUrl).bypassFilters(true)
			);
			//@formatter:on
		} catch (Exception e) {
			logger.atError().setCause(e).log(() -> "Problem querying reaction API.");

			return reply("Sorry, an error occurred >.> : " + e.getMessage(), chatCommand);
		}
	}

	private String url(String content) {
		return uriBuilder.setParameter("tag", content).toString();
	}
}