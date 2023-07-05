package oakbot.task;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsoup.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import oakbot.listener.ChatGPTListener;

/**
 * Sends a request to the ChatGPT API.
 * @author Michael Angstadt
 */
public class ChatGPTRequest {
	private static final Logger logger = Logger.getLogger(ChatGPTListener.class.getName());

	private final ObjectMapper mapper = new ObjectMapper();
	private final ObjectNode requestRoot;
	private final ArrayNode messagesArray;

	private final String apiKey;

	/**
	 * @param apiKey the API key
	 * @param prompt the prompt to define the bot's personality (e.g. "You are a
	 * helpful assistant")
	 * @param maxTokens defines how long ChatGPT's response will be (one word is
	 * about 0.75 tokens)
	 */
	public ChatGPTRequest(String apiKey, String prompt, int maxTokens) {
		this.apiKey = apiKey;

		requestRoot = mapper.createObjectNode();
		requestRoot.put("model", "gpt-3.5-turbo");
		requestRoot.put("max_tokens", maxTokens);
		requestRoot.put("temperature", 1.0);

		messagesArray = mapper.createArrayNode();
		ObjectNode messageObj = messagesArray.addObject();
		messageObj.put("role", "system");
		messageObj.put("content", prompt);
		requestRoot.set("messages", messagesArray);

		//ArrayNode stopArray = mapper.createArrayNode();
		//stopArray.add(".");
		//requestRoot.set("stop", stopArray);
	}

	public void addHumanMessage(String message) {
		ObjectNode messageObj = messagesArray.addObject();
		messageObj.put("role", "user");
		messageObj.put("content", message);
	}

	public void addBotMessage(String message) {
		ObjectNode messageObj = messagesArray.addObject();
		messageObj.put("role", "assistant");
		messageObj.put("content", message);
	}

	public String send(CloseableHttpClient client) throws IOException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Sending request to ChatGPT: " + jsonToStringPrettyPrint(requestRoot));
		}

		JsonNode responseBody = null;
		try {
			HttpPost request = new HttpPost("https://api.openai.com/v1/chat/completions");
			request.setHeader("Authorization", "Bearer " + apiKey);
			request.setEntity(new StringEntity(jsonToString(requestRoot), ContentType.APPLICATION_JSON));

			try (CloseableHttpResponse response = client.execute(request)) {
				try (InputStream in = response.getEntity().getContent()) {
					responseBody = mapper.readTree(in);
				}
			}
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Response from ChatGPT: " + jsonToStringPrettyPrint(responseBody));
			}

			String completionMessage;
			try {
				completionMessage = responseBody.get("choices").get(0).get("message").get("content").asText();
			} catch (NullPointerException e) {
				throw new IOException(e);
			}

			return completionMessage;
		} catch (IOException e) {
			String requestBodyStr = jsonToStringPrettyPrint(requestRoot);
			String responseBodyStr = (responseBody == null) ? null : jsonToStringPrettyPrint(responseBody);
			throw new IOException("Problem communicating with ChatGPT API.\nRequest: " + requestBodyStr + "\nResponse: " + responseBodyStr, e);
		}
	}

	private String jsonToString(JsonNode node) {
		ObjectWriter writer = mapper.writer();
		try {
			return writer.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}

	private String jsonToStringPrettyPrint(JsonNode node) {
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
		try {
			return writer.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}
}
