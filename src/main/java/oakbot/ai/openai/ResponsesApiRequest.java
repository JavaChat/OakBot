package oakbot.ai.openai;

import java.util.List;
import java.util.Objects;

/**
 * Allows the user to have a text-based conversation.
 * @see "https://developers.openai.com/api/reference/resources/responses/methods/create"
 */
public class ResponsesApiRequest {
	private final List<Input> inputs;
	private final String model;
	private final String instructions;
	private final Integer maxOutputTokens;
	private final String reasoningEffort;
	private final String verbosity;

	private ResponsesApiRequest(ResponsesApiRequest.Builder builder) {
		inputs = List.copyOf(builder.inputs);
		model = builder.model;
		instructions = builder.instructions;
		maxOutputTokens = builder.maxOutputTokens;
		reasoningEffort = builder.reasoningEffort;
		verbosity = builder.verbosity;
	}

	public List<Input> getInputs() {
		return inputs;
	}

	public String getModel() {
		return model;
	}

	public String getInstructions() {
		return instructions;
	}

	public Integer getMaxOutputTokens() {
		return maxOutputTokens;
	}

	public String getReasoningEffort() {
		return reasoningEffort;
	}

	public String getVerbosity() {
		return verbosity;
	}

	public static class Builder {
		private List<Input> inputs;
		private String model;
		private String instructions;
		private Integer maxOutputTokens;
		private String reasoningEffort;
		private String verbosity;

		public Builder inputs(List<Input> inputs) {
			this.inputs = inputs;
			return this;
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder instructions(String instructions) {
			this.instructions = instructions;
			return this;
		}

		public Builder maxOutputTokens(Integer maxOutputTokens) {
			this.maxOutputTokens = maxOutputTokens;
			return this;
		}

		/**
		 * none, low, medium, high, xhigh
		 */
		public Builder reasoningEffort(String reasoningEffort) {
			this.reasoningEffort = reasoningEffort;
			return this;
		}

		/**
		 * low, med, high
		 */
		public Builder verbosity(String verbosity) {
			this.verbosity = verbosity;
			return this;
		}

		public ResponsesApiRequest build() {
			return new ResponsesApiRequest(this);
		}
	}

	public static class Input {
		private final String role;
		private final String text;
		private final String imageUrl;
		private final String imageDetail;

		public Input(Builder builder) {
			role = Objects.requireNonNull(builder.role);
			text = builder.text;
			imageUrl = builder.imageUrl;
			if (text == null && imageUrl == null) {
				throw new IllegalStateException("No message content provided.");
			}
			imageDetail = builder.imageDetail;
		}

		public String getRole() {
			return role;
		}

		public String getText() {
			return text;
		}

		public String getImageUrl() {
			return imageUrl;
		}

		public String getImageDetail() {
			return imageDetail;
		}

		public static class Builder {
			private String role;
			private String text;
			private String imageUrl;
			private String imageDetail;

			/**
			 * <p>
			 * Sets the type of user that created the message.
			 * </p>
			 * <p>
			 * Required.
			 * </p>
			 * <p>
			 * Supported values: developer (prompt), user (human), assistant
			 * (AI),
			 * tool
			 * </p>
			 * @param role the role
			 * @return this
			 */
			public Builder role(String role) {
				this.role = role;
				return this;
			}

			/**
			 * Sets the content of this input to text.
			 * @param text the text
			 * @return this
			 */
			public Builder text(String text) {
				this.text = text;
				return this;
			}

			/**
			 * Sets the content of this input to an image.
			 * @param url the image URL
			 * @param detail how closely the model should analyze the images
			 * (low, high, original, auto)
			 * @return this
			 * @see "https://developers.openai.com/api/docs/guides/images-vision"
			 */
			public Builder image(String url, String detail) {
				this.imageUrl = url;
				this.imageDetail = detail;
				return this;
			}

			public Input build() {
				return new Input(this);
			}
		}
	}
}