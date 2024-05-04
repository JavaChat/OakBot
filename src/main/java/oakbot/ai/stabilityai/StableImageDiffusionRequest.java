package oakbot.ai.stabilityai;

import java.util.Objects;

/**
 * <p>
 * Generate images using Stable Diffusion 3.0 (SD3) or Stable Diffusion 3.0
 * Turbo (SD3 Turbo), using either a prompt (text-to-image) or a image + prompt
 * (image-to-image) as the input.
 * </p>
 * <p>
 * Cost: SD3 is 6.5 credits and SD3 Turbo is 4 credits
 * </p>
 * @see "https://platform.stability.ai/docs/api-reference#tag/Generate/paths/~1v2beta~1stable-image~1generate~1sd3/post"
 */
public class StableImageDiffusionRequest {
	private final String prompt;

	private final byte[] image;
	private final String imageContentType;
	private final double strength;

	private final String aspectRatio;
	private final String negativePrompt;
	private final String model;
	private final Integer seed;
	private final String outputFormat;

	private StableImageDiffusionRequest(StableImageDiffusionRequest.Builder builder) {
		prompt = Objects.requireNonNull(builder.prompt);
		image = builder.image;
		imageContentType = builder.imageContentType;
		strength = builder.strength;
		aspectRatio = builder.aspectRatio;
		negativePrompt = builder.negativePrompt;
		model = builder.model;
		seed = builder.seed;
		outputFormat = builder.outputFormat;
	}

	public String getPrompt() {
		return prompt;
	}

	public byte[] getImage() {
		return image;
	}

	public String getImageContentType() {
		return imageContentType;
	}

	public double getStrength() {
		return strength;
	}

	public String getAspectRatio() {
		return aspectRatio;
	}

	public String getNegativePrompt() {
		return negativePrompt;
	}

	public String getModel() {
		return model;
	}

	public Integer getSeed() {
		return seed;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public static class Builder {
		private String prompt;

		private byte[] image;
		private String imageContentType;
		private double strength;

		private String aspectRatio;
		private String negativePrompt;
		private String model;
		private Integer seed;
		private String outputFormat;

		/**
		 * <p>
		 * Sets what you wish to see in the output image. A strong, descriptive
		 * prompt that clearly defines elements, colors, and subjects will lead
		 * to better results.
		 * </p>
		 * @param prompt the prompt
		 * @return this
		 */
		public Builder prompt(String prompt) {
			this.prompt = prompt;
			return this;
		}

		/**
		 * <p>
		 * Sets the image to use as the starting point for the generation.
		 * </p>
		 * <p>
		 * Supported formats: jpeg, png, webp
		 * </p>
		 * @param image the image
		 * @param contentType the content type of the image (e.g. "image/jpeg")
		 * @param strength how much influence the starting image has on the
		 * generated image. A value of 0 would yield an image that is identical
		 * to the input. A value of 1 would be as if you passed in no image at
		 * all.
		 * @return this
		 */
		public Builder image(byte[] image, String contentType, double strength) {
			this.image = image;
			this.imageContentType = contentType;
			this.strength = strength;
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
		 * @param aspectRatio the aspect ratio
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
		 * Sets the model to use for generation.
		 * <p>
		 * Default: sd3
		 * </p>
		 * <p>
		 * Valid values: sd3 (6.5 credits), sd3-turbo (4 credits)
		 * </p>
		 * @param model the model
		 * @return this
		 */
		public Builder model(String model) {
			this.model = model;
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

		public StableImageDiffusionRequest build() {
			return new StableImageDiffusionRequest(this);
		}
	}
}