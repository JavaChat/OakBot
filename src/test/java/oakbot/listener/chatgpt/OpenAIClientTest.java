package oakbot.listener.chatgpt;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;
import oakbot.util.MockHttpClientBuilder;

/**
 * @author Michael Angstadt
 */
public class OpenAIClientTest {
	@After
	public void after() {
		HttpFactory.restore();
	}

	@Test
	public void chatCompletion() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");

		ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest("Prompt goes here.");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.request("POST", "https://api.openai.com/v1/chat/completions", request -> {
				String expected = "Bearer KEY";
				String actual = request.getFirstHeader("Authorization").getValue();
				assertEquals(expected, actual);
			})
			.response(200, responseChatCompletion("Response message."))
		.build());
		//@formatter:on

		String expected = "Response message.";
		String actual = client.chatCompletion(chatCompletionRequest);
		assertEquals(expected, actual);
	}

	private String responseChatCompletion(String responseMessage) {
		ObjectNode root = JsonUtils.newObject();

		//@formatter:off
		return JsonUtils.toString(root
		.put("id", "chatcmpl-8739H6quSXU5gws7FoIIutD3TsOsZ")
		.put("object", "chat.completion")
		.put("model", "gpt-3.5-turbo-0613")
		.<ObjectNode>set("choices", root.arrayNode().add(root.objectNode()
			.put("index", 0)
			.<ObjectNode>set("message", root.objectNode()
				.put("role", "assistant")
				.put("content", responseMessage)
			)
			.put("finish_reason", "stop")
		))
		.<ObjectNode>set("usage", root.objectNode()
			.put("prompt_tokens", 50)
			.put("completion_tokens", 9)
			.put("total_tokens", 59)
		));
		//@formatter:on
	}
}
