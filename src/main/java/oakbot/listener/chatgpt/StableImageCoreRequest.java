package oakbot.listener.chatgpt;

import java.util.Objects;

/**
 * <p>
 * Our primary service for text-to-image generation, Stable Image Core
 * represents the best quality achievable at high speed. No prompt engineering
 * is required! Try asking for a style, a scene, or a character, and see what
 * you get.
 * </p>
 * <p>
 * Cost: 3 credits
 * </p>
 * @see "https://platform.stability.ai/docs/api-reference#tag/Generate/paths/~1v2beta~1stable-image~1generate~1core/post"
 */
public class StableImageCoreRequest {
	private final String prompt;
	private final String aspectRatio;
	private final String negativePrompt;
	private final Integer seed;
	private final String stylePreset;
	private final String outputFormat;

	private StableImageCoreRequest(StableImageCoreRequest.Builder builder) {
		prompt = Objects.requireNonNull(builder.prompt);
		aspectRatio = builder.aspectRatio;
		negativePrompt = builder.negativePrompt;
		seed = builder.seed;
		stylePreset = builder.stylePreset;
		outputFormat = builder.outputFormat;
	}

	public String getPrompt() {
		return prompt;
	}

	public String getAspectRatio() {
		return aspectRatio;
	}

	public String getNegativePrompt() {
		return negativePrompt;
	}

	public Integer getSeed() {
		return seed;
	}

	public String getStylePreset() {
		return stylePreset;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public static class Builder {
		private String prompt;
		private String aspectRatio;
		private String negativePrompt;
		private Integer seed;
		private String stylePreset;
		private String outputFormat;

		/**
		 * <p>
		 * Sets what you wish to see in the output image. A strong, descriptive
		 * prompt that clearly defines elements, colors, and subjects will lead
		 * to better results.
		 * </p>
		 * <p>
		 * To control the weight of a given word use the format (word:weight),
		 * where word is the word you'd like to control the weight of and weight
		 * is a value between 0 and 1.
		 * </p>
		 * <p>
		 * For example: The sky was a crisp (blue:0.3) and (green:0.8) would
		 * convey a sky that was blue and green, but more green than blue.
		 * </p>
		 * @param prompt the prompt
		 * @return this
		 */
		public StableImageCoreRequest.Builder prompt(String prompt) {
			this.prompt = prompt;
			return this;
		}

		/**
		 * <p>
		 * Sets the aspect ratio of the generated image.
		 * </p>
		 * <p>
		 * Default value: 1:1
		 * </p>
		 * <p>
		 * Valid values: 16:9, 1:1, 21:9, 2:3, 3:2, 4:5, 5:4, 9:16, 9:21
		 * </p>
		 * @param aspectRatio
		 * @return this
		 */
		public Builder aspectRatio(String aspectRatio) {
			this.aspectRatio = aspectRatio;
			return this;
		}

		/**
		 * Sets a blurb of text describing what you do not wish to see in the
		 * output image.
		 * @param negativePrompt the negative prompt
		 * @return this
		 */
		public Builder negativePrompt(String negativePrompt) {
			this.negativePrompt = negativePrompt;
			return this;
		}

		/**
		 * <p>
		 * Sets a specific value that is used to guide the "randomness" of the
		 * generation. Use "0" to generate a random seed.
		 * </p>
		 * <p>
		 * Default value: 0
		 * </p>
		 * <p>
		 * Valid values: [ 0 .. 4294967294 ]
		 * </p>
		 * @param seed the seed
		 * @return this
		 */
		public Builder seed(Integer seed) {
			this.seed = seed;
			return this;
		}

		/**
		 * <p>
		 * Sets the art style of the generated image.
		 * </p>
		 * <p>
		 * Default value: no particular style
		 * </p>
		 * <p>
		 * Valid values: 3d-model, analog-film, anime, cinematic, comic-book,
		 * digital-art, enhance, fantasy-art, isometric, line-art, low-poly,
		 * modeling-compound, neon-punk, origami, photographic, pixel-art,
		 * tile-texture
		 * </p>
		 * @param stylePreset
		 * @return this
		 */
		public Builder stylePreset(String stylePreset) {
			this.stylePreset = stylePreset;
			return this;
		}

		/**
		 * <p>
		 * Sets the format of the generated image.
		 * </p>
		 * <p>
		 * Default value: png
		 * </p>
		 * <p>
		 * Valid values: jpeg, png, webp
		 * </p>
		 * @param outputFormat the output format
		 * @return this
		 */
		public Builder outputFormat(String outputFormat) {
			this.outputFormat = outputFormat;
			return this;
		}

		public StableImageCoreRequest build() {
			return new StableImageCoreRequest(this);
		}
	}
}