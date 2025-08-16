package oakbot.ai.openai;

import java.util.List;
import java.util.Objects;

/**
 * Allows the user to have a text-based conversation.
 * @see "https://platform.openai.com/docs/api-reference/chat/create"
 */
public class ChatCompletionRequest {
	private final List<Message> messages;
	private final String model;
	private final Double frequencyPenalty;
	//private final Object logitBias;
	//private final Boolean logprobs;
	//private final Integer topLogprobs;
	private Integer maxTokens;
	private final Integer numCompletionsToGenerate;
	private final Double presencePenalty;
	private final String responseFormat;
	private final Integer seed;
	private final List<String> stop;
	//private final Boolean stream;
	private final Double temperature;
	private final Double topP;
	//private final List<Object> tools;
	//private final Object toolChoice;
	private final String user;

	private ChatCompletionRequest(ChatCompletionRequest.Builder builder) {
		messages = List.copyOf(builder.messages);
		model = Objects.requireNonNull(builder.model);
		frequencyPenalty = builder.frequencyPenalty;
		maxTokens = builder.maxTokens;
		numCompletionsToGenerate = builder.numCompletionsToGenerate;
		presencePenalty = builder.presencePenalty;
		responseFormat = builder.responseFormat;
		seed = builder.seed;
		stop = (builder.stop == null) ? List.of() : List.copyOf(builder.stop);
		temperature = builder.temperature;
		topP = builder.topP;
		user = builder.user;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public String getModel() {
		return model;
	}

	public Double getFrequencyPenalty() {
		return frequencyPenalty;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(int maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Integer getNumCompletionsToGenerate() {
		return numCompletionsToGenerate;
	}

	public Double getPresencePenalty() {
		return presencePenalty;
	}

	public String getResponseFormat() {
		return responseFormat;
	}

	public Integer getSeed() {
		return seed;
	}

	public List<String> getStop() {
		return stop;
	}

	public Double getTemperature() {
		return temperature;
	}

	public Double getTopP() {
		return topP;
	}

	public String getUser() {
		return user;
	}

	public static class Builder {
		private List<Message> messages;
		private String model;
		private Double frequencyPenalty;
		private Integer maxTokens;
		private Integer numCompletionsToGenerate;
		private Double presencePenalty;
		private String responseFormat;
		private Integer seed;
		private List<String> stop;
		private Double temperature;
		private Double topP;
		private String user;

		/**
		 * <p>
		 * Sets a list of messages comprising the conversation so far.
		 * </p>
		 * <p>
		 * Required. The first message must have a "system" role (prompt).
		 * </p>
		 * @param messages the messages
		 * @return this
		 */
		public Builder messages(List<Message> messages) {
			this.messages = messages;
			return this;
		}

		/**
		 * <p>
		 * Gets the ID of the model to use.
		 * </p>
		 * <p>
		 * Required.
		 * </p>
		 * @param model the model
		 * @return this
		 * @see <a href=
		 * "https://platform.openai.com/docs/models/model-endpoint-compatibility">Supported
		 * models</a>
		 */
		public Builder model(String model) {
			this.model = model;
			return this;
		}

		/**
		 * <p>
		 * Sets how likely the model will repeat the same content. Positive
		 * values penalize new tokens based on their existing frequency in the
		 * text so far, decreasing the model's likelihood to repeat the same
		 * line verbatim.
		 * </p>
		 * <p>
		 * Default value: 0.0
		 * </p>
		 * <p>
		 * Supported values: [ -2.0 .. 2.0 ]
		 * </p>
		 * @param frequencyPenalty the frequency penalty
		 * @return this
		 * @see "https://platform.openai.com/docs/guides/text-generation/parameter-details"
		 */
		public Builder frequencyPenalty(Double frequencyPenalty) {
			this.frequencyPenalty = frequencyPenalty;
			return this;
		}

		/**
		 * <p>
		 * Sets the maximum number of tokens that can be generated in the chat
		 * completion.
		 * </p>
		 * <p>
		 * Default value: unlimited
		 * </p>
		 * @param maxTokens the maximum number of tokens
		 * @return this
		 */
		public Builder maxTokens(Integer maxTokens) {
			this.maxTokens = maxTokens;
			return this;
		}

		/**
		 * <p>
		 * How many chat completion choices to generate for each input message.
		 * Note that you will be charged based on the number of generated tokens
		 * across all of the choices. Keep n as 1 to minimize costs.
		 * </p>
		 * <p>
		 * Default value: 1
		 * </p>
		 * @param numCompletionsToGenerate the number of completions to generate
		 * @return this
		 */
		public Builder numCompletionsToGenerate(Integer numCompletionsToGenerate) {
			this.numCompletionsToGenerate = numCompletionsToGenerate;
			return this;
		}

		/**
		 * <p>
		 * Sets how likely the model is to talk about new topics. Positive
		 * values penalize new tokens based on whether they appear in the text
		 * so far, increasing the model's likelihood to talk about new topics.
		 * </p>
		 * <p>
		 * Default value: 0.0
		 * </p>
		 * <p>
		 * Supported values: [ -2.0 .. 2.0 ]
		 * </p>
		 * @param presencePenalty the presence penalty
		 * @return this
		 * @see "https://platform.openai.com/docs/guides/text-generation/parameter-details"
		 */
		public Builder presencePenalty(Double presencePenalty) {
			this.presencePenalty = presencePenalty;
			return this;
		}

		/**
		 * <p>
		 * Sets the format that the model will output. Compatible with GPT-4
		 * Turbo and all GPT-3.5 Turbo models newer than gpt-3.5-turbo-1106.
		 * </p>
		 * <p>
		 * Important: When using JSON mode, you must also instruct the model to
		 * produce JSON yourself via a system or user message. Without this, the
		 * model may generate an unending stream of whitespace until the
		 * generation reaches the token limit, resulting in a long-running and
		 * seemingly "stuck" request. Also note that the message content may be
		 * partially cut off if finish_reason="length", which indicates the
		 * generation exceeded max_tokens or the conversation exceeded the max
		 * context length.
		 * </p>
		 * <p>
		 * Default value: text
		 * </p>
		 * <p>
		 * Supported values: json_object, text
		 * </p>
		 * @param responseFormat the response format
		 * @return this
		 */
		public Builder responseFormat(String responseFormat) {
			this.responseFormat = responseFormat;
			return this;
		}

		/**
		 * If specified, our system will make a best effort to sample
		 * deterministically, such that repeated requests with the same seed and
		 * parameters should return the same result. Determinism is not
		 * guaranteed, and you should refer to the system_fingerprint response
		 * parameter to monitor changes in the backend.
		 * @param seed the seed
		 * @return this
		 */
		public Builder seed(Integer seed) {
			this.seed = seed;
			return this;
		}

		/**
		 * Sets up to 4 sequences where the API will stop generating further
		 * tokens.
		 * @param stop the stop sequences
		 * @return this
		 */
		public Builder stop(List<String> stop) {
			this.stop = stop;
			return this;
		}

		/**
		 * <p>
		 * Sets how random the output will be. Higher values like 0.8 will
		 * make the output more random, while lower values like 0.2 will make it
		 * more focused and deterministic. We generally recommend altering this
		 * or {@link #topP} but not both.
		 * </p>
		 * <p>
		 * Default value: 1.0
		 * </p>
		 * <p>
		 * Supported values: [ 0.0 .. 2.0 ]
		 * </p>
		 * @param temperature the temperature
		 * @return this
		 */
		public Builder temperature(Double temperature) {
			this.temperature = temperature;
			return this;
		}

		/**
		 * <p>
		 * An alternative to sampling with temperature, called nucleus sampling,
		 * where the model considers the results of the tokens with top_p
		 * probability mass. So 0.1 means only the tokens comprising the top 10%
		 * probability mass are considered. We generally recommend altering this
		 * or temperature but not both.
		 * </p>
		 * <p>
		 * Default value: 1.0
		 * </p>
		 * @param topP the top_p value
		 * @return this
		 */
		public Builder topP(Double topP) {
			this.topP = topP;
			return this;
		}

		/**
		 * Sets a unique identifier representing your end-user, which can help
		 * OpenAI to monitor and detect abuse.
		 * @param user the unique identifier
		 * @return this
		 * @see "https://platform.openai.com/docs/guides/safety-best-practices/end-user-ids"
		 */
		public Builder user(String user) {
			this.user = user;
			return this;
		}

		public ChatCompletionRequest build() {
			return new ChatCompletionRequest(this);
		}
	}

	public static class Message {
		private final String role;
		private final String text;
		private final List<String> imageUrls;
		private final String imageDetail;
		private final String name;

		public Message(Builder builder) {
			role = Objects.requireNonNull(builder.role);
			text = builder.text;
			imageUrls = (builder.imageUrls == null) ? List.of() : List.copyOf(builder.imageUrls);
			if (text == null && imageUrls.isEmpty()) {
				throw new IllegalArgumentException("No message content provided.");
			}
			imageDetail = builder.imageDetail;
			name = builder.name;
		}

		public String getRole() {
			return role;
		}

		public String getText() {
			return text;
		}

		public List<String> getImageUrls() {
			return imageUrls;
		}

		public String getImageDetail() {
			return imageDetail;
		}

		public String getName() {
			return name;
		}

		public static class Builder {
			private String role;
			private String text;
			private List<String> imageUrls;
			private String imageDetail;
			private String name;

			/**
			 * <p>
			 * Sets the type of user that created the message.
			 * </p>
			 * <p>
			 * Required.
			 * </p>
			 * <p>
			 * Supported values: system (prompt), user (human), assistant (AI),
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
			 * Sets the text content of the message.
			 * @param text the text
			 * @return this
			 */
			public Builder text(String text) {
				this.text = text;
				return this;
			}

			/**
			 * Sets any image content of the message. Only supported by models
			 * that support vision.
			 * @param imageUrls the image URLs
			 * @param detail how closely the model should analyze the images
			 * (low, high, auto)
			 * @return this
			 * @see "https://platform.openai.com/docs/guides/vision/low-or-high-fidelity-image-understanding"
			 */
			public Builder imageUrls(List<String> imageUrls, String detail) {
				this.imageUrls = imageUrls;
				this.imageDetail = detail;
				return this;
			}

			/**
			 * Sets the name of the participant that created the message.
			 * Provides the model information to differentiate between
			 * participants of the same role.
			 * @param name the name
			 * @return this
			 */
			public Builder name(String name) {
				this.name = name;
				return this;
			}

			public Message build() {
				return new Message(this);
			}
		}
	}
}