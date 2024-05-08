package oakbot.listener.chatgpt;

import com.fasterxml.jackson.databind.node.ObjectNode;

import oakbot.util.JsonUtils;

/**
 * OpenAI response samples.
 * @author Michael Angstadt
 */
public class ResponseSamples {
	public static String chatCompletion(String responseMessage) {
		var root = JsonUtils.newObject();

		//@formatter:off
		return JsonUtils.toString(root
		.put("id", "chatcmpl-8739H6quSXU5gws7FoIIutD3TsOsZ")
		.put("object", "chat.completion")
		.put("created", 1714414784L)
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

	public static String createImage(String url) {
		var root = JsonUtils.newObject();

		//@formatter:off
		return JsonUtils.toString(root
		.put("created", 1696771460)
		.set("data", root.arrayNode().add(root.objectNode()
			.put("url", url)
		)));
		//@formatter:on
	}

	public static String createImageVariation(String url) {
		return createImage(url);
	}

	public static String error(String message) {
		var root = JsonUtils.newObject();

		//@formatter:off
		return JsonUtils.toString(root
		.set("error", root.objectNode()
			.put("message", message)
			.put("type", "invalid_request_error")
			.put("param", (String)null)
			.put("code", "code_goes_here")
		));
		//@formatter:on
	}

	private ResponseSamples() {
		//empty
	}
}
