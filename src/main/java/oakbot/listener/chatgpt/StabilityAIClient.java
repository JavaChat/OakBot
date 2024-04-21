package oakbot.listener.chatgpt;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;

/**
 * @author Michael Angstadt
 * @see "https://platform.stability.ai"
 */
public class StabilityAIClient {
	private final String apiKey;

	/**
	 * @param apiKey the API key
	 * @see "https://platform.stability.ai/account/keys"
	 */
	public StabilityAIClient(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Generates an image using Stable Image Core.
	 * @param apiRequest the request
	 * @return the response
	 * @throws IOException if there's a problem sending the request
	 * @throws StabilityAIException if an error response is returned
	 */
	public StableImageResponse generateImage(StableImageCoreRequest apiRequest) throws IOException, StabilityAIException {
		HttpPost request = postRequestWithApiKey("/core");

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();

		builder.addTextBody("prompt", apiRequest.getPrompt());
		if (apiRequest.getAspectRatio() != null) {
			builder.addTextBody("aspect_ratio", apiRequest.getAspectRatio());
		}
		if (apiRequest.getNegativePrompt() != null) {
			builder.addTextBody("negative_prompt", apiRequest.getNegativePrompt());
		}
		if (apiRequest.getSeed() != null) {
			builder.addTextBody("seed", apiRequest.getSeed() + "");
		}
		if (apiRequest.getStylePreset() != null) {
			builder.addTextBody("style_preset", apiRequest.getStylePreset());
		}
		if (apiRequest.getOutputFormat() != null) {
			builder.addTextBody("output_format", apiRequest.getOutputFormat());
		}

		request.setEntity(builder.build());

		return send(request);
	}

	/**
	 * Generates an image using Stable Diffusion 3.0.
	 * @param apiRequest the request
	 * @return the response
	 * @throws IOException if there's a problem sending the request
	 * @throws StabilityAIException if an error response is returned
	 */
	public StableImageResponse generateImage(StableImageDiffusionRequest apiRequest) throws IOException, StabilityAIException {
		HttpPost request = postRequestWithApiKey("/sd3");

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();

		builder.addTextBody("prompt", apiRequest.getPrompt());
		if (apiRequest.getModel() != null) {
			builder.addTextBody("model", apiRequest.getModel());
		}
		if (apiRequest.getImage() != null) {
			builder.addTextBody("mode", "image-to-image");
			builder.addBinaryBody("image", apiRequest.getImage(), ContentType.parse(apiRequest.getImageContentType()), "image");
			builder.addTextBody("strength", apiRequest.getStrength() + "");
		}
		if (apiRequest.getAspectRatio() != null) {
			builder.addTextBody("aspect_ratio", apiRequest.getAspectRatio());
		}
		if (apiRequest.getNegativePrompt() != null) {
			builder.addTextBody("negative_prompt", apiRequest.getNegativePrompt());
		}
		if (apiRequest.getSeed() != null) {
			builder.addTextBody("seed", apiRequest.getSeed() + "");
		}
		if (apiRequest.getOutputFormat() != null) {
			builder.addTextBody("output_format", apiRequest.getOutputFormat());
		}

		request.setEntity(builder.build());

		return send(request);
	}

	private StableImageResponse send(HttpPost request) throws IOException {
		try (CloseableHttpClient client = HttpFactory.connect().getClient()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				int statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 200) {
					String contentType = getHeaderValue(response, "content-type");
					String finishReason = getHeaderValue(response, "finish-reason");
					int seed = getHeaderValueInt(response, "seed");
					byte[] image = EntityUtils.toByteArray(response.getEntity());

					return new StableImageResponse(image, contentType, finishReason, seed);
				}

				/*
				 * Parse error response and throw exception.
				 */
				JsonNode body;
				try (InputStream in = response.getEntity().getContent()) {
					body = JsonUtils.parse(in);
				}

				String name = body.path("name").asText();

				//@formatter:off
				List<String> errors = StreamSupport.stream(Spliterators.spliteratorUnknownSize(body.path("errors").iterator(), Spliterator.ORDERED), false)
					.map(JsonNode::asText)
				.collect(Collectors.toList());
				//@formatter:on

				throw new StabilityAIException(name, errors);
			}
		}
	}

	private String getHeaderValue(CloseableHttpResponse response, String name) {
		Header header = response.getFirstHeader(name);
		return (header == null) ? null : header.getValue();
	}

	private int getHeaderValueInt(CloseableHttpResponse response, String name) {
		String value = getHeaderValue(response, name);
		if (value == null) {
			return 0;
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private HttpPost postRequestWithApiKey(String uriPath) {
		HttpPost request = new HttpPost("https://api.stability.ai/v2beta/stable-image/generate" + uriPath);
		request.setHeader("Authorization", "Bearer " + apiKey);
		request.setHeader("Accept", "image/*");
		return request;
	}
}
