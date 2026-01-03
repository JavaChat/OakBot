package oakbot.ai.openai;

import static oakbot.util.JsonUtils.putIfNotNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.util.HttpFactory;
import oakbot.util.HttpRequestLogger;
import oakbot.util.JsonUtils;

/**
 * Client for interacting with OpenAI.
 * @author Michael Angstadt
 * @see "https://platform.openai.com/docs/api-reference"
 */
public class OpenAIClient {
	private static final Logger logger = LoggerFactory.getLogger(OpenAIClient.class);

	private final String apiKey;
	private final HttpRequestLogger requestLogger;

	/**
	 * @param apiKey the API key
	 * @see "https://platform.openai.com/account/api-keys"
	 */
	public OpenAIClient(String apiKey) {
		this(apiKey, null);
	}

	/**
	 * @param apiKey the API key
	 * @param requestLogger logs each request/response (can be null)
	 * @see "https://platform.openai.com/account/api-keys"
	 */
	public OpenAIClient(String apiKey, HttpRequestLogger requestLogger) {
		this.apiKey = apiKey;
		this.requestLogger = requestLogger;
	}

	/**
	 * Lists all available models.
	 * @return the models
	 * @throws OpenAIException if OpenAI returns an error response
	 * @throws IOException if there's a network problem
	 * @see "https://platform.openai.com/docs/api-reference/models/list"
	 */
	public List<OpenAIModel> listModels() throws IOException, OpenAIException {
		var request = getRequestWithApiKey("/v1/models");

		logRequest(request);

		JsonNode responseBody = null;
		var responseStatusCode = 0;
		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				try (var in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}
			}
		} catch (IOException e) {
			logIOException(request, responseStatusCode, responseBody, e);
			throw e;
		} finally {
			logHttpCall(request, responseStatusCode, responseBody);
		}

		logResponse(responseStatusCode, responseBody);

		lookForError(null, responseBody);

		return parseListModelsResponse(responseBody);
	}

	/**
	 * Sends a chat completion request.
	 * @param apiRequest the request
	 * @return the completion response
	 * @throws OpenAIException if OpenAI returns an error response
	 * @throws IOException if there's a network problem
	 * @see "https://platform.openai.com/docs/api-reference/chat"
	 */
	public ChatCompletionResponse chatCompletion(ChatCompletionRequest apiRequest) throws IOException, OpenAIException {
		var request = postRequestWithApiKey("/v1/chat/completions");
		request.setEntity(new JsonEntity(toJson(apiRequest)));

		logRequest(request);

		JsonNode responseBody = null;
		var responseStatusCode = 0;
		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				try (var in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}
			}
		} catch (IOException e) {
			logIOException(request, responseStatusCode, responseBody, e);
			throw e;
		} finally {
			logHttpCall(request, responseStatusCode, responseBody);
		}

		logResponse(responseStatusCode, responseBody);

		lookForError(null, responseBody);

		return parseChatCompletionResponse(responseBody);
	}

	/**
	 * Creates an image.
	 * @param model the model to use (e.g. "dall-e-2")
	 * @param size the image size (e.g. "256x256")
	 * @param outputFormat the image format of the generated image. Only
	 * supported by certain models. Can be null.
	 * @param outputCompression the compression level of the generated image.
	 * Only supported by certain models and output formats. Can be null.
	 * @param prompt a description of what the image should look like
	 * @return the response
	 * @throws OpenAIException if OpenAI returns an error response
	 * @throws IOException if there's a network problem
	 * @see "https://platform.openai.com/docs/api-reference/images/create"
	 */
	public CreateImageResponse createImage(String model, String size, String outputFormat, Integer outputCompression, String prompt) throws IOException, OpenAIException {
		var request = postRequestWithApiKey("/v1/images/generations");

		//@formatter:off
		var jsonObject = JsonUtils.newObject()
			.put("model", model)
			.put("prompt", prompt)
			.put("size", size);
		//@formatter:on

		if (outputFormat != null) {
			jsonObject.put("output_format", outputFormat);
		}

		if (outputCompression != null) {
			jsonObject.put("output_compression", outputCompression);
		}

		request.setEntity(new JsonEntity(jsonObject));

		logRequest(request);

		JsonNode responseBody = null;
		var responseStatusCode = 0;
		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				try (var in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}
			}
		} catch (IOException e) {
			logIOException(request, responseStatusCode, responseBody, e);
			throw e;
		} finally {
			logHttpCall(request, responseStatusCode, responseBody);
		}

		logResponse(responseStatusCode, responseBody);

		lookForError(prompt, responseBody);

		return parseCreateImageResponse(responseBody);
	}

	/**
	 * Creates a variation of the given image.
	 * @param inputImageUrl the URL of the input image
	 * @param size the size of the generated image
	 * @return the response
	 * @throws OpenAIException if OpenAI returns an error response
	 * @throws IOException if there's a network problem
	 * @throws IllegalArgumentException if the given URL is invalid
	 * @see "https://platform.openai.com/docs/api-reference/images/createVariation"
	 */
	public CreateImageResponse createImageVariation(String inputImageUrl, String size) throws IllegalArgumentException, IOException, OpenAIException {
		try (var client = HttpFactory.connect().getClient()) {
			var image = downloadImageAndAttemptToConvertToPng(client, inputImageUrl);

			var request = postRequestWithApiKey("/v1/images/variations");

			//@formatter:off
			request.setEntity(MultipartEntityBuilder.create()
				.addBinaryBody("image", image, ContentType.IMAGE_PNG, "image.png")
				.addTextBody("size", size)
			.build());
			//@formatter:on

			logRequest(request);

			JsonNode responseBody = null;
			var responseStatusCode = 0;

			try (var response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				try (var in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}
			} catch (IOException e) {
				logIOException(request, responseStatusCode, responseBody, e);
				throw e;
			} finally {
				logHttpCall(request, responseStatusCode, responseBody);
			}

			logResponse(responseStatusCode, responseBody);

			lookForError(null, responseBody);

			return parseCreateImageResponse(responseBody);
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
		var request = postRequestWithApiKey("/v1/audio/speech");

		var node = JsonUtils.newObject();
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
		var responseStatusCode = 0;
		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				if (responseStatusCode == 200) {
					return EntityUtils.toByteArray(response.getEntity());
				}

				try (var in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}
			}
		} catch (IOException e) {
			logIOException(request, responseStatusCode, responseBody, e);
			throw e;
		} finally {
			logHttpCall(request, responseStatusCode, responseBody);
		}

		logResponse(responseStatusCode, responseBody);

		lookForError(apiRequest.getInput(), responseBody);

		return null;
	}

	/**
	 * Classifies if the given text is potentially harmful.
	 * @param input the input text to classify
	 * @return the moderation response
	 * @throws IOException if there's a network problem
	 * @throws OpenAIException if an error response is returned
	 * @see "https://platform.openai.com/docs/api-reference/moderations/create"
	 */
	public ModerationResponse moderate(String input) throws IOException, OpenAIException {
		return moderate(input, null);
	}

	/**
	 * Classifies if the given text is potentially harmful.
	 * @param input the input text to classify
	 * @param model the model to use or null to use the default model
	 * @return the moderation response
	 * @throws IOException if there's a network problem
	 * @throws OpenAIException if an error response is returned
	 * @see "https://platform.openai.com/docs/api-reference/moderations/create"
	 */
	public ModerationResponse moderate(String input, String model) throws IOException, OpenAIException {
		var request = postRequestWithApiKey("/v1/moderations");

		var node = JsonUtils.newObject();
		node.put("input", input);
		putIfNotNull(node, "model", model);

		request.setEntity(new JsonEntity(node));

		logRequest(request);

		JsonNode responseBody = null;
		var responseStatusCode = 0;
		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				responseStatusCode = response.getStatusLine().getStatusCode();
				try (var in = response.getEntity().getContent()) {
					responseBody = JsonUtils.parse(in);
				}
			}
		} catch (IOException e) {
			logIOException(request, responseStatusCode, responseBody, e);
			throw e;
		} finally {
			logHttpCall(request, responseStatusCode, responseBody);
		}

		logResponse(responseStatusCode, responseBody);

		lookForError(input, responseBody);

		return parseModerationResponse(responseBody);
	}

	private void logRequest(HttpUriRequest request) {
		logger.atDebug().log(() -> {
			var sb = new StringBuilder();
			sb.append("Sending ").append(request.getMethod()).append(" request to OpenAI:");
			sb.append("\nURI: ").append(request.getURI());

			if (request instanceof HttpEntityEnclosingRequest entityRequest) {
				if (entityRequest.getEntity() instanceof JsonEntity entity) {
					sb.append("\nBody: ").append(JsonUtils.prettyPrint(entity.node));
				}
			}

			return sb.toString();
		});
	}

	private void logResponse(int statusCode, JsonNode body) {
		logger.atDebug().log(() -> "Response from OpenAI: HTTP " + statusCode + ": " + JsonUtils.prettyPrint(body));
	}

	private void logIOException(HttpUriRequest request, int responseStatusCode, JsonNode responseBody, IOException e) {
		logger.atError().setCause(e).log(() -> {
			var sb = new StringBuilder();
			sb.append("Problem communicating with OpenAI.");
			sb.append("\nRequest: ").append(request.getURI());

			if (request instanceof HttpEntityEnclosingRequest entityRequest) {
				if (entityRequest.getEntity() instanceof JsonEntity entity) {
					sb.append(": ").append(JsonUtils.prettyPrint(entity.node));
				}
			}

			if (responseBody != null) {
				var responseBodyStr = JsonUtils.prettyPrint(responseBody);
				sb.append("\nResponse (HTTP ").append(responseStatusCode).append("): ").append(responseBodyStr);
			}

			return sb.toString();
		});
	}

	private void logHttpCall(HttpUriRequest request, int responseStatusCode, JsonNode responseBodyJson) {
		if (requestLogger == null) {
			return;
		}

		var requestMethod = request.getMethod();
		var requestUrl = request.getURI().toString();

		String requestBody;
		if (request instanceof HttpEntityEnclosingRequest entityRequest && entityRequest.getEntity() instanceof JsonEntity entity) {
			requestBody = JsonUtils.prettyPrint(entity.node);
		} else {
			requestBody = "";
		}

		String responseBody;
		if (responseStatusCode == 0) {
			responseBody = "error sending request";
		} else {
			responseBody = (responseBodyJson == null) ? "could not parse response body as JSON" : JsonUtils.prettyPrint(responseBodyJson);
		}

		try {
			requestLogger.log(requestMethod, requestUrl, requestBody, responseStatusCode, responseBody);
		} catch (IOException ignore) {
		}
	}

	private HttpGet getRequestWithApiKey(String uriPath) {
		var request = new HttpGet(buildUri(uriPath));
		setAuthorizationHeader(request);
		return request;
	}

	private HttpPost postRequestWithApiKey(String uriPath) {
		var request = new HttpPost(buildUri(uriPath));
		setAuthorizationHeader(request);
		return request;
	}

	private void setAuthorizationHeader(HttpRequest request) {
		request.setHeader("Authorization", "Bearer " + apiKey);
	}

	private String buildUri(String path) {
		return "https://api.openai.com" + path;
	}

	private byte[] downloadImageAndAttemptToConvertToPng(CloseableHttpClient client, String url) throws IOException {
		var request = new HttpGet(url);

		try (var response = client.execute(request)) {
			var status = response.getStatusLine().getStatusCode();
			if (status != 200) {
				throw new IOException("Image URL returned HTTP " + status + ".");
			}

			var entity = response.getEntity();
			var origData = EntityUtils.toByteArray(entity);

			if (isJpegOrGif(entity)) {
				var pngData = convertToPng(origData);
				if (pngData != null) {
					return pngData;
				}
				logger.atWarn().log(() -> "Unable to convert non-PNG image to PNG: " + url);
			}

			return origData;
		}
	}

	private boolean isJpegOrGif(HttpEntity entity) {
		var header = entity.getContentType();
		if (header == null) {
			return false;
		}

		var contentType = header.getValue();
		if (contentType == null) {
			return false;
		}

		/*
		 * Use "startsWith" because the header value can contain extra data on
		 * the end (e.g. "image/gif;encoding=utf-8")
		 */
		return contentType.startsWith("image/jpeg") || contentType.startsWith("image/gif");
	}

	private byte[] convertToPng(byte[] data) throws IOException {
		BufferedImage image;
		try (var in = new ByteArrayInputStream(data)) {
			image = ImageIO.read(in);
		}
		if (image == null) {
			return null;
		}

		try (var out = new ByteArrayOutputStream()) {
			ImageIO.write(image, "PNG", out);
			return out.toByteArray();
		}
	}

	/**
	 * Throws an exception if there is an error in the given response from the
	 * OpenAI API.
	 * @param prompt the prompt or input text the user entered, or null if not
	 * applicable
	 * @param response the OpenAI API response
	 * @throws OpenAIException if there is an error in the given response
	 */
	private void lookForError(String prompt, JsonNode response) throws OpenAIException {
		var error = response.get("error");
		if (error == null) {
			return;
		}

		var message = error.path("message").asText();
		var type = error.path("type").asText();
		var param = error.path("param").asText();
		var code = error.path("code").asText();

		/*
		 * If rejected by the moderation system, get the reason(s) why.
		 */
		if (prompt != null && message.contains("Your request was rejected as a result of our safety system.")) {
			Set<String> flaggedCategories;
			try {
				flaggedCategories = moderate(prompt).getFlaggedCategories();
			} catch (Exception e) {
				logger.atWarn().setCause(e).log(() -> "Ignoring failed call to moderation endpoint.");
				flaggedCategories = Set.of();
			}

			throw new OpenAIModerationException(message, type, param, code, flaggedCategories);
		}

		throw new OpenAIException(message, type, param, code);
	}

	private JsonNode toJson(ChatCompletionRequest apiRequest) {
		var node = JsonUtils.newObject();

		node.put("model", apiRequest.getModel());
		putIfNotNull(node, "frequency_penalty", apiRequest.getFrequencyPenalty());
		putIfNotNull(node, "max_completion_tokens", apiRequest.getMaxTokens());
		putIfNotNull(node, "n", apiRequest.getNumCompletionsToGenerate());
		putIfNotNull(node, "presence_penalty", apiRequest.getPresencePenalty());

		if (apiRequest.getResponseFormat() != null) {
			node.set("response_format", node.objectNode().put("type", apiRequest.getResponseFormat()));
		}

		putIfNotNull(node, "seed", apiRequest.getSeed());
		putIfNotNull(node, "temperature", apiRequest.getTemperature());
		putIfNotNull(node, "top_p", apiRequest.getTopP());
		putIfNotNull(node, "user", apiRequest.getUser());
		putIfNotNull(node, "reasoning_effort", apiRequest.getReasoningEffort());

		var messagesNode = node.putArray("messages");
		for (var message : apiRequest.getMessages()) {
			var messageNode = messagesNode.addObject();
			messageNode.put("role", message.getRole());

			putIfNotNull(messageNode, "name", sanitizeMessageName(message.getName()));

			var contentNode = messageNode.putArray("content");

			if (message.getText() != null) {
				//@formatter:off
				contentNode.addObject()
					.put("type", "text")
					.put("text", message.getText());
				//@formatter:on
			}

			for (var imageUrl : message.getImageUrls()) {
				var imageContentNode = contentNode.addObject();
				imageContentNode.put("type", "image_url");

				var urlNode = imageContentNode.putObject("image_url");
				urlNode.put("url", imageUrl);
				putIfNotNull(urlNode, "detail", message.getImageDetail());
			}
		}

		if (!apiRequest.getStop().isEmpty()) {
			var stopNode = node.putArray("stop");
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

		var maxLength = 64;
		if (name.length() > maxLength) {
			name = name.substring(0, maxLength);
		}

		return name;
	}

	private ChatCompletionResponse parseChatCompletionResponse(JsonNode node) {
		//@formatter:off
		return new ChatCompletionResponse.Builder()
			.id(node.path("id").asText())
			.created(JsonUtils.asEpochSecond(node.path("created")))
			.model(node.path("model").asText())
			.systemFingerprint(node.path("system_fingerprint").asText())
			.promptTokens(node.path("usage").path("prompt_tokens").asInt())
			.completionTokens(node.path("usage").path("completion_tokens").asInt())
			.choices(JsonUtils.streamArray(node.path("choices"))
				.map(choiceNode -> {
					var content = choiceNode.path("message").path("content").asText();
					var finishReason = choiceNode.path("finish_reason").asText();
					return new ChatCompletionResponse.Choice(content, finishReason);
				})
				.toList())
		.build();
		//@formatter:on
	}

	private ModerationResponse parseModerationResponse(JsonNode node) {
		var results = node.path("results").path(0);

		//@formatter:off
		return new ModerationResponse.Builder()
			.id(node.path("id").asText())
			.model(node.path("model").asText())
			.flaggedCategories(JsonUtils.streamObject(results.path("categories"))
				.filter(entry -> entry.getValue().asBoolean())
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet()))
			.categoryScores(JsonUtils.streamObject(results.path("category_scores"))
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().asDouble())))
		.build();
		//@formatter:on
	}

	private CreateImageResponse parseCreateImageResponse(JsonNode node) {
		var created = JsonUtils.asEpochSecond(node.path("created"));

		var data = node.path("data").path(0);

		var urlNode = data.get("url");
		var url = (urlNode == null) ? null : urlNode.asText();

		var b64Node = data.get("b64_json");
		var imageData = (b64Node == null) ? null : Base64.getDecoder().decode(b64Node.asText());

		var revisedPromptNode = data.get("revised_prompt");
		var revisedPrompt = (revisedPromptNode == null) ? null : revisedPromptNode.asText();

		return new CreateImageResponse(created, url, imageData, revisedPrompt);
	}

	private List<OpenAIModel> parseListModelsResponse(JsonNode node) {
		return JsonUtils.streamArray(node.path("data")).map(n -> {
			var id = n.path("id").asText();
			var created = JsonUtils.asEpochSecond(n.path("created"));
			var owner = n.path("owned_by").asText();
			return new OpenAIModel(id, created, owner);
		}).toList();
	}

	private static class JsonEntity extends StringEntity {
		private final JsonNode node;

		public JsonEntity(JsonNode node) {
			super(JsonUtils.toString(node), ContentType.APPLICATION_JSON);

			this.node = node;
		}
	}
}
