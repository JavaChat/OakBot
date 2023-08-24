package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;

/**
 * Generates images using OpenAI's DALL·E.
 * @author Michael Angstadt
 * @see "https://platform.openai.com/docs/api-reference/images"
 */
public class ImageCommand implements Command {
	private static final Logger logger = Logger.getLogger(ImageCommand.class.getName());

	private final String apiKey;

	/**
	 * @param apiKey the OpenAI API key
	 */
	public ImageCommand(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public String name() {
		return "image";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Creates images using OpenAI's DALL·E.")
			.example("a cute Java programmer", "Generates an image using the given prompt.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String prompt = chatCommand.getContent().trim();
		if (prompt.isEmpty()) {
			return reply("Image prompt is missing.", chatCommand);
		}

		try {
			String url = sendRequest(prompt) + "&a=.png";
			return create(new PostMessage(url).bypassFilters(true));
		} catch (ChatGPTException e) {
			return reply(new ChatBuilder().code().append("ERROR BEEP BOOP: ").append(e.getMessage()).code(), chatCommand);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem communicating with OpenAI.", e);
			return reply("Problem communicating with OpenAI.", chatCommand);
		}
	}

	private String sendRequest(String prompt) throws IOException, ChatGPTException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode requestRoot = mapper.createObjectNode();
		requestRoot.put("prompt", prompt);
		requestRoot.put("size", "256x256");

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Sending request to ChatGPT: " + JsonUtils.prettyPrint(requestRoot));
		}

		HttpPost request = new HttpPost("https://api.openai.com/v1/images/generations");
		request.setHeader("Authorization", "Bearer " + apiKey);
		request.setEntity(new StringEntity(JsonUtils.toString(requestRoot), ContentType.APPLICATION_JSON));

		JsonNode responseBody = null;
		int responseStatusCode = 0;
		try (CloseableHttpClient client = HttpFactory.connect().getClient()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				try (InputStream in = response.getEntity().getContent()) {
					responseBody = mapper.readTree(in);
				}
			}
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Response from ChatGPT: " + JsonUtils.prettyPrint(responseBody));
			}

			JsonNode error = responseBody.get("error");
			if (error != null) {
				JsonNode node = error.get("message");
				String message = (node == null) ? null : node.asText();

				node = error.get("type");
				String type = (node == null) ? null : node.asText();

				node = error.get("param");
				String param = (node == null) ? null : node.asText();

				node = error.get("code");
				String code = (node == null) ? null : node.asText();

				throw new ChatGPTException(message, type, param, code);
			}

			try {
				return responseBody.get("data").get(0).get("url").asText();
			} catch (NullPointerException e) {
				throw new IOException("JSON response not structured as expected.", e);
			}
		} catch (IOException e) {
			String requestBodyStr = JsonUtils.prettyPrint(requestRoot);

			StringBuilder sb = new StringBuilder();
			sb.append("Problem communicating with ChatGPT API.");
			sb.append("\nRequest: ").append(requestBodyStr);

			if (responseBody != null) {
				String responseBodyStr = JsonUtils.prettyPrint(responseBody);
				sb.append("\nResponse (HTTP ").append(responseStatusCode).append("): ").append(responseBodyStr);
			}

			throw new IOException(sb.toString(), e);
		}
	}
}
