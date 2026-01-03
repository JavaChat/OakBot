package oakbot.ai.stabilityai;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangstadt.sochat4j.util.Sleeper;

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
	 * Our primary service for text-to-image generation, Stable Image Core
	 * represents the best quality achievable at high speed. No prompt
	 * engineering is required! Try asking for a style, a scene, or a character,
	 * and see what you get.
	 * @param apiRequest the request
	 * @return the response
	 * @throws IOException if there's a network problem
	 * @throws StabilityAIException if an error response is returned
	 */
	public StableImageResponse generateImage(StableImageCoreRequest apiRequest) throws IOException, StabilityAIException {
		var request = postRequestWithApiKey("/stable-image/generate/core");
		acceptImageResponse(request);

		var builder = MultipartEntityBuilder.create();

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

		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				var statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 200) {
					return parseStableImageResponse(response);
				}

				throw parseErrorException(response);
			}
		}
	}

	/**
	 * Generate images using Stable Diffusion 3.0 (SD3) or Stable Diffusion 3.0
	 * Turbo (SD3 Turbo), using either a prompt (text-to-image) or a image +
	 * prompt (image-to-image) as the input.
	 * @param apiRequest the request
	 * @return the response
	 * @throws IOException if there's a network problem
	 * @throws StabilityAIException if an error response is returned
	 */
	public StableImageResponse generateImage(StableImageDiffusionRequest apiRequest) throws IOException, StabilityAIException {
		var request = postRequestWithApiKey("/stable-image/generate/sd3");
		acceptImageResponse(request);

		var builder = MultipartEntityBuilder.create();

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

		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				var statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 200) {
					return parseStableImageResponse(response);
				}

				throw parseErrorException(response);
			}
		}
	}

	/**
	 * Segments the foreground from an image and implements and removes the
	 * background.
	 * @param apiRequest the request
	 * @return the response
	 * @throws IOException if there's a network problem
	 * @throws StabilityAIException if an error response is returned
	 */
	public StableImageResponse removeBackground(RemoveBackgroundRequest apiRequest) throws IOException, StabilityAIException {
		var request = postRequestWithApiKey("/stable-image/edit/remove-background");
		acceptImageResponse(request);

		var builder = MultipartEntityBuilder.create();

		builder.addBinaryBody("image", apiRequest.getImage(), ContentType.parse(apiRequest.getImageContentType()), "image");
		if (apiRequest.getOutputFormat() != null) {
			builder.addTextBody("output_format", apiRequest.getOutputFormat());
		}

		request.setEntity(builder.build());

		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				var statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 200) {
					return parseStableImageResponse(response);
				}

				throw parseErrorException(response);
			}
		}
	}

	/**
	 * <p>
	 * Takes images between 64x64 and 1 megapixel and upscales them all the way
	 * to 4K resolution. Put more generally, it can upscale images ~20-40x times
	 * while preserving, and often enhancing, quality. Creative Upscale works
	 * best on highly degraded images and is not for photos of 1mp or above as
	 * it performs heavy reimagining (controlled by creativity scale).
	 * </p>
	 * <p>
	 * This method blocks until an image has been generated or a timeout has
	 * been reached.
	 * </p>
	 * @param apiRequest the request
	 * @return the response
	 * @throws IOException if there's a network problem
	 * @throws StabilityAIException if an error response is returned
	 * @throws TimeoutException if the job doesn't finish within a certain time
	 * period
	 */
	public StableImageResponse upscaleSync(UpscaleRequest apiRequest) throws IOException, StabilityAIException, TimeoutException {
		var pollingInterval = Duration.ofSeconds(10); //rate-limiting may occur if you poll more than once every 10 seconds
		var maxTries = 12;

		var id = upscaleStart(apiRequest);

		var tries = 0;
		while (true) {
			if (tries == maxTries) {
				var seconds = pollingInterval.multipliedBy(maxTries).getSeconds();
				throw new TimeoutException("Image " + id + " still not ready after " + seconds + " seconds.");
			}

			Sleeper.sleep(pollingInterval);

			var response = upscaleFetch(id);
			if (response.isPresent()) {
				return response.get();
			}

			tries++;
		}
	}

	/**
	 * Takes images between 64x64 and 1 megapixel and upscales them all the way
	 * to 4K resolution. Put more generally, it can upscale images ~20-40x times
	 * while preserving, and often enhancing, quality. Creative Upscale works
	 * best on highly degraded images and is not for photos of 1mp or above as
	 * it performs heavy reimagining (controlled by creativity scale).
	 * @param apiRequest the request
	 * @return the job ID (use {@link #upscaleFetch} to fetch the result)
	 * @throws IOException if there's a network problem
	 * @throws StabilityAIException if an error response is returned
	 */
	public String upscaleStart(UpscaleRequest apiRequest) throws IOException, StabilityAIException {
		var request = postRequestWithApiKey("/stable-image/upscale/creative");

		var builder = MultipartEntityBuilder.create();

		builder.addBinaryBody("image", apiRequest.getImage(), ContentType.parse(apiRequest.getImageContentType()), "image");
		builder.addTextBody("prompt", apiRequest.getPrompt());
		if (apiRequest.getNegativePrompt() != null) {
			builder.addTextBody("negative_prompt", apiRequest.getNegativePrompt());
		}
		if (apiRequest.getOutputFormat() != null) {
			builder.addTextBody("output_format", apiRequest.getOutputFormat());
		}
		if (apiRequest.getSeed() != null) {
			builder.addTextBody("seed", apiRequest.getSeed() + "");
		}
		if (apiRequest.getCreativity() != null) {
			builder.addTextBody("creativity", apiRequest.getCreativity() + "");
		}

		request.setEntity(builder.build());

		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				var body = parseJsonResponse(response);

				var statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 200) {
					return body.path("id").asText();
				}

				throw parseErrorException(statusCode, body);
			}
		}
	}

	/**
	 * Checks on the status of an image upscale job.
	 * @param id the job ID
	 * @return the generated image or empty if the image isn't ready yet
	 * @throws IOException if there's a network problem
	 * @throws StabilityAIException if an error response is returned
	 */
	public Optional<StableImageResponse> upscaleFetch(String id) throws IOException, StabilityAIException {
		var request = getRequestWithApiKey("/stable-image/upscale/creative/result/" + id);
		acceptImageResponse(request);

		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				var statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 200) {
					/*
					 * Job done.
					 */
					return Optional.of(parseStableImageResponse(response));
				}

				if (statusCode == 202) {
					/*
					 * Request still in progress.
					 */
					return Optional.empty();
				}

				/*
				 * Parse error response and throw exception.
				 */
				throw parseErrorException(response);
			}
		}
	}

	/**
	 * <p>
	 * Generate a short video based on an initial image with Stable Video
	 * Diffusion, a latent video diffusion model.
	 * </p>
	 * <p>
	 * This method blocks until a video has been generated or a timeout has been
	 * reached.
	 * </p>
	 * @param apiRequest the request
	 * @return the response
	 * @throws IOException if there's a network problem
	 * @throws StabilityAIException if an error response is returned
	 * @throws TimeoutException if the job doesn't finish within a certain time
	 * period
	 */
	public StableImageResponse videoSync(VideoRequest apiRequest) throws IOException, StabilityAIException, TimeoutException {
		var pollingInterval = Duration.ofSeconds(10); //rate-limiting may occur if you poll more than once every 10 seconds
		var maxTries = 30;

		var id = videoStart(apiRequest);

		var tries = 0;
		while (true) {
			if (tries == maxTries) {
				var seconds = pollingInterval.multipliedBy(maxTries).getSeconds();
				throw new TimeoutException("Video " + id + " still not ready after " + seconds + " seconds.");
			}

			Sleeper.sleep(pollingInterval);

			var response = videoFetch(id);
			if (response.isPresent()) {
				return response.get();
			}

			tries++;
		}
	}

	/**
	 * Generate a short video based on an initial image with Stable Video
	 * Diffusion, a latent video diffusion model.
	 * @param apiRequest the request
	 * @return the job ID (use {@link #videoFetch} to fetch the result)
	 * @throws IOException if there's a network problem
	 * @throws StabilityAIException if an error response is returned
	 */
	public String videoStart(VideoRequest apiRequest) throws IOException, StabilityAIException {
		var request = postRequestWithApiKey("/image-to-video");

		var builder = MultipartEntityBuilder.create();

		builder.addBinaryBody("image", apiRequest.getImage(), ContentType.parse(apiRequest.getImageContentType()), "image");
		if (apiRequest.getCfgScale() != null) {
			builder.addTextBody("cfg_scale", apiRequest.getCfgScale() + "");
		}
		if (apiRequest.getMotionBucketId() != null) {
			builder.addTextBody("motion_bucket_id", apiRequest.getMotionBucketId() + "");
		}
		if (apiRequest.getSeed() != null) {
			builder.addTextBody("seed", apiRequest.getSeed() + "");
		}

		request.setEntity(builder.build());

		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				var body = parseJsonResponse(response);

				var statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 200) {
					return body.path("id").asText();
				}

				throw parseErrorException(statusCode, body);
			}
		}
	}

	/**
	 * Checks on the status of a video job.
	 * @param id the job ID
	 * @return the generated video or empty if the video isn't ready yet
	 * @throws IOException if there's a network problem
	 * @throws StabilityAIException if an error response is returned
	 */
	public Optional<StableImageResponse> videoFetch(String id) throws IOException, StabilityAIException {
		var request = getRequestWithApiKey("/image-to-video/result/" + id);
		acceptVideoResponse(request);

		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				var statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 200) {
					/*
					 * Job done.
					 */
					return Optional.of(parseStableImageResponse(response));
				}

				if (statusCode == 202) {
					/*
					 * Request still in progress.
					 */
					return Optional.empty();
				}

				/*
				 * Parse error response and throw exception.
				 */
				throw parseErrorException(response);
			}
		}
	}

	private JsonNode parseJsonResponse(CloseableHttpResponse response) throws IOException {
		try (var in = response.getEntity().getContent()) {
			return JsonUtils.parse(in);
		}
	}

	private StabilityAIException parseErrorException(int statusCode, JsonNode body) {
		var name = body.path("name").asText();

		//@formatter:off
		var errors = JsonUtils.streamArray(body.path("errors"))
			.map(JsonNode::asText)
		.toList();
		//@formatter:on

		return new StabilityAIException(statusCode, name, errors);
	}

	private StabilityAIException parseErrorException(CloseableHttpResponse response) throws IOException {
		var statusCode = response.getStatusLine().getStatusCode();
		var body = parseJsonResponse(response);
		return parseErrorException(statusCode, body);
	}

	private StableImageResponse parseStableImageResponse(CloseableHttpResponse response) throws IOException {
		var contentType = getHeaderValue(response, "content-type");
		var finishReason = getHeaderValue(response, "finish-reason");
		var seed = getHeaderValueInt(response, "seed");
		var image = EntityUtils.toByteArray(response.getEntity());

		return new StableImageResponse(image, contentType, finishReason, seed);
	}

	private String getHeaderValue(CloseableHttpResponse response, String name) {
		var header = response.getFirstHeader(name);
		return (header == null) ? null : header.getValue();
	}

	private int getHeaderValueInt(CloseableHttpResponse response, String name) {
		var value = getHeaderValue(response, name);
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
		var request = new HttpPost("https://api.stability.ai/v2beta" + uriPath);
		setApiKey(request);
		return request;
	}

	private HttpGet getRequestWithApiKey(String uriPath) {
		var request = new HttpGet("https://api.stability.ai/v2beta" + uriPath);
		setApiKey(request);
		return request;
	}

	private void setApiKey(HttpRequest request) {
		request.setHeader("Authorization", "Bearer " + apiKey);
	}

	private void acceptImageResponse(HttpRequest request) {
		request.setHeader("Accept", "image/*");
	}

	private void acceptVideoResponse(HttpRequest request) {
		request.setHeader("Accept", "video/*");
	}
}
