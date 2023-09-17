package oakbot.listener.chatgpt;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;

public class OpenAIClient {
	private static final Logger logger = Logger.getLogger(OpenAIClient.class.getName());

	private final String apiKey;

	/**
	 * @param apiKey the API key
	 * @see "https://platform.openai.com/account/api-keys"
	 */
	public OpenAIClient(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Sends a chat completion request.
	 * @param chatRequest the request
	 * @return the completion response
	 * @throws OpenAIException if OpenAI returns an error response
	 * @throws IOException if there's a problem communicating with OpenAI
	 * @see "https://platform.openai.com/docs/api-reference/chat"
	 */
	public String chatCompletion(ChatCompletionRequest chatRequest) throws IOException {
		HttpPost request = postRequestWithApiKey("/v1/chat/completions");
		request.setEntity(new JsonEntity(chatRequest.getRoot()));

		logRequest(request);

		JsonNode responseBody = null;
		int responseStatusCode = 0;
		try (CloseableHttpClient client = HttpFactory.connect().getClient()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				try (InputStream in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}
			}

			logResponse(responseStatusCode, responseBody);

			lookForError(responseBody);

			try {
				return responseBody.get("choices").get(0).get("message").get("content").asText();
			} catch (NullPointerException e) {
				throw new IOException("JSON response not structured as expected.", e);
			}
		} catch (IOException e) {
			logError(request, responseStatusCode, responseBody, e);
			throw e;
		}
	}

	/**
	 * Creates an image.
	 * @param prompt a description of what the image should look like.
	 * @return the URL to the image. The image will be deleted off their servers
	 * within 5-10 minutes.
	 * @throws OpenAIException if OpenAI returns an error response
	 * @throws IOException if there's a problem communicating with OpenAI
	 * @see "https://platform.openai.com/docs/api-reference/images/create"
	 */
	public String createImage(String prompt) throws IOException, OpenAIException {
		HttpPost request = postRequestWithApiKey("/v1/images/generations");

		//@formatter:off
		request.setEntity(new JsonEntity(JsonUtils.newObject()
			.put("prompt", prompt)
			.put("size", "256x256")
		));
		//@formatter:on

		logRequest(request);

		JsonNode responseBody = null;
		int responseStatusCode = 0;
		try (CloseableHttpClient client = HttpFactory.connect().getClient()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				try (InputStream in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}
			}

			logResponse(responseStatusCode, responseBody);

			lookForError(responseBody);

			try {
				return responseBody.get("data").get(0).get("url").asText();
			} catch (NullPointerException e) {
				throw new IOException("JSON response not structured as expected.", e);
			}
		} catch (IOException e) {
			logError(request, responseStatusCode, responseBody, e);
			throw e;
		}
	}

	/**
	 * Creates a variation of the given image.
	 * @param url the URL to the image
	 * @return the URL to the variation. The image will be deleted off their
	 * servers within 5-10 minutes.
	 * @throws OpenAIException if OpenAI returns an error response
	 * @throws IOException if there's a problem downloading the input image or
	 * communicating with OpenAI
	 * @see "https://platform.openai.com/docs/api-reference/images/createVariation"
	 */
	public String createImageVariation(String url) throws IOException, OpenAIException {
		try (CloseableHttpClient client = HttpFactory.connect().getClient()) {
			byte[] image = downloadImage(client, url);

			HttpPost request = postRequestWithApiKey("/v1/images/variations");

			//@formatter:off
			request.setEntity(MultipartEntityBuilder.create()
				.addBinaryBody("image", image, ContentType.IMAGE_PNG, "image.png")
				.addTextBody("size", "256x256")
			.build());
			//@formatter:on

			logRequest(request);

			JsonNode responseBody = null;
			int responseStatusCode = 0;

			try (CloseableHttpResponse response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				try (InputStream in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}

				logResponse(responseStatusCode, responseBody);

				lookForError(responseBody);

				try {
					return responseBody.get("data").get(0).get("url").asText();
				} catch (NullPointerException e) {
					throw new IOException("JSON response not structured as expected.", e);
				}
			} catch (IOException e) {
				logError(request, responseStatusCode, responseBody, e);
				throw e;
			}
		}
	}

	private void logRequest(HttpPost request) {
		logger.fine(() -> {
			StringBuilder sb = new StringBuilder();
			sb.append("Sending request to OpenAI: \nURI: ").append(request.getURI());

			if (request.getEntity() instanceof JsonEntity) {
				JsonEntity entity = (JsonEntity) request.getEntity();
				sb.append("\nBody: " + JsonUtils.prettyPrint(entity.node));
			}

			return sb.toString();
		});
	}

	private void logResponse(int statusCode, JsonNode body) {
		logger.fine(() -> "Response from OpenAI: HTTP " + statusCode + ": " + JsonUtils.prettyPrint(body));
	}

	private void logError(HttpPost request, int responseStatusCode, JsonNode responseBody, IOException e) throws IOException {
		logger.log(Level.SEVERE, e, () -> {
			StringBuilder sb = new StringBuilder();
			sb.append("Problem communicating with OpenAI.");
			sb.append("\nRequest: ").append(request.getURI());

			if (request.getEntity() instanceof JsonEntity) {
				JsonEntity entity = (JsonEntity) request.getEntity();
				sb.append(": ").append(JsonUtils.prettyPrint(entity.node));
			}

			if (responseBody != null) {
				String responseBodyStr = JsonUtils.prettyPrint(responseBody);
				sb.append("\nResponse (HTTP ").append(responseStatusCode).append("): ").append(responseBodyStr);
			}

			return sb.toString();
		});

		throw e;
	}

	private HttpPost postRequestWithApiKey(String uriPath) {
		HttpPost request = new HttpPost("https://api.openai.com" + uriPath);
		request.setHeader("Authorization", "Bearer " + apiKey);
		return request;
	}

	private byte[] downloadImage(CloseableHttpClient client, String url) throws IOException {
		HttpGet request = new HttpGet(url);

		try (CloseableHttpResponse response = client.execute(request)) {
			int status = response.getStatusLine().getStatusCode();
			if (status != 200) {
				throw new IOException("Image URL returned HTTP " + status + ".");
			}

			//TODO convert JPEGs to PNGs here. However, it can't find ImageIO on the classpath for some reason,

			return EntityUtils.toByteArray(response.getEntity());
		}
	}

	/**
	 * Throws an exception if there is an error in the given response from the
	 * OpenAI API.
	 * @param response the OpenAI API response
	 * @throws OpenAIException if there is an error in the given response
	 */
	private void lookForError(JsonNode response) throws OpenAIException {
		JsonNode error = response.get("error");
		if (error == null) {
			return;
		}

		JsonNode node = error.get("message");
		String message = (node == null) ? null : node.asText();

		node = error.get("type");
		String type = (node == null) ? null : node.asText();

		node = error.get("param");
		String param = (node == null) ? null : node.asText();

		node = error.get("code");
		String code = (node == null) ? null : node.asText();

		throw new OpenAIException(message, type, param, code);
	}

	private static class JsonEntity extends StringEntity {
		private final JsonNode node;

		public JsonEntity(JsonNode node) {
			super(JsonUtils.toString(node), ContentType.APPLICATION_JSON);

			this.node = node;
		}
	}
}
