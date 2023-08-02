package oakbot.command.urban;

import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;

import com.github.mangstadt.sochat4j.SplitStrategy;
import com.github.mangstadt.sochat4j.util.Http;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;

/**
 * Gets word definitions from urbandictionary.com
 * @author Michael Angstadt
 */
public class UrbanCommand implements Command {
	private static final Logger logger = Logger.getLogger(UrbanCommand.class.getName());

	@Override
	public String name() {
		return "urban";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Retrieves definitions from urbandictionary.com.")
			.example("brah", "Displays the top definition for \"brah\".")
			.example("brah 2", "Displays the second most popular definition for \"brah\".")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			return reply("You have to type a word to see its definition... -_-", chatCommand);
		}

		//parse the user's input
		String word;
		int definitionToDisplay;
		{
			int lastSpace = content.lastIndexOf(' ');
			if (lastSpace < 0) {
				word = content;
				definitionToDisplay = 1;
			} else {
				String afterLastSpace = content.substring(lastSpace + 1);
				try {
					definitionToDisplay = Integer.parseInt(afterLastSpace);
					if (definitionToDisplay < 1) {
						definitionToDisplay = 1;
					}
					word = content.substring(0, lastSpace);
				} catch (NumberFormatException e) {
					word = content;
					definitionToDisplay = 1;
				}
			}
		}

		UrbanResponse response;
		try (Http http = HttpFactory.connect()) {
			String url = url(word);
			response = http.get(url).getBodyAsJson(UrbanResponse.class);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem getting word from Urban Dictionary.", e);

			//@formatter:off
			return post(new ChatBuilder()
				.reply(chatCommand)
				.append("Sorry, an unexpected error occurred: ")
				.code(e.getMessage())
			);
			//@formatter:on
		}

		List<UrbanDefinition> words = response.getDefinitions();
		if (words == null || words.isEmpty()) {
			return reply("No definition found.", chatCommand);
		}

		if (definitionToDisplay > words.size()) {
			definitionToDisplay = words.size();
		}

		UrbanDefinition urbanWord = words.get(definitionToDisplay - 1);
		String definition = urbanWord.getDefinition();
		if (containsNewlines(definition)) {
			//do not use markup if the definition contains newlines
			definition = removeLinks(definition);

			//@formatter:off
			return ChatActions.create(
				new PostMessage(
					new ChatBuilder()
					.reply(chatCommand)
					.append(urbanWord.getWord())
					.append(" (").append(urbanWord.getPermalink()).append("):").nl()
					.append(definition)
				)
				.splitStrategy(SplitStrategy.WORD)
			);
			//@formatter:on
		}

		definition = encodeLinks(definition);

		//@formatter:off
		return ChatActions.create(
			new PostMessage(
				new ChatBuilder()
				.reply(chatCommand)
				.link(new ChatBuilder().bold().code(urbanWord.getWord()).bold().toString(), urbanWord.getPermalink())
				.append(": ")
				.append(definition)
			)
			.splitStrategy(SplitStrategy.WORD)
		);
		//@formatter:on
	}

	private String url(String word) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("http")
			.setHost("api.urbandictionary.com")
			.setPath("/v0/define")
			.setParameter("term", word)
		.toString();
		//@formatter:on
	}

	private static boolean containsNewlines(String definition) {
		return definition.contains("\n") || definition.contains("\r");
	}

	private static String removeLinks(String definition) {
		return definition.replaceAll("[\\[\\]]", "");
	}

	private static String encodeLinks(String definition) {
		Pattern p = Pattern.compile("\\[(.*?)\\]");
		Matcher m = p.matcher(definition);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String word = m.group(1);
			try {
				URIBuilder b = new URIBuilder("http://www.urbandictionary.com/define.php");
				b.addParameter("term", word);
				String url = b.toString();

				ChatBuilder cb = new ChatBuilder();
				cb.link(word, url);
				m.appendReplacement(sb, cb.toString());
			} catch (URISyntaxException e) {
				//should never be thrown since the URL string is hard-coded, but just incase...
				//remove the link
				m.appendReplacement(sb, word);
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}
}