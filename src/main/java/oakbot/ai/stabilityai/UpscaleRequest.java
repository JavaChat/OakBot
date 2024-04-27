package oakbot.ai.stabilityai;

import java.util.Objects;

/**
 * <p>
 * Takes images between 64x64 and 1 megapixel and upscales them all the way to
 * 4K resolution. Put more generally, it can upscale images ~20-40x times while
 * preserving, and often enhancing, quality. Creative Upscale works best on
 * highly degraded images and is not for photos of 1mp or above as it performs
 * heavy reimagining (controlled by creativity scale).
 * </p>
 * <p>
 * Cost: 25 credits
 * </p>
 * @see "https://platform.stability.ai/docs/api-reference#tag/Upscale"
 */
public class UpscaleRequest {
	private final byte[] image;
	private final String imageContentType;
	private final String prompt;
	private final String negativePrompt;
	private final String outputFormat;
	private final Integer seed;
	private final Double creativity;

	private UpscaleRequest(UpscaleRequest.Builder builder) {
		image = Objects.requireNonNull(builder.image);
		imageContentType = Objects.requireNonNull(builder.imageContentType);
		prompt = Objects.requireNonNull(builder.prompt);
		negativePrompt = builder.negativePrompt;
		outputFormat = builder.outputFormat;
		seed = builder.seed;
		creativity = builder.creativity;
	}

	public byte[] getImage() {
		return image;
	}

	public String getImageContentType() {
		return imageContentType;
	}

	public String getPrompt() {
		return prompt;
	}

	public String getNegativePrompt() {
		return negativePrompt;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public Integer getSeed() {
		return seed;
	}

	public Double getCreativity() {
		return creativity;
	}

	public static class Builder {
		private byte[] image;
		private String imageContentType;
		private String prompt;
		private String negativePrompt;
		private String outputFormat;
		private Integer seed;
		private Double creativity;

		/**
		 * <p>
		 * Sets the image you wish to upscale. The total pixel count cannot
		 * exceed 1,048,576 pixels (e.g. 1024x1024, 2048x512, etc.). Every side
		 * must be at least 64 pixels
		 * </p>
		 * <p>
		 * Supported formats: jpeg, png, webp
		 * </p>
		 * @param image the image
		 * @param contentType the content type of the image (e.g. "image/jpeg")
		 * @return this
		 */
		public Builder image(byte[] image, String contentType) {
			this.image = image;
			this.imageContentType = contentType;
			return this;
		}

		/**
		 * <p>
		 * What you wish to see in the output image. A strong, descriptive
		 * prompt that clearly defines elements, colors, and subjects will lead
		 * to better results.
		 * </p>
		 * <p>
		 * To control the weight of a given word use the format (word:weight),
		 * where word is the word you'd like to control the weight of and weight
		 * is a value between 0 and 1. For example: "The sky was a crisp
		 * (blue:0.3) and (green:0.8)" would convey a sky that was blue and
		 * green, but more green than blue.
		 * </p>
		 * @param prompt the prompt
		 * @return this
		 */
		public Builder prompt(String prompt) {
			this.prompt = prompt;
			return this;
		}

		/**
		 * Sets a blurb describing what you do not wish to see in the
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
		 * Indicates how creative the model should be when upscaling an image.
		 * Higher values will result in more details being added to the image
		 * during upscaling.
		 * </p>
		 * <p>
		 * Default value: 0.3
		 * </p>
		 * <p>
		 * Valid values: [ 0 .. 0.35 ]
		 * </p>
		 * @param creativity the creativity value
		 * @return this
		 */
		public Builder creativity(Double creativity) {
			this.creativity = creativity;
			return this;
		}

		public UpscaleRequest build() {
			return new UpscaleRequest(this);
		}
	}
}