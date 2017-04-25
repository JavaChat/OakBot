package oakbot.command.urban;

import static oakbot.command.Command.reply;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;

/**
 * Gets word definitions from urbandictionary.com
 * @author Michael Angstadt
 */
public class UrbanCommand implements Command {
	private static final Logger logger = Logger.getLogger(UrbanCommand.class.getName());

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public String name() {
		return "urban";
	}

	@Override
	public String description() {
		return "Retrieves definitions from urbandictionary.com";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append("Retrieves definitions from urbandictionary.com").nl()
			.append("Usage: ").append(trigger).append(name()).append(" WORD [DEFINITION_NUM=1]").nl()
			.append("Examples:").nl()
			.append(trigger).append(name()).append(" brah").nl()
			.append(trigger).append(name()).append(" wut 2")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
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
		try {
			URIBuilder b = new URIBuilder("http://api.urbandictionary.com/v0/define");
			b.addParameter("term", word);
			String url = b.toString();

			response = mapper.readValue(get(url), UrbanResponse.class);
		} catch (IOException | URISyntaxException e) {
			logger.log(Level.SEVERE, "Problem getting word from Urban Dictionary.", e);

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
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
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append(urbanWord.getWord())
				.append(" (").append(urbanWord.getPermalink()).append("):").nl()
				.append(definition)
			, SplitStrategy.WORD);
			//@formatter:on
		}

		definition = encodeLinks(definition);

		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(chatCommand)
			.link(new ChatBuilder().bold().code(urbanWord.getWord()).bold().toString(), urbanWord.getPermalink())
			.append(": ")
			.append(definition)
		, SplitStrategy.WORD);
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

	/**
	 * Makes an HTTP GET request to the given URL.
	 * @param url the URL
	 * @return the response body
	 * @throws IOException
	 */
	InputStream get(String url) throws IOException {
		URL urlObj = new URL(url);
		return urlObj.openStream();
	}
}