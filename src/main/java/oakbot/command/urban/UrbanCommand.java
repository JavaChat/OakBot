package oakbot.command.urban;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
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

		Input input = parseInput(content);
		String url = apiUrl(input.word);

		UrbanResponse response;
		try (Http http = HttpFactory.connect()) {
			response = http.get(url).getBodyAsJson(UrbanResponse.class);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem getting word from Urban Dictionary.", e);
			return error("Sorry, an unexpected error occurred: ", e, chatCommand);
		}

		List<UrbanDefinition> words = response.getDefinitions();
		if (words == null || words.isEmpty()) {
			return reply("No definition found.", chatCommand);
		}

		int definitionToDisplay = Math.min(input.definitionToDisplay, words.size());
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

	private Input parseInput(String content) {
		int lastSpace = content.lastIndexOf(' ');
		if (lastSpace < 0) {
			return new Input(content, 1);
		}

		String afterLastSpace = content.substring(lastSpace + 1);
		int definitionToDisplay;
		try {
			definitionToDisplay = Integer.parseInt(afterLastSpace);
		} catch (NumberFormatException e) {
			return new Input(content, 1);
		}

		if (definitionToDisplay < 1) {
			definitionToDisplay = 1;
		}
		String word = content.substring(0, lastSpace);
		return new Input(word, definitionToDisplay);
	}

	private class Input {
		private final String word;
		private final int definitionToDisplay;

		public Input(String word, int definitionToDisplay) {
			this.word = word;
			this.definitionToDisplay = definitionToDisplay;
		}
	}

	private String apiUrl(String word) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("http")
			.setHost("api.urbandictionary.com")
			.setPath("/v0/define")
			.setParameter("term", word)
		.toString();
		//@formatter:on
	}

	private String websiteUrl(String word) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("http")
			.setHost("www.urbandictionary.com")
			.setPath("/define.php")
			.setParameter("term", word)
		.toString();
		//@formatter:on
	}

	private boolean containsNewlines(String definition) {
		return definition.contains("\n") || definition.contains("\r");
	}

	private String removeLinks(String definition) {
		return definition.replaceAll("[\\[\\]]", "");
	}

	private String encodeLinks(String definition) {
		Pattern p = Pattern.compile("\\[(.*?)\\]");
		Matcher m = p.matcher(definition);
		StringBuilder sb = new StringBuilder();

		while (m.find()) {
			String word = m.group(1);
			String url = websiteUrl(word);

			ChatBuilder cb = new ChatBuilder();
			cb.link(word, url);
			m.appendReplacement(sb, cb.toString());
		}
		m.appendTail(sb);

		return sb.toString();
	}
}