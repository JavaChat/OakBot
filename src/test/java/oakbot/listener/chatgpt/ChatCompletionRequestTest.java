package oakbot.listener.chatgpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

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
		request.addBotMessage("Bot message 1.", List.of("http://www.example.com/image.png"));
		request.addBotMessage("Bot message 2.", List.of(), 1);
		JsonNode root = request.getRoot();

		assertEquals("gpt-3.5-turbo", root.get("model").asText());
		assertEquals(3, root.get("messages").size());
		assertEquals(3, request.getMessageCount());
		assertEquals("system", root.get("messages").get(0).get("role").asText());
		assertEquals("Prompt.", root.get("messages").get(0).get("content").asText());

		assertEquals("assistant", root.get("messages").get(1).get("role").asText());
		assertEquals("Bot message 2.", root.get("messages").get(1).get("content").asText());

		assertEquals("assistant", root.get("messages").get(2).get("role").asText());
		assertEquals("text", root.get("messages").get(2).get("content").get(0).get("type").asText());
		assertEquals("Bot message 1.", root.get("messages").get(2).get("content").get(0).get("text").asText());
		assertEquals("image_url", root.get("messages").get(2).get("content").get(1).get("type").asText());
		assertEquals("http://www.example.com/image.png", root.get("messages").get(2).get("content").get(1).get("image_url").get("url").asText());
		assertEquals("low", root.get("messages").get(2).get("content").get(1).get("image_url").get("detail").asText());

		assertNull(root.get("max_tokens"));
		assertNull(root.get("stop"));
	}

	@Test
	public void addHumanMessage() {
		ChatCompletionRequest request = new ChatCompletionRequest("Prompt.");
		request.addHumanMessage("Human message 1.", List.of("http://www.example.com/image.png"));
		request.addHumanMessage("Human message 2.", List.of(), 1);
		JsonNode root = request.getRoot();

		assertEquals("gpt-3.5-turbo", root.get("model").asText());
		assertEquals(3, root.get("messages").size());
		assertEquals(3, request.getMessageCount());
		assertEquals("system", root.get("messages").get(0).get("role").asText());
		assertEquals("Prompt.", root.get("messages").get(0).get("content").asText());

		assertEquals("user", root.get("messages").get(1).get("role").asText());
		assertEquals("Human message 2.", root.get("messages").get(1).get("content").asText());

		assertEquals("user", root.get("messages").get(2).get("role").asText());
		assertEquals("text", root.get("messages").get(2).get("content").get(0).get("type").asText());
		assertEquals("Human message 1.", root.get("messages").get(2).get("content").get(0).get("text").asText());
		assertEquals("image_url", root.get("messages").get(2).get("content").get(1).get("type").asText());
		assertEquals("http://www.example.com/image.png", root.get("messages").get(2).get("content").get(1).get("image_url").get("url").asText());
		assertEquals("low", root.get("messages").get(2).get("content").get(1).get("image_url").get("detail").asText());

		assertNull(root.get("max_tokens"));
		assertNull(root.get("stop"));
	}
}
