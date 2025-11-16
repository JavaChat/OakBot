package oakbot.command.urban;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;

/**
 * Gets word definitions from urbandictionary.com
 * @author Michael Angstadt
 */
public class UrbanCommand implements Command {
	private static final Logger logger = LoggerFactory.getLogger(UrbanCommand.class);

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
		var content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			return reply("You have to type a word to see its definition... -_-", chatCommand);
		}

		var input = parseInput(content);
		var url = apiUrl(input.word);

		UrbanResponse response;
		try (var http = HttpFactory.connect()) {
			response = http.get(url).getBodyAsJson(UrbanResponse.class);
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem getting word from Urban Dictionary.");
			return error("Sorry, an unexpected error occurred: ", e, chatCommand);
		}

		var words = response.getDefinitions();
		if (words == null || words.isEmpty()) {
			return reply("No definition found.", chatCommand);
		}

		var definitionToDisplay = Math.min(input.definitionToDisplay, words.size());
		var urbanWord = words.get(definitionToDisplay - 1);
		var definition = urbanWord.getDefinition();
		if (containsNewlines(definition)) {
			//do not use markup if the definition contains newlines
			definition = removeLinks(definition);

			//@formatter:off
			return reply(new ChatBuilder()
				.append(urbanWord.getWord())
				.append(" (").append(urbanWord.getPermalink()).append("):").nl()
				.append(definition), chatCommand, SplitStrategy.WORD);
			//@formatter:on
		}

		definition = encodeLinks(definition);

		//@formatter:off
		return reply(new ChatBuilder()
			.link(new ChatBuilder().bold().code(urbanWord.getWord()).bold().toString(), urbanWord.getPermalink())
			.append(": ")
			.append(definition), chatCommand, SplitStrategy.WORD);
		//@formatter:on
	}

	private Input parseInput(String content) {
		var lastSpace = content.lastIndexOf(' ');
		if (lastSpace < 0) {
			return new Input(content, 1);
		}

		var afterLastSpace = content.substring(lastSpace + 1);
		int definitionToDisplay;
		try {
			definitionToDisplay = Integer.parseInt(afterLastSpace);
		} catch (NumberFormatException e) {
			return new Input(content, 1);
		}

		if (definitionToDisplay < 1) {
			definitionToDisplay = 1;
		}
		var word = content.substring(0, lastSpace);
		return new Input(word, definitionToDisplay);
	}

	private record Input(String word, int definitionToDisplay) {
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
		var p = Pattern.compile("\\[(.*?)\\]");
		var m = p.matcher(definition);
		var sb = new StringBuilder();

		while (m.find()) {
			var word = m.group(1);
			var url = websiteUrl(word);

			var cb = new ChatBuilder();
			cb.link(word, url);
			m.appendReplacement(sb, cb.toString());
		}
		m.appendTail(sb);

		return sb.toString();
	}
}