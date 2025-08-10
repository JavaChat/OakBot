package oakbot.ai.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.ai.openai.ChatCompletionRequest.Message;
import oakbot.listener.chatgpt.ResponseSamples;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;
import oakbot.util.MockHttpClientBuilder;

/**
 * @author Michael Angstadt
 */
class OpenAIClientTest {
	/**
	 * Program for testing API calls.
	 * @param args the OpenAI API key
	 */
	public static void main(String args[]) throws Exception {
		/*
		 * Send all log output to console.
		 */
		Logger rootLogger = Logger.getLogger("");
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		rootLogger.addHandler(consoleHandler);
		rootLogger.setLevel(Level.ALL);

		var apiKey = args[0];
		var client = new OpenAIClient(apiKey);

		{
			//@formatter:off
			var request = new ChatCompletionRequest.Builder().maxTokens(300).model("gpt-5").messages(List.of(
				new Message.Builder().role("system").text("You are friendly.").build(),
				new Message.Builder().role("user").text("How are you?").build()
			)).build();
			//@formatter:on

			var response = client.chatCompletion(request);
			System.out.println(response.getChoices().get(0).getContent());
		}

		{
			var response = client.createImage("gpt-image-1", "1024x1024", "jpeg", null, "A cute Java programmer");
			var fileName = "image." + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()).replaceAll("[-:]", "") + ".jpg";
			Files.write(Paths.get(fileName), response.getData());
			System.out.println("Revised prompt: " + response.getRevisedPrompt());
		}
	}

	@AfterEach
	void after() {
		HttpFactory.restore();
	}

	@Test
	void chatCompletion() throws Exception {
		var client = new OpenAIClient("KEY");

		//@formatter:off
		var chatCompletionRequest = new ChatCompletionRequest.Builder()
			.model("model")
			.messages(List.of(
				new ChatCompletionRequest.Message.Builder()
					.role("system")
					.text("prompt")
				.build()
			))
		.build();
		//@formatter:on

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.request("POST", "https://api.openai.com/v1/chat/completions", request -> {
				assertAuthHeader(request, "KEY");

				var node = parseRequestBody(request);
				assertEquals("model", node.get("model").asText());

				var it = node.get("messages").iterator();
				var message = it.next();
				assertEquals("system", message.get("role").asText());
				assertEquals("text", message.get("content").get(0).get("type").asText());
				assertEquals("prompt", message.get("content").get(0).get("text").asText());
				assertFalse(it.hasNext());
			})
			.responseOk(ResponseSamples.chatCompletion("Response message."))
		.build());
		//@formatter:on

		var actual = client.chatCompletion(chatCompletionRequest);
		assertEquals("chatcmpl-8739H6quSXU5gws7FoIIutD3TsOsZ", actual.getId());
		assertEquals("gpt-3.5-turbo-0613", actual.getModel());
		assertEquals(Instant.ofEpochSecond(1714414784L), actual.getCreated());
		assertEquals("", actual.getSystemFingerprint());
		assertEquals(1, actual.getChoices().size());
		assertEquals("Response message.", actual.getChoices().get(0).getContent());
		assertEquals("stop", actual.getChoices().get(0).getFinishReason());
		assertEquals(50, actual.getPromptTokens());
		assertEquals(9, actual.getCompletionTokens());
	}

	@Test
	void chatCompletion_sanitize_name() throws Exception {
		var client = new OpenAIClient("KEY");

		//@formatter:off
		var chatCompletionRequest = new ChatCompletionRequest.Builder()
			.model("model")
			.messages(List.of(
				new ChatCompletionRequest.Message.Builder()
					.role("system")
					.text("prompt")
				.build(),
				new ChatCompletionRequest.Message.Builder()
					.role("user")
					//name not defined
					.text("Hello1")
				.build(),
				new ChatCompletionRequest.Message.Builder()
					.role("user")
					.name("мзж") //remove all characters
					.text("Hello2")
				.build(),
				new ChatCompletionRequest.Message.Builder()
					.role("user")
					.name("мMзж") //remove all but the second character
					.text("Hello3")
				.build(),
				new ChatCompletionRequest.Message.Builder()
					.role("user")
					.name("aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffffgggggggggg") //truncate to 64 characters
					.text("Hello4")
				.build()
			))
		.build();
		//@formatter:on

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.request("POST", "https://api.openai.com/v1/chat/completions", request -> {
				assertAuthHeader(request, "KEY");

				var node = parseRequestBody(request);
				assertEquals("model", node.get("model").asText());
				
				var it = node.get("messages").iterator();
				var message = it.next();
				assertEquals("system", message.get("role").asText());
				assertEquals("text", message.get("content").get(0).get("type").asText());
				assertEquals("prompt", message.get("content").get(0).get("text").asText());
				message = it.next();
				assertEquals("user", message.get("role").asText());
				assertNull(message.get("content").get(0).get("name"));
				assertEquals("text", message.get("content").get(0).get("type").asText());
				assertEquals("Hello1", message.get("content").get(0).get("text").asText());
				message = it.next();
				assertEquals("user", message.get("role").asText());
				assertNull(message.get("content").get(0).get("name"));
				assertEquals("text", message.get("content").get(0).get("type").asText());
				assertEquals("Hello2", message.get("content").get(0).get("text").asText());
				message = it.next();
				assertEquals("user", message.get("role").asText());
				assertEquals("M", message.get("name").asText());
				assertEquals("text", message.get("content").get(0).get("type").asText());
				assertEquals("Hello3", message.get("content").get(0).get("text").asText());
				message = it.next();
				assertEquals("user", message.get("role").asText());
				assertEquals("aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffffgggg", message.get("name").asText());
				assertEquals("text", message.get("content").get(0).get("type").asText());
				assertEquals("Hello4", message.get("content").get(0).get("text").asText());
				assertFalse(it.hasNext());
			})
			.responseOk(ResponseSamples.chatCompletion("Response message."))
		.build());
		//@formatter:on

		var actual = client.chatCompletion(chatCompletionRequest);
		assertEquals("chatcmpl-8739H6quSXU5gws7FoIIutD3TsOsZ", actual.getId());
		assertEquals("gpt-3.5-turbo-0613", actual.getModel());
		assertEquals(Instant.ofEpochSecond(1714414784L), actual.getCreated());
		assertEquals("", actual.getSystemFingerprint());
		assertEquals(1, actual.getChoices().size());
		assertEquals("Response message.", actual.getChoices().get(0).getContent());
		assertEquals("stop", actual.getChoices().get(0).getFinishReason());
		assertEquals(50, actual.getPromptTokens());
		assertEquals(9, actual.getCompletionTokens());
	}

	@Test
	void chatCompletion_error() {
		var client = new OpenAIClient("KEY");

		//@formatter:off
		var chatCompletionRequest = new ChatCompletionRequest.Builder()
			.model("model")
			.messages(List.of(
				new ChatCompletionRequest.Message.Builder()
					.role("system")
					.text("prompt")
				.build()
			))
		.build();
		//@formatter:on

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.request("POST", "https://api.openai.com/v1/chat/completions", request -> {
				assertAuthHeader(request, "KEY");

				var node = parseRequestBody(request);
				assertEquals("model", node.get("model").asText());

				var it = node.get("messages").iterator();
				var message = it.next();
				assertEquals("system", message.get("role").asText());
				assertEquals("text", message.get("content").get(0).get("type").asText());
				assertEquals("prompt", message.get("content").get(0).get("text").asText());
				assertFalse(it.hasNext());
			})
			.responseOk(ResponseSamples.error("Error."))
		.build());
		//@formatter:on

		var e = assertThrows(OpenAIException.class, () -> client.chatCompletion(chatCompletionRequest));
		assertEquals("Error.", e.getMessage());
	}

	@Test
	void createImage() throws Exception {
		var client = new OpenAIClient("KEY");
		var url = "https://oaidalleapiprodscus.blob.core.windows.net/private/org-N9aoMjcwsu6DCiJnMMvZSCJL/user-LiiW5Y0ymFAK6mbpkwguzsbU/img-urS5BJU18cX45rehJaB33FhW.png?st=2023-10-08T12%3A24%3A20Z&se=2023-10-08T14%3A24%3A20Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-10-08T03%3A41%3A31Z&ske=2023-10-09T03%3A41%3A31Z&sks=b&skv=2021-08-06&sig=BKj04a3Ds9gFYURBN/dPDtbEEJ0Wenfx0EVHzItfsM8%3D";

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.request("POST", "https://api.openai.com/v1/images/generations", request -> {
				assertAuthHeader(request, "KEY");
				
				var root = parseRequestBody(request);
				assertEquals("model", root.get("model").asText());
				assertEquals("Prompt.", root.get("prompt").asText());
				assertEquals("256x256", root.get("size").asText());
			})
			.responseOk(ResponseSamples.createImage(url))
		.build());
		//@formatter:on

		var actual = client.createImage("model", "256x256", null, null, "Prompt.");
		assertEquals(Instant.ofEpochSecond(1696771460L), actual.getCreated());
		assertEquals(url, actual.getUrl());
		assertNull(actual.getRevisedPrompt());
	}

	@Test
	void createImage_error() {
		var client = new OpenAIClient("KEY");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.request("POST", "https://api.openai.com/v1/images/generations", request -> {
				assertAuthHeader(request, "KEY");
				
				var root = parseRequestBody(request);
				assertEquals("model", root.get("model").asText());
				assertEquals("Prompt.", root.get("prompt").asText());
				assertEquals("256x256", root.get("size").asText());
			})
			.responseOk(ResponseSamples.error("Error."))
		.build());
		//@formatter:on

		var e = assertThrows(OpenAIException.class, () -> client.createImage("model", "256x256", null, null, "Prompt."));
		assertEquals("Error.", e.getMessage());
	}

	@Test
	void createImageVariation() throws Exception {
		var client = new OpenAIClient("KEY");
		var url = "https://example.com/image.png";
		var resultUrl = "https://oaidalleapiprodscus.blob.core.windows.net/private/org-N9aoMjcwsu6DCiJnMMvZSCJL/user-LiiW5Y0ymFAK6mbpkwguzsbU/img-urS5BJU18cX45rehJaB33FhW.png?st=2023-10-08T12%3A24%3A20Z&se=2023-10-08T14%3A24%3A20Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-10-08T03%3A41%3A31Z&ske=2023-10-09T03%3A41%3A31Z&sks=b&skv=2021-08-06&sig=BKj04a3Ds9gFYURBN/dPDtbEEJ0Wenfx0EVHzItfsM8%3D";

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet(url)
			.responseOk("image data".getBytes(), ContentType.IMAGE_PNG)
			.request("POST", "https://api.openai.com/v1/images/variations", request -> {
				assertAuthHeader(request, "KEY");
				
				/*
				--G90J1DrSCMftxImIw89yxXchS58xY-t
				Content-Disposition: form-data; name="image"; filename="image.png"
				Content-Type: image/png
				Content-Transfer-Encoding: binary
				
				image data
				--G90J1DrSCMftxImIw89yxXchS58xY-t
				Content-Disposition: form-data; name="size"
				Content-Type: text/plain; charset=ISO-8859-1
				Content-Transfer-Encoding: 8bit
				
				256x256
				--G90J1DrSCMftxImIw89yxXchS58xY-t--
				*/
				
				var body = getBody(request);
				assertTrue(body.matches("(?s).*?Content-Disposition: form-data; name=\"image\"; filename=\"image\\.png\".*"));
				assertTrue(body.matches("(?s).*?image data.*"));
				
				assertTrue(body.matches("(?s).*?Content-Disposition: form-data; name=\"size\".*"));
				assertTrue(body.matches("(?s).*?256x256.*"));
			})
			.responseOk(ResponseSamples.createImage(resultUrl))
		.build());
		//@formatter:on

		var actual = client.createImageVariation(url, "256x256");
		assertEquals(Instant.ofEpochSecond(1696771460L), actual.getCreated());
		assertEquals(resultUrl, actual.getUrl());
		assertNull(actual.getRevisedPrompt());
	}

	@Test
	void createImageVariation_bad_url_syntax() {
		var client = new OpenAIClient("KEY");
		var url = "https://example.com/image.png user thinks they can include a prompt too";

		assertThrows(IllegalArgumentException.class, () -> client.createImageVariation(url, "256x256"));
	}

	/**
	 * If the supplied URL returns a non-200 response, throw an exception.
	 */
	@Test
	void createImageVariation_404() {
		var client = new OpenAIClient("KEY");
		var url = "https://example.com/image.png";

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet(url)
			.response(404, "")
		.build());
		//@formatter:on

		assertThrows(IOException.class, () -> client.createImageVariation(url, "256x256"));
	}

	/**
	 * Data with "image/jpeg" content type will be converted to PNG before being
	 * sent to OpenAI.
	 */
	@Test
	void createImageVariation_jpeg() throws Exception {
		var client = new OpenAIClient("KEY");
		var url = "https://example.com/image.png";
		var resultUrl = "https://oaidalleapiprodscus.blob.core.windows.net/private/org-N9aoMjcwsu6DCiJnMMvZSCJL/user-LiiW5Y0ymFAK6mbpkwguzsbU/img-urS5BJU18cX45rehJaB33FhW.png?st=2023-10-08T12%3A24%3A20Z&se=2023-10-08T14%3A24%3A20Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-10-08T03%3A41%3A31Z&ske=2023-10-09T03%3A41%3A31Z&sks=b&skv=2021-08-06&sig=BKj04a3Ds9gFYURBN/dPDtbEEJ0Wenfx0EVHzItfsM8%3D";

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet(url)
			.responseOk(new Gobble(getClass(), "image.jpg").asByteArray(), ContentType.IMAGE_JPEG)
			.request("POST", "https://api.openai.com/v1/images/variations", request -> {
				assertAuthHeader(request, "KEY");
			})
			.responseOk(ResponseSamples.createImage(resultUrl))
		.build());
		//@formatter:on

		var actual = client.createImageVariation(url, "256x256");
		assertEquals(Instant.ofEpochSecond(1696771460L), actual.getCreated());
		assertEquals(resultUrl, actual.getUrl());
		assertNull(actual.getRevisedPrompt());
	}

	/**
	 * Data with "image/jpeg" content type will be converted to PNG before being
	 * sent to OpenAI. If the conversion fails, send the original data to
	 * OpenAI.
	 */
	@Test
	void createImageVariation_jpeg_invalid() {
		var client = new OpenAIClient("KEY");
		var url = "https://example.com/image.jpg";

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet(url)
			.responseOk("bad data".getBytes(), ContentType.IMAGE_JPEG)
			.request("POST", "https://api.openai.com/v1/images/variations", request -> {
				assertAuthHeader(request, "KEY");
			})
			.responseOk(ResponseSamples.error("Uploaded image must be a PNG and less than 4 MB."))
		.build());
		//@formatter:on

		var e = assertThrows(OpenAIException.class, () -> client.createImageVariation(url, "256x256"));
		assertEquals("Uploaded image must be a PNG and less than 4 MB.", e.getMessage());
	}

	/**
	 * Data is sent to OpenAI is unaltered unless it has a "image/jpeg" content
	 * type.
	 */
	@Test
	void createImageVariation_non_png_content_type() {
		var client = new OpenAIClient("KEY");
		var url = "https://www.google.com";

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet(url)
			.responseOk("non-image data".getBytes(), ContentType.TEXT_HTML)
			.request("POST", "https://api.openai.com/v1/images/variations", request -> {
				assertAuthHeader(request, "KEY");
				
				var body = getBody(request);
				assertTrue(body.matches("(?s).*?non-image data.*"));
			})
			.responseOk(ResponseSamples.error("Uploaded image must be a PNG and less than 4 MB."))
		.build());
		//@formatter:on

		var e = assertThrows(OpenAIException.class, () -> client.createImageVariation(url, "256x256"));
		assertEquals("Uploaded image must be a PNG and less than 4 MB.", e.getMessage());
	}

	/**
	 * Data without a content type will be sent to OpenAI unaltered.
	 */
	@Test
	void createImageVariation_no_content_type() throws Exception {
		var client = new OpenAIClient("KEY");
		var url = "https://example.com/image.png";
		var resultUrl = "https://oaidalleapiprodscus.blob.core.windows.net/private/org-N9aoMjcwsu6DCiJnMMvZSCJL/user-LiiW5Y0ymFAK6mbpkwguzsbU/img-urS5BJU18cX45rehJaB33FhW.png?st=2023-10-08T12%3A24%3A20Z&se=2023-10-08T14%3A24%3A20Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-10-08T03%3A41%3A31Z&ske=2023-10-09T03%3A41%3A31Z&sks=b&skv=2021-08-06&sig=BKj04a3Ds9gFYURBN/dPDtbEEJ0Wenfx0EVHzItfsM8%3D";

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet(url)
			.responseOk("image data")
			.request("POST", "https://api.openai.com/v1/images/variations", request -> {
				assertAuthHeader(request, "KEY");
				
				String body = getBody(request);
				assertTrue(body.matches("(?s).*?image data.*"));
			})
			.responseOk(ResponseSamples.createImage(resultUrl))
		.build());
		//@formatter:on

		var actual = client.createImageVariation(url, "256x256");
		assertEquals(Instant.ofEpochSecond(1696771460L), actual.getCreated());
		assertEquals(resultUrl, actual.getUrl());
		assertNull(actual.getRevisedPrompt());
	}

	private static void assertAuthHeader(HttpRequest request, String key) {
		var expected = "Bearer " + key;
		var actual = request.getFirstHeader("Authorization").getValue();
		assertEquals(expected, actual);
	}

	private static JsonNode parseRequestBody(HttpRequest request) {
		var entity = ((HttpEntityEnclosingRequest) request).getEntity();
		try (var in = entity.getContent()) {
			return JsonUtils.parse(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String getBody(HttpRequest request) {
		var entity = ((HttpEntityEnclosingRequest) request).getEntity();
		try {
			return EntityUtils.toString(entity);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
