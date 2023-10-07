package oakbot.listener.chatgpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Michael Angstadt
 */
public class ChatCompletionRequestTest {
	@Test
	public void constructor() {
		ChatCompletionRequest request = new ChatCompletionRequest("Prompt.");
		JsonNode root = request.getRoot();

		assertEquals("gpt-3.5-turbo", root.get("model").asText());
		assertEquals(1, root.get("messages").size());
		assertEquals("system", root.get("messages").get(0).get("role").asText());
		assertEquals("Prompt.", root.get("messages").get(0).get("content").asText());
		assertNull(root.get("max_tokens"));
		assertNull(root.get("stop"));
	}

	@Test
	public void setModel() {
		ChatCompletionRequest request = new ChatCompletionRequest("Prompt.");
		request.setModel("model");
		JsonNode root = request.getRoot();

		assertEquals("model", root.get("model").asText());
		assertEquals(1, root.get("messages").size());
		assertEquals("system", root.get("messages").get(0).get("role").asText());
		assertEquals("Prompt.", root.get("messages").get(0).get("content").asText());
		assertNull(root.get("max_tokens"));
		assertNull(root.get("stop"));
	}

	@Test
	public void setMaxTokensForCompletion() {
		ChatCompletionRequest request = new ChatCompletionRequest("Prompt.");
		request.setMaxTokensForCompletion(50);
		JsonNode root = request.getRoot();

		assertEquals("gpt-3.5-turbo", root.get("model").asText());
		assertEquals(1, root.get("messages").size());
		assertEquals("system", root.get("messages").get(0).get("role").asText());
		assertEquals("Prompt.", root.get("messages").get(0).get("content").asText());
		assertEquals(50, root.get("max_tokens").asInt());
		assertNull(root.get("stop"));

		request.setMaxTokensForCompletion(0);
		assertNull(root.get("max_tokens"));
	}

	@Test
	public void addStopSequence() {
		ChatCompletionRequest request = new ChatCompletionRequest("Prompt.");
		request.addStopSequence("STOP");
		JsonNode root = request.getRoot();

		assertEquals("gpt-3.5-turbo", root.get("model").asText());
		assertEquals(1, root.get("messages").size());
		assertEquals("system", root.get("messages").get(0).get("role").asText());
		assertEquals("Prompt.", root.get("messages").get(0).get("content").asText());
		assertNull(root.get("max_tokens"));
		assertEquals(1, root.get("stop").size());
		assertEquals("STOP", root.get("stop").get(0).asText());
	}

	@Test
	public void addBotMessage() {
		ChatCompletionRequest request = new ChatCompletionRequest("Prompt.");
		request.addBotMessage("Bot message.");
		JsonNode root = request.getRoot();

		assertEquals("gpt-3.5-turbo", root.get("model").asText());
		assertEquals(2, root.get("messages").size());
		assertEquals("system", root.get("messages").get(0).get("role").asText());
		assertEquals("Prompt.", root.get("messages").get(0).get("content").asText());
		assertEquals("assistant", root.get("messages").get(1).get("role").asText());
		assertEquals("Bot message.", root.get("messages").get(1).get("content").asText());
		assertNull(root.get("max_tokens"));
		assertNull(root.get("stop"));
	}

	@Test
	public void addHumanMessage() {
		ChatCompletionRequest request = new ChatCompletionRequest("Prompt.");
		request.addHumanMessage("Human message.");
		JsonNode root = request.getRoot();

		assertEquals("gpt-3.5-turbo", root.get("model").asText());
		assertEquals(2, root.get("messages").size());
		assertEquals("system", root.get("messages").get(0).get("role").asText());
		assertEquals("Prompt.", root.get("messages").get(0).get("content").asText());
		assertEquals("user", root.get("messages").get(1).get("role").asText());
		assertEquals("Human message.", root.get("messages").get(1).get("content").asText());
		assertNull(root.get("max_tokens"));
		assertNull(root.get("stop"));
	}
}
