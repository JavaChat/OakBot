package oakbot.ai.openai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.listener.chatgpt.ResponseSamples;
import oakbot.util.Gobble;
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

		//@formatter:off
		ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest.Builder()
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

				JsonNode node = parseRequestBody(request);
				assertEquals("model", node.get("model").asText());
				assertEquals("system", node.get("messages").get(0).get("role").asText());
				assertEquals("text", node.get("messages").get(0).get("content").get(0).get("type").asText());
				assertEquals("prompt", node.get("messages").get(0).get("content").get(0).get("text").asText());
			})
			.responseOk(ResponseSamples.chatCompletion("Response message."))
		.build());
		//@formatter:on

		String expected = "Response message.";
		String actual = client.chatCompletion(chatCompletionRequest);
		assertEquals(expected, actual);
	}

	@Test
	public void chatCompletion_error() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");

		//@formatter:off
		ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest.Builder()
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

				JsonNode node = parseRequestBody(request);
				assertEquals("model", node.get("model").asText());
				assertEquals("system", node.get("messages").get(0).get("role").asText());
				assertEquals("text", node.get("messages").get(0).get("content").get(0).get("type").asText());
				assertEquals("prompt", node.get("messages").get(0).get("content").get(0).get("text").asText());
			})
			.responseOk(ResponseSamples.error("Error."))
		.build());
		//@formatter:on

		try {
			client.chatCompletion(chatCompletionRequest);
			fail();
		} catch (OpenAIException e) {
			assertEquals("Error.", e.getMessage());
		}
	}

	@Test
	public void createImage() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");
		String url = "https://oaidalleapiprodscus.blob.core.windows.net/private/org-N9aoMjcwsu6DCiJnMMvZSCJL/user-LiiW5Y0ymFAK6mbpkwguzsbU/img-urS5BJU18cX45rehJaB33FhW.png?st=2023-10-08T12%3A24%3A20Z&se=2023-10-08T14%3A24%3A20Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-10-08T03%3A41%3A31Z&ske=2023-10-09T03%3A41%3A31Z&sks=b&skv=2021-08-06&sig=BKj04a3Ds9gFYURBN/dPDtbEEJ0Wenfx0EVHzItfsM8%3D";

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.request("POST", "https://api.openai.com/v1/images/generations", request -> {
				assertAuthHeader(request, "KEY");
				
				JsonNode root = parseRequestBody(request);
				assertEquals("model", root.get("model").asText());
				assertEquals("Prompt.", root.get("prompt").asText());
				assertEquals("256x256", root.get("size").asText());
			})
			.responseOk(ResponseSamples.createImage(url))
		.build());
		//@formatter:on

		String actual = client.createImage("model", "256x256", "Prompt.");
		assertEquals(url, actual);
	}

	@Test
	public void createImage_error() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.request("POST", "https://api.openai.com/v1/images/generations", request -> {
				assertAuthHeader(request, "KEY");
				
				JsonNode root = parseRequestBody(request);
				assertEquals("model", root.get("model").asText());
				assertEquals("Prompt.", root.get("prompt").asText());
				assertEquals("256x256", root.get("size").asText());
			})
			.responseOk(ResponseSamples.error("Error."))
		.build());
		//@formatter:on

		try {
			client.createImage("model", "256x256", "Prompt.");
			fail();
		} catch (OpenAIException e) {
			assertEquals("Error.", e.getMessage());
		}
	}

	@Test
	public void createImageVariation() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");
		String url = "https://example.com/image.png";
		String resultUrl = "https://oaidalleapiprodscus.blob.core.windows.net/private/org-N9aoMjcwsu6DCiJnMMvZSCJL/user-LiiW5Y0ymFAK6mbpkwguzsbU/img-urS5BJU18cX45rehJaB33FhW.png?st=2023-10-08T12%3A24%3A20Z&se=2023-10-08T14%3A24%3A20Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-10-08T03%3A41%3A31Z&ske=2023-10-09T03%3A41%3A31Z&sks=b&skv=2021-08-06&sig=BKj04a3Ds9gFYURBN/dPDtbEEJ0Wenfx0EVHzItfsM8%3D";

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
				
				String body = getBody(request);
				assertTrue(body.matches("(?s).*?Content-Disposition: form-data; name=\"image\"; filename=\"image\\.png\".*"));
				assertTrue(body.matches("(?s).*?image data.*"));
				
				assertTrue(body.matches("(?s).*?Content-Disposition: form-data; name=\"size\".*"));
				assertTrue(body.matches("(?s).*?256x256.*"));
			})
			.responseOk(ResponseSamples.createImage(resultUrl))
		.build());
		//@formatter:on

		String actual = client.createImageVariation(url);
		assertEquals(resultUrl, actual);
	}

	@Test
	public void createImageVariation_bad_url_syntax() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");
		String url = "https://example.com/image.png user thinks they can include a prompt too";

		try {
			client.createImageVariation(url);
			fail();
		} catch (IllegalArgumentException expected) {
		}
	}

	/**
	 * If the supplied URL returns a non-200 response, throw an exception.
	 */
	@Test
	public void createImageVariation_404() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");
		String url = "https://example.com/image.png";

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet(url)
			.response(404, "")
		.build());
		//@formatter:on

		try {
			client.createImageVariation(url);
			fail();
		} catch (IOException expected) {
		}
	}

	/**
	 * Data with "image/jpeg" content type will be converted to PNG before being
	 * sent to OpenAI.
	 */
	@Test
	public void createImageVariation_jpeg() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");
		String url = "https://example.com/image.png";
		String resultUrl = "https://oaidalleapiprodscus.blob.core.windows.net/private/org-N9aoMjcwsu6DCiJnMMvZSCJL/user-LiiW5Y0ymFAK6mbpkwguzsbU/img-urS5BJU18cX45rehJaB33FhW.png?st=2023-10-08T12%3A24%3A20Z&se=2023-10-08T14%3A24%3A20Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-10-08T03%3A41%3A31Z&ske=2023-10-09T03%3A41%3A31Z&sks=b&skv=2021-08-06&sig=BKj04a3Ds9gFYURBN/dPDtbEEJ0Wenfx0EVHzItfsM8%3D";

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

		String actual = client.createImageVariation(url);
		assertEquals(resultUrl, actual);
	}

	/**
	 * Data with "image/jpeg" content type will be converted to PNG before being
	 * sent to OpenAI. If the conversion fails, send the original data to
	 * OpenAI.
	 */
	@Test
	public void createImageVariation_jpeg_invalid() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");
		String url = "https://example.com/image.jpg";

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

		try {
			client.createImageVariation(url);
			fail();
		} catch (OpenAIException e) {
			assertEquals("Uploaded image must be a PNG and less than 4 MB.", e.getMessage());
		}
	}

	/**
	 * Data is sent to OpenAI is unaltered unless it has a "image/jpeg" content
	 * type.
	 */
	@Test
	public void createImageVariation_non_png_content_type() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");
		String url = "https://www.google.com";

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet(url)
			.responseOk("non-image data".getBytes(), ContentType.TEXT_HTML)
			.request("POST", "https://api.openai.com/v1/images/variations", request -> {
				assertAuthHeader(request, "KEY");
				
				String body = getBody(request);
				assertTrue(body.matches("(?s).*?non-image data.*"));
			})
			.responseOk(ResponseSamples.error("Uploaded image must be a PNG and less than 4 MB."))
		.build());
		//@formatter:on

		try {
			client.createImageVariation(url);
			fail();
		} catch (OpenAIException e) {
			assertEquals("Uploaded image must be a PNG and less than 4 MB.", e.getMessage());
		}
	}

	/**
	 * Data without a content type will be sent to OpenAI unaltered.
	 */
	@Test
	public void createImageVariation_no_content_type() throws Exception {
		OpenAIClient client = new OpenAIClient("KEY");
		String url = "https://example.com/image.png";
		String resultUrl = "https://oaidalleapiprodscus.blob.core.windows.net/private/org-N9aoMjcwsu6DCiJnMMvZSCJL/user-LiiW5Y0ymFAK6mbpkwguzsbU/img-urS5BJU18cX45rehJaB33FhW.png?st=2023-10-08T12%3A24%3A20Z&se=2023-10-08T14%3A24%3A20Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-10-08T03%3A41%3A31Z&ske=2023-10-09T03%3A41%3A31Z&sks=b&skv=2021-08-06&sig=BKj04a3Ds9gFYURBN/dPDtbEEJ0Wenfx0EVHzItfsM8%3D";

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

		String actual = client.createImageVariation(url);
		assertEquals(resultUrl, actual);
	}

	private static void assertAuthHeader(HttpRequest request, String key) {
		String expected = "Bearer " + key;
		String actual = request.getFirstHeader("Authorization").getValue();
		assertEquals(expected, actual);
	}

	private static JsonNode parseRequestBody(HttpRequest request) {
		HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
		try (InputStream in = entity.getContent()) {
			return JsonUtils.parse(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String getBody(HttpRequest request) {
		HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
		try {
			return EntityUtils.toString(entity);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
