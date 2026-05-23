package oakbot.ai.openai;

import java.util.Objects;

/**
 * Request for generating an image using the newer GPT image models. It excludes
 * certain parameters that are only supported by the dall-e-2 and dall-e-3
 * models. OpenAI got rid of these models.
 * @author Michael Angstadt
 * @see "https://developers.openai.com/api/reference/resources/images/methods/generate"
 */
public class CreateGptImageRequest {
	private final String prompt;
	private final String backgroundTransparency;
	private final String model;
	private final String moderation;
	private final Integer numberOfImagesToGenerate;
	private final Integer outputCompression;
	private final String outputFormat;
	private final String quality;
	private final String size;
	private final String user;

	private CreateGptImageRequest(Builder builder) {
		prompt = Objects.requireNonNull(builder.prompt);
		backgroundTransparency = builder.backgroundTransparency;
		model = builder.model;
		moderation = builder.moderation;
		numberOfImagesToGenerate = builder.numberOfImagesToGenerate;
		outputCompression = builder.outputCompression;
		outputFormat = builder.outputFormat;
		quality = builder.quality;
		size = builder.size;
		user = builder.user;
	}

	public String getPrompt() {
		return prompt;
	}

	public String getBackgroundTransparency() {
		return backgroundTransparency;
	}

	public String getModel() {
		return model;
	}

	public String getModeration() {
		return moderation;
	}

	public Integer getNumberOfImagesToGenerate() {
		return numberOfImagesToGenerate;
	}

	public Integer getOutputCompression() {
		return outputCompression;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public String getQuality() {
		return quality;
	}

	public String getSize() {
		return size;
	}

	public String getUser() {
		return user;
	}

	public static class Builder {
		private String prompt;
		private String backgroundTransparency;
		private String model;
		private String moderation;
		private Integer numberOfImagesToGenerate;
		private Integer outputCompression;
		private String outputFormat;
		private String quality;
		private String size;
		private String user;

		public Builder prompt(String prompt) {
			this.prompt = prompt;
			return this;
		}

		public Builder backgroundTransparency(String backgroundTransparency) {
			this.backgroundTransparency = backgroundTransparency;
			return this;
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		/**
		 * low, auto
		 */
		public Builder moderation(String moderation) {
			this.moderation = moderation;
			return this;
		}

		public Builder numberOfImagesToGenerate(Integer numberOfImagesToGenerate) {
			this.numberOfImagesToGenerate = numberOfImagesToGenerate;
			return this;
		}

		public Builder outputCompression(Integer outputCompression) {
			this.outputCompression = outputCompression;
			return this;
		}

		public Builder outputFormat(String outputFormat) {
			this.outputFormat = outputFormat;
			return this;
		}

		/**
		 * high, medium, low, auto
		 */
		public Builder quality(String quality) {
			this.quality = quality;
			return this;
		}

		public Builder size(String size) {
			this.size = size;
			return this;
		}

		public Builder user(String user) {
			this.user = user;
			return this;
		}

		public CreateGptImageRequest build() {
			return new CreateGptImageRequest(this);
		}
	}
}
