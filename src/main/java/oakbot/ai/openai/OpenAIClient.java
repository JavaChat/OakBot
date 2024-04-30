package oakbot.ai.openai;

import static oakbot.util.JsonUtils.putIfNotNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;

/**
 * Client for interacting with OpenAI.
 * @author Michael Angstadt
 * @see "https://platform.openai.com/docs/api-reference"
 */
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
	 * @param apiRequest the request
	 * @return the completion response
	 * @throws OpenAIException if OpenAI returns an error response
	 * @throws IOException if there's a network problem
	 * @see "https://platform.openai.com/docs/api-reference/chat"
	 */
	public ChatCompletionResponse chatCompletion(ChatCompletionRequest apiRequest) throws IOException {
		HttpPost request = postRequestWithApiKey("/v1/chat/completions");
		request.setEntity(new JsonEntity(toJson(apiRequest)));

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

			return parseChatCompletionResponse(responseBody);
		} catch (IOException e) {
			logError(request, responseStatusCode, responseBody, e);
			throw e;
		}
	}

	/**
	 * Creates an image.
	 * @param model the model to use (e.g. "dall-e-2")
	 * @param size the image size (e.g. "256x256")
	 * @param prompt a description of what the image should look like.
	 * @return the URL to the image. The image will be deleted off their servers
	 * within 5-10 minutes.
	 * @throws OpenAIException if OpenAI returns an error response
	 * @throws IOException if there's a network problem
	 * @see "https://platform.openai.com/docs/api-reference/images/create"
	 */
	public String createImage(String model, String size, String prompt) throws IOException, OpenAIException {
		HttpPost request = postRequestWithApiKey("/v1/images/generations");

		//@formatter:off
		request.setEntity(new JsonEntity(JsonUtils.newObject()
			.put("model", model)
			.put("prompt", prompt)
			.put("size", size)
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

			return extractJsonField("data/0/url", responseBody);
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
	 * @throws IOException if there's a network problem
	 * @throws IllegalArgumentException if the given URL is invalid
	 * @see "https://platform.openai.com/docs/api-reference/images/createVariation"
	 */
	public String createImageVariation(String url) throws IllegalArgumentException, IOException, OpenAIException {
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

				return extractJsonField("data/0/url", responseBody);
			} catch (IOException e) {
				logError(request, responseStatusCode, responseBody, e);
				throw e;
			}
		}
	}

	/**
	 * Generates audio from input text.
	 * @param apiRequest the request
	 * @return the generated audio data
	 * @throws IOException if there's a network problem
	 * @throws OpenAIException if an error response is returned
	 */
	public byte[] createSpeech(CreateSpeechRequest apiRequest) throws IOException, OpenAIException {
		HttpPost request = postRequestWithApiKey("/v1/audio/speech");

		ObjectNode node = JsonUtils.newObject();
		node.put("model", apiRequest.getModel());
		node.put("input", apiRequest.getInput());
		node.put("voice", apiRequest.getVoice());
		if (apiRequest.getResponseFormat() != null) {
			node.put("response_format", apiRequest.getResponseFormat());
		}
		if (apiRequest.getSpeed() != null) {
			node.put("speed", apiRequest.getSpeed());
		}

		request.setEntity(new JsonEntity(node));

		logRequest(request);

		JsonNode responseBody = null;
		int responseStatusCode = 0;
		try (CloseableHttpClient client = HttpFactory.connect().getClient()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				if (responseStatusCode == 200) {
					return EntityUtils.toByteArray(response.getEntity());
				}

				try (InputStream in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}
			}

			logResponse(responseStatusCode, responseBody);

			lookForError(responseBody);

			return null;
		} catch (IOException e) {
			logError(request, responseStatusCode, responseBody, e);
			throw e;
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

	private String extractJsonField(String path, JsonNode node) throws IOException {
		try {
			return JsonUtils.extractField(path, node);
		} catch (IllegalArgumentException e) {
			throw new IOException(e);
		}
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

			HttpEntity entity = response.getEntity();
			byte[] origData = EntityUtils.toByteArray(entity);

			if (isJpegOrGif(entity)) {
				byte[] pngData = convertToPng(origData);
				if (pngData != null) {
					return pngData;
				}
				logger.warning(() -> "Unable to convert non-PNG image to PNG: " + url);
			}

			return origData;
		}
	}

	private boolean isJpegOrGif(HttpEntity entity) {
		Header header = entity.getContentType();
		if (header == null) {
			return false;
		}

		String contentType = header.getValue();
		if (contentType == null) {
			return false;
		}

		/**
		 * Use "startsWith" because the header value can contain extra data on
		 * the end (e.g. "image/gif;encoding=utf-8")
		 */
		return contentType.startsWith("image/jpeg") || contentType.startsWith("image/gif");
	}

	private byte[] convertToPng(byte[] data) throws IOException {
		BufferedImage image;
		try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
			image = ImageIO.read(in);
		}
		if (image == null) {
			return null;
		}

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ImageIO.write(image, "PNG", out);
			return out.toByteArray();
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

		String message = error.path("message").asText();
		String type = error.path("type").asText();
		String param = error.path("param").asText();
		String code = error.path("code").asText();

		throw new OpenAIException(message, type, param, code);
	}

	private JsonNode toJson(ChatCompletionRequest apiRequest) {
		ObjectNode node = JsonUtils.newObject();

		node.put("model", apiRequest.getModel());
		putIfNotNull(node, "frequency_penalty", apiRequest.getFrequencyPenalty());
		putIfNotNull(node, "max_tokens", apiRequest.getMaxTokens());
		putIfNotNull(node, "n", apiRequest.getNumCompletionsToGenerate());
		putIfNotNull(node, "presence_penalty", apiRequest.getPresencePenalty());

		if (apiRequest.getResponseFormat() != null) {
			node.set("response_format", node.objectNode().put("type", apiRequest.getResponseFormat()));
		}

		putIfNotNull(node, "seed", apiRequest.getSeed());
		putIfNotNull(node, "temperature", apiRequest.getTemperature());
		putIfNotNull(node, "top_p", apiRequest.getTopP());
		putIfNotNull(node, "user", apiRequest.getUser());

		ArrayNode messagesNode = node.putArray("messages");
		for (ChatCompletionRequest.Message message : apiRequest.getMessages()) {
			ObjectNode messageNode = messagesNode.addObject();
			messageNode.put("role", message.getRole());

			putIfNotNull(messageNode, "name", sanitizeMessageName(message.getName()));

			ArrayNode contentNode = messageNode.putArray("content");

			if (message.getText() != null) {
				//@formatter:off
				contentNode.addObject()
					.put("type", "text")
					.put("text", message.getText());
				//@formatter:on
			}

			for (String imageUrl : message.getImageUrls()) {
				ObjectNode imageContentNode = contentNode.addObject();
				imageContentNode.put("type", "image_url");

				ObjectNode urlNode = imageContentNode.putObject("image_url");
				urlNode.put("url", imageUrl);
				putIfNotNull(urlNode, "detail", message.getImageDetail());
			}
		}

		if (!apiRequest.getStop().isEmpty()) {
			ArrayNode stopNode = node.arrayNode();
			node.set("stop", stopNode);
			apiRequest.getStop().forEach(stopNode::add);
		}

		return node;
	}

	/**
	 * <p>
	 * Sanitize name field.
	 * </p>
	 * <p>
	 * If the name contains unsupported characters, or is too long, an error
	 * response is returned saying that the name must match the pattern
	 * "^[a-zA-Z0-9_-]{1,64}$".
	 * </p>
	 * @param name the name value
	 * @return the sanitized name value or null if the value is null or null if
	 * all characters are invalid
	 */
	private String sanitizeMessageName(String name) {
		if (name == null) {
			return null;
		}

		name = name.replaceAll("[^a-zA-Z0-9_-]", "");
		if (name.isEmpty()) {
			return null;
		}

		int maxLength = 64;
		if (name.length() > maxLength) {
			name = name.substring(0, maxLength);
		}

		return name;
	}

	private ChatCompletionResponse parseChatCompletionResponse(JsonNode node) {
		//@formatter:off
		return new ChatCompletionResponse.Builder()
			.id(node.path("id").asText())
			.created(Instant.ofEpochSecond(node.path("created").asLong()))
			.model(node.path("model").asText())
			.systemFingerprint(node.path("system_fingerprint").asText())
			.promptTokens(node.path("usage").path("prompt_tokens").asInt())
			.completionTokens(node.path("usage").path("completion_tokens").asInt())
			.choices(JsonUtils.stream(node.path("choices"))
				.map(choiceNode -> {
					String content = choiceNode.path("message").path("content").asText();
					String finishReason = choiceNode.path("finish_reason").asText();
					return new ChatCompletionResponse.Choice(content, finishReason);
				})
			.collect(Collectors.toList()))
		.build();
		//@formatter:on
	}

	private static class JsonEntity extends StringEntity {
		private final JsonNode node;

		public JsonEntity(JsonNode node) {
			super(JsonUtils.toString(node), ContentType.APPLICATION_JSON);

			this.node = node;
		}
	}
}
