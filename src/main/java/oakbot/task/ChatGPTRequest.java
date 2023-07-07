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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Sends a request to the ChatGPT API.
 * @author Michael Angstadt
 */
public class ChatGPTRequest {
	private static final Logger logger = Logger.getLogger(ChatGPTRequest.class.getName());

	private final ObjectMapper mapper = new ObjectMapper();
	private final ObjectNode requestRoot;
	private final ArrayNode messagesArray;

	private final String apiKey;

	/**
	 * @param parameters parameters for connecting to ChatGPT
	 */
	public ChatGPTRequest(ChatGPTParameters parameters) {
		this(parameters.getApiKey(), parameters.getPrompt(), parameters.getMaxTokensForCompletion());
	}

	/**
	 * @param apiKey the API key
	 * @param prompt defines the bot's personality (e.g. "You are a helpful
	 * assistant"). This counts against your usage quota. Each word is about
	 * 1.33 tokens.
	 * @param maxTokensForCompletion places a limit on the length of the
	 * completion (response). If this number is too short, then the completion
	 * will be abruptly truncated. One word is about 1.33 tokens.
	 * @see <a href="https://platform.openai.com/account/api-keys">API keys</a>
	 */
	public ChatGPTRequest(String apiKey, String prompt, int maxTokensForCompletion) {
		this.apiKey = apiKey;

		requestRoot = mapper.createObjectNode();
		requestRoot.put("model", "gpt-3.5-turbo");
		requestRoot.put("max_tokens", maxTokensForCompletion);
		requestRoot.put("temperature", 1.0);

		messagesArray = mapper.createArrayNode();
		requestRoot.set("messages", messagesArray);

		addMessage(prompt, "system");

		//ArrayNode stopArray = mapper.createArrayNode();
		//stopArray.add(".");
		//requestRoot.set("stop", stopArray);
	}

	/**
	 * Adds a message to the request, and marks the message as having been
	 * written by a person.
	 * @param message the message
	 */
	public void addHumanMessage(String message) {
		addMessage(message, "user");
	}

	/**
	 * Adds a message to the request, and marks the message as having been
	 * written by ChatGPT.
	 * @param message the message
	 */
	public void addBotMessage(String message) {
		addMessage(message, "assistant");
	}

	/**
	 * Adds a message to the request.
	 * @param message the message
	 * @param role the role
	 */
	private void addMessage(String message, String role) {
		ObjectNode messageObj = messagesArray.addObject();
		messageObj.put("role", role);
		messageObj.put("content", message);
	}

	/**
	 * Sends the request.
	 * @param client the HTTP client
	 * @return the completion response
	 * @throws IOException if there's any sort of problem communicating with the
	 * ChatGPT servers
	 */
	public String send(CloseableHttpClient client) throws IOException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Sending request to ChatGPT: " + jsonToStringDebug(requestRoot));
		}

		JsonNode responseBody = null;
		int responseStatusCode = 0;
		try {
			HttpPost request = new HttpPost("https://api.openai.com/v1/chat/completions");
			request.setHeader("Authorization", "Bearer " + apiKey);
			request.setEntity(new StringEntity(jsonToString(requestRoot), ContentType.APPLICATION_JSON));

			try (CloseableHttpResponse response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				try (InputStream in = response.getEntity().getContent()) {
					responseBody = mapper.readTree(in);
				}
			}
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Response from ChatGPT: " + jsonToStringDebug(responseBody));
			}

			String completionMessage;
			try {
				completionMessage = responseBody.get("choices").get(0).get("message").get("content").asText();
			} catch (NullPointerException e) {
				//the JSON is not structured as expected
				throw new IOException(e);
			}

			return completionMessage;
		} catch (IOException e) {
			String requestBodyStr = jsonToStringDebug(requestRoot);

			StringBuilder sb = new StringBuilder();
			sb.append("Problem communicating with ChatGPT API.");
			sb.append("\nRequest: ").append(requestBodyStr);

			if (responseBody != null) {
				String responseBodyStr = jsonToStringDebug(responseBody);
				sb.append("\nResponse (HTTP ").append(responseStatusCode).append("): ").append(responseBodyStr);
			}

			throw new IOException(sb.toString(), e);
		}
	}

	private String jsonToString(JsonNode node) throws JsonProcessingException {
		ObjectWriter writer = mapper.writer();
		return writer.writeValueAsString(node);
	}

	private String jsonToStringDebug(JsonNode node) {
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
		try {
			return writer.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			return "Error writing JSON object to string: " + e.getMessage();
		}
	}

	//@formatter:off
	/*
	Sample request/response
	
	Request:
	{"model":"gpt-3.5-turbo","messages":[{"role":"system","content":"You are knowledgable, but grumpy, Java software developer."},{"role":"user","content":"When was Java 17 released?"}],"max_tokens":50,"temperature":1.0}
	
	Response:
	{
	  "id" : "chatcmpl-7Yi4sFx7sq4hfA3huekbWN04TVZAN",
	  "object" : "chat.completion",
	  "created" : 1688506942,
	  "model" : "gpt-3.5-turbo-0613",
	  "choices" : [ {
	    "index" : 0,
	    "message" : {
	      "role" : "assistant",
	      "content" : "Java 17 was released on September 14, 2021. Finally, they managed to make it work, but who knows for how long..."
	    },
	    "finish_reason" : "stop"
	  } ],
	  "usage" : {
	    "prompt_tokens" : 32,
	    "completion_tokens" : 30,
	    "total_tokens" : 62
	  }
	}
	*/
	//@formatter:on
}
