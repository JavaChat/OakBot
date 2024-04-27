package oakbot.listener.chatgpt;

import java.util.Objects;

/**
 * <p>
 * Generate a short video based on an initial image with Stable Video Diffusion,
 * a latent video diffusion model.
 * </p>
 * <p>
 * Cost: 20 credits.
 * </p>
 * @see "https://platform.stability.ai/docs/api-reference#tag/Image-to-Video"
 */
public class VideoRequest {
	private final byte[] image;
	private final String imageContentType;
	private final Double cfgScale;
	private final Integer motionBucketId;
	private final Integer seed;

	private VideoRequest(VideoRequest.Builder builder) {
		image = Objects.requireNonNull(builder.image);
		imageContentType = Objects.requireNonNull(builder.imageContentType);
		cfgScale = builder.cfgScale;
		motionBucketId = builder.motionBucketId;
		seed = builder.seed;
	}

	public byte[] getImage() {
		return image;
	}

	public String getImageContentType() {
		return imageContentType;
	}

	public Double getCfgScale() {
		return cfgScale;
	}

	public Integer getMotionBucketId() {
		return motionBucketId;
	}

	public Integer getSeed() {
		return seed;
	}

	public static class Builder {
		private byte[] image;
		private String imageContentType;
		private Double cfgScale;
		private Integer motionBucketId;
		private Integer seed;

		/**
		 * <p>
		 * Sets the source image used in the video generation process.
		 * </p>
		 * <p>
		 * Supported formats: jpeg, png
		 * </p>
		 * <p>
		 * Supported dimensions: 1024x576, 576x1024, 768x768
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
		 * Set how strongly the video sticks to the original image. Use lower
		 * values to allow the model more freedom to make changes and higher
		 * values to correct motion distortions.
		 * </p>
		 * <p>
		 * Default value: 1.8
		 * </p>
		 * <p>
		 * Valid values: [ 0 .. 10 ]
		 * </p>
		 * @param cfgScale the value
		 * @return this
		 */
		public Builder cfgScale(Double cfgScale) {
			this.cfgScale = cfgScale;
			return this;
		}

		/**
		 * <p>
		 * Sets how much motion the video should include. Lower values generally
		 * result in less motion in the output video, while higher values
		 * generally result in more motion.
		 * </p>
		 * <p>
		 * Default value: 127
		 * </p>
		 * <p>
		 * Valid values: [ 1 .. 255 ]
		 * </p>
		 * @param motionBucketId the value
		 * @return this
		 */
		public Builder motionBucketId(Integer motionBucketId) {
			this.motionBucketId = motionBucketId;
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

		public VideoRequest build() {
			return new VideoRequest(this);
		}
	}
}