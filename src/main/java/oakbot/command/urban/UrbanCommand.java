package oakbot.command.urban;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;

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
	public String helpText() {
		ChatBuilder cb = new ChatBuilder();
		cb.append("Retrieves definitions from ");
		cb.link("urbandictionary.com", "http://www.urbandictionary.com");
		cb.append(".  ");
		cb.code().append("=").append(name()).append(" word").code();
		return cb.toString();
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		String word = message.getContent();
		if (word.isEmpty()) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("You have to type a word to see its definition... -_-")
				.toString()
			);
			//@formatter:on
		}

		UrbanResponse response;
		try {
			String url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(word, "UTF-8");
			response = mapper.readValue(get(url), UrbanResponse.class);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem getting word from Urban Dictionary.", e);
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("Sorry, an unexpected error occurred contacting ")
				.link("urbandictionary.com", "http://www.ubrandictionary.com")
				.append("... >.>")
				.toString()
			);
			//@formatter:on
		}

		List<UrbanDefinition> words = response.getDefinitions();
		if (words == null || words.isEmpty()) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("No definition found.")
				.toString()
			);
			//@formatter:on
		}

		UrbanDefinition urbanWord = words.get(0);
		String definition = urbanWord.getDefinition();
		if (definition.contains("\n") || definition.contains("\r")) {
			//do not use markup if the definition contains newlines
			definition = removeLinks(definition);

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append(urbanWord.getWord())
				.append(" (").append(urbanWord.getPermalink()).append("):\n")
				.append(definition)
				.toString()
			, SplitStrategy.WORD);
			//@formatter:on
		}

		definition = encodeLinks(definition);

		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(message)
			.link(new ChatBuilder().bold().code(urbanWord.getWord()).bold().toString(), urbanWord.getPermalink())
			.append(": ")
			.append(definition)
			.toString()
		, SplitStrategy.WORD);
		//@formatter:on
	}

	private static String removeLinks(String definition) {
		return definition.replaceAll("[\\[\\]]", "");
	}

	private static String encodeLinks(String definition) {
		Pattern p = Pattern.compile("\\[(.*?)\\]");
		Matcher m = p.matcher(definition);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			ChatBuilder cb = new ChatBuilder();
			try {
				cb.link(m.group(1), "http://www.urbandictionary.com/define.php?term=" + URLEncoder.encode(m.group(1), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			m.appendReplacement(sb, cb.toString());
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
	String get(String url) throws IOException {
		URL urlObj = new URL(url);
		try (Reader reader = new InputStreamReader(urlObj.openStream())) {
			return CharStreams.toString(reader);
		}
	}
}