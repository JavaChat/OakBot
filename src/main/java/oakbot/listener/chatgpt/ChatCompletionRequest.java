package oakbot.listener.chatgpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import oakbot.util.JsonUtils;

/**
 * An OpenAI chat completion request.
 * @author Michael Angstadt
 * @see "https://platform.openai.com/docs/api-reference/chat"
 */
public class ChatCompletionRequest {
	private final ObjectNode root;
	private final ArrayNode messages;
	private ArrayNode stopSequences;

	/**
	 * @param prompt defines the bot's personality (e.g. "You are a helpful
	 * assistant"). This counts against your usage quota. Each word is about
	 * 1.33 tokens.
	 */
	public ChatCompletionRequest(String prompt) {
		root = JsonUtils.newObject();
		setModel("gpt-3.5-turbo");

		messages = root.arrayNode();
		root.set("messages", messages);

		addMessage(prompt, "system", 0);
	}

	/**
	 * Sets the model. Defaults to "gpt-3.5-turbo".
	 * @param model the model
	 * @see "https://platform.openai.com/docs/models/overview"
	 */
	public void setModel(String model) {
		root.put("model", model);
	}

	/**
	 * <p>
	 * Sets the maximum number of tokens tokens the completion can be. Defaults
	 * to no limit.
	 * </p>
	 * <p>
	 * If this number is too short, then the completion may be abruptly
	 * truncated. One word is about 1.33 tokens.
	 * </p>
	 * @param maxTokens the max tokens or {@code <= 0} for no limit
	 */
	public void setMaxTokensForCompletion(int maxTokens) {
		if (maxTokens > 0) {
			root.put("max_tokens", maxTokens);
		} else {
			root.remove("max_tokens");
		}
	}

	/**
	 * Adds a stop sequence, which tells it to stop generating further tokens.
	 * Defaults to no stop sequences.
	 * @param stopSequence the stop sequence
	 */
	public void addStopSequence(String stopSequence) {
		if (stopSequences == null) {
			stopSequences = root.arrayNode();
			root.set("stop", stopSequences);
		}

		stopSequences.add(stopSequence);
	}

	/**
	 * Adds a message to the request, and marks the message as having been
	 * written by a person.
	 * @param message the message
	 */
	public void addHumanMessage(String message) {
		addMessage(message, "user", getMessageCount());
	}

	/**
	 * Adds a message to the request, and marks the message as having been
	 * written by a person.
	 * @param message the message
	 * @param index the position in the list to insert the message
	 */
	public void addHumanMessage(String message, int index) {
		addMessage(message, "user", index);
	}

	/**
	 * Adds a message to the request, and marks the message as having been
	 * written by ChatGPT.
	 * @param message the message
	 */
	public void addBotMessage(String message) {
		addMessage(message, "assistant", getMessageCount());
	}

	/**
	 * Adds a message to the request, and marks the message as having been
	 * written by ChatGPT.
	 * @param message the message
	 * @param index the position in the list to insert the message
	 */
	public void addBotMessage(String message, int index) {
		addMessage(message, "assistant", index);
	}

	/**
	 * Adds a message to the request.
	 * @param message the message
	 * @param role the role
	 * @param index the position in the list to insert the message
	 */
	private void addMessage(String message, String role, int index) {
		//@formatter:off
		messages.insertObject(index)
			.put("role", role)
			.put("content", message);
		//@formatter:off
	}

	/**
	 * Gets the number of messages in the request, including the prompt.
	 * @return the number of messages
	 */
	public int getMessageCount() {
		return messages.size();
	}

	JsonNode getRoot() {
		return root;
	}

	//@formatter:off
	/*
	Sample request/response
	
	Request:
	{
	  "model" : "gpt-3.5-turbo",
	  "messages" : [{"role":"system","content":"You are knowledgable, but grumpy, Java software developer."},{"role":"user","content":"When was Java 17 released?"}],
	  "max_tokens" : 50,
	  "temperature" : 1.0
	}
	
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
