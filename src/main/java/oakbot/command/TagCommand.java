package oakbot.command;

import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.github.mangstadt.sochat4j.SplitStrategy;
import com.github.mangstadt.sochat4j.util.Http;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;

/**
 * Displays StackOverflow tag descriptions (can sort of act like a Computer
 * Science urban dictionary).
 * @author Michael Angstadt
 */
public class TagCommand implements Command {
	private static final Logger logger = Logger.getLogger(TagCommand.class.getName());

	@Override
	public String name() {
		return "tag";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays the description of a StackOverflow tag.")
			.example("mvc", "Displays the description of the \"mvc\" tag.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			return reply("Please specify the tag name (e.g. \"java\").", chatCommand);
		}

		String tag = content.toLowerCase().replace(' ', '-');
		String url = url(tag);

		Document document;
		try (Http http = HttpFactory.connect()) {
			document = http.get(url).getBodyAsHtml();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error getting tag description.", e);

			//@formatter:off
			return post(new ChatBuilder()
				.reply(chatCommand)
				.append("An error occurred retrieving the tag description: ")
				.code(e.getMessage())
			);
			//@formatter:on
		}

		Element element = document.getElementById("wiki-excerpt");
		if (element == null) {
			return reply("Tag not found. :(", chatCommand);
		}

		String definition = element.text();

		//@formatter:off
		return ChatActions.create(
			new PostMessage(new ChatBuilder()
				.reply(chatCommand)
				.tag(tag)
				.append(' ').append(definition)
			)
			.splitStrategy(SplitStrategy.WORD)
		);
		//@formatter:on
	}

	private String url(String tag) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("http")
			.setHost("stackoverflow.com")
			.setPathSegments("tags", tag, "info")
		.toString();	
		//@formatter:on
	}
}
