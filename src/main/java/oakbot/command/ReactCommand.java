package oakbot.command;

import static oakbot.command.Command.reply;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatBuilder;

/**
 * Displays reaction gifs of human emotions.
 * @author Michael Angstadt
 */
public class ReactCommand implements Command {
	private static final Logger logger = Logger.getLogger(ReactCommand.class.getName());

	private final ObjectMapper mapper = new ObjectMapper();
	private final URIBuilder uriBuilder;

	public ReactCommand(String key) {
		uriBuilder = new URIBuilder(URI.create("http://replygif.net/api/gifs"));
		uriBuilder.addParameter("tag-operator", "and");
		if (key != null) {
			uriBuilder.addParameter("api-key", key);
		}
	}

	@Override
	public String name() {
		return "react";
	}

	@Override
	public List<String> aliases() {
		return Arrays.asList("reaction");
	}

	@Override
	public String description() {
		return "Displays a reaction gif.";
	}

	@Override
	public String helpText(String trigger) {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			return reply("Please specify a human emotion.", chatCommand);
		}

		uriBuilder.setParameter("tag", content);

		try {
			JsonNode node = get(uriBuilder.build());
			if (node.size() == 0) {
				return reply("Unknown human emotion. Please visit http://replygif.net/t for a list of emotions.", chatCommand);
			}

			int index = random.nextInt(node.size());
			String imageUrl = node.get(index).get("file").asText();
			return new ChatResponse(imageUrl);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem querying reaction API.", e);

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("Sorry, an error occurred >.> : ").append(e.getMessage())
			);
			//@formatter:on
		}
	}

	/**
	 * Makes an HTTP GET request to the given URL.
	 * @param uri the URL
	 * @return the response body
	 * @throws IOException
	 */
	JsonNode get(URI uri) throws IOException {
		HttpGet request = new HttpGet(uri);
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				try (InputStream in = response.getEntity().getContent()) {
					return mapper.readTree(in);
				}
			}
		}
	}
}