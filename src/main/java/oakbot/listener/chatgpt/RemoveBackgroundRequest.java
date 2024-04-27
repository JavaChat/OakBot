package oakbot.listener.chatgpt;

import java.util.Objects;

/**
 * <p>
 * The Remove Background service accurately segments the foreground from an
 * image and implements and removes the background.
 * </p>
 * <p>
 * Cost: 2 credits
 * </p>
 * @see "https://platform.stability.ai/docs/api-reference#tag/Edit/paths/~1v2beta~1stable-image~1edit~1remove-background/post"
 */
public class RemoveBackgroundRequest {
	private final byte[] image;
	private final String imageContentType;
	private final String outputFormat;

	private RemoveBackgroundRequest(RemoveBackgroundRequest.Builder builder) {
		image = Objects.requireNonNull(builder.image);
		imageContentType = Objects.requireNonNull(builder.imageContentType);
		outputFormat = builder.outputFormat;
	}

	public byte[] getImage() {
		return image;
	}

	public String getImageContentType() {
		return imageContentType;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public static class Builder {
		private byte[] image;
		private String imageContentType;
		private String outputFormat;

		/**
		 * <p>
		 * Sets the image whose background you wish to remove. Please ensure
		 * that the source image is in the correct format and dimensions.
		 * </p>
		 * <p>
		 * Every side must be at least 64 pixels. The total pixel count cannot
		 * exceed 4,194,304 pixels (e.g. 20482048, 40961024, etc.)
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
		 * Sets the format of the generated image.
		 * </p>
		 * <p>
		 * Default value: png
		 * </p>
		 * <p>
		 * Valid values: png, webp
		 * </p>
		 * @param outputFormat the output format
		 * @return this
		 */
		public Builder outputFormat(String outputFormat) {
			this.outputFormat = outputFormat;
			return this;
		}

		public RemoveBackgroundRequest build() {
			return new RemoveBackgroundRequest(this);
		}
	}
}