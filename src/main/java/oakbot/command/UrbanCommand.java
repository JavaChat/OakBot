package oakbot.command;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Gets word definitions from urbandictionary.com
 * @author Michael Angstadt
 */
public class UrbanCommand implements Command {
	private static final Logger logger = Logger.getLogger(UrbanCommand.class.getName());

	private final HttpClient client;

	public UrbanCommand(HttpClient client) {
		this.client = client;
	}

	@Override
	public String name() {
		return "urban";
	}

	@Override
	public String description() {
		ChatBuilder cb = new ChatBuilder();
		cb.append("Retrieves definitions from ");
		cb.link("urbandictionary.com", "http://www.urbandictionary.com");
		cb.append(".");
		return cb.toString();
	}

	@Override
	public String helpText() {
		ChatBuilder cb = new ChatBuilder(description());
		cb.code().append("  =").append(name()).append(" word").code();
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

		UrbanWord urbanWord;
		try {
			urbanWord = getDefinition(word);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem getting word from Urban Dictionary.", e);
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("Sorry, there was an error contacting ")
				.link("urbandictionary.com", "http://www.ubrandictionary.com")
				.append("... >.>")
				.toString()
			);
			//@formatter:on
		}

		if (urbanWord == null) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("No definition found.")
				.toString()
			);
			//@formatter:on
		}

		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(message)
			.link(new ChatBuilder().bold().code(urbanWord.word).bold().toString(), urbanWord.permalink)
			.append(": ")
			.append(urbanWord.definition)
			.toString()
		);
		//@formatter:on
	}

	private UrbanWord getDefinition(String word) throws IOException {
		String url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(word, "UTF-8");
		HttpGet request = new HttpGet(url);
		HttpResponse response = client.execute(request);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode responseNode = mapper.readTree(response.getEntity().getContent());
		JsonNode listNode = responseNode.get("list");
		if (listNode == null) {
			return null;
		}

		JsonNode definitionNode = listNode.get(0);
		if (definitionNode == null) {
			return null;
		}

		String theWord = asText(definitionNode.get("word"));
		String permalink = asText(definitionNode.get("permalink"));
		String definition = asText(definitionNode.get("definition"));
		return new UrbanWord(theWord, permalink, definition);
	}

	private String asText(JsonNode node) {
		return (node == null) ? null : node.asText();
	}

	private static class UrbanWord {
		private final String word;
		private final String permalink;
		private final String definition;

		public UrbanWord(String word, String permalink, String definition) {
			this.word = word;
			this.permalink = permalink;
			this.definition = definition;
		}
	}
}