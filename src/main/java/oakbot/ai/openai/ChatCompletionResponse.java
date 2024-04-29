package oakbot.ai.openai;

import java.time.Instant;
import java.util.List;

/**
 * A chat completion response.
 * @author Michael Angstadt
 * @see OpenAIClient#chatCompletion
 * @see "https://platform.openai.com/docs/api-reference/chat/object"
 */
public class ChatCompletionResponse {
	private final String id;
	private final Instant created;
	private final String model;
	private final String systemFingerprint;
	private final int promptTokens;
	private final int completionTokens;
	private final List<Choice> choices;

	private ChatCompletionResponse(ChatCompletionResponse.Builder builder) {
		id = builder.id;
		created = builder.created;
		model = builder.model;
		systemFingerprint = builder.systemFingerprint;
		promptTokens = builder.promptTokens;
		completionTokens = builder.completionTokens;
		choices = (builder.choices == null) ? List.of() : List.copyOf(builder.choices);
	}

	/**
	 * Gets the unique identifier for the chat completion.
	 * @return the ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Gets when the chat completion was created.
	 * @return the timestamp
	 */
	public Instant getCreated() {
		return created;
	}

	/**
	 * Gets the model used for the chat completion.
	 * @return the model
	 */
	public String getModel() {
		return model;
	}

	/**
	 * Gets a fingerprint which represents the backend configuration that
	 * the model runs with. Can be used in conjunction with the
	 * {@link ChatCompletionRequest.Builder#seed seed} request parameter
	 * to understand when backend changes have been made that might impact
	 * determinism.
	 * @return the fingerprint
	 */
	public String getSystemFingerprint() {
		return systemFingerprint;
	}

	/**
	 * Gets the number of tokens that the prompt consumed.
	 * @return the number of tokens
	 */
	public int getPromptTokens() {
		return promptTokens;
	}

	/**
	 * Gets the number of tokens that the generated completion consumed.
	 * @return the number of tokens
	 */
	public int getCompletionTokens() {
		return completionTokens;
	}

	/**
	 * Gets the list of chat completion choices. Can be more than one if
	 * {@link ChatCompletionRequest.Builder#numCompletionsToGenerate n}
	 * is greater than 1.
	 * @return the choices
	 * @see ChatCompletionRequest.Builder#numCompletionsToGenerate
	 */
	public List<Choice> getChoices() {
		return choices;
	}

	public static class Builder {
		private String id;
		private Instant created;
		private String model;
		private String systemFingerprint;
		private int promptTokens;
		private int completionTokens;
		private List<Choice> choices;

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder created(Instant created) {
			this.created = created;
			return this;
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder systemFingerprint(String systemFingerprint) {
			this.systemFingerprint = systemFingerprint;
			return this;
		}

		public Builder promptTokens(int promptTokens) {
			this.promptTokens = promptTokens;
			return this;
		}

		public Builder completionTokens(int completionTokens) {
			this.completionTokens = completionTokens;
			return this;
		}

		public Builder choices(List<Choice> choices) {
			this.choices = choices;
			return this;
		}

		public ChatCompletionResponse build() {
			return new ChatCompletionResponse(this);
		}
	}

	public static class Choice {
		private final String content;
		// private final Object logprobs;
		private final String finishReason;

		/**
		 * @param content the content of the message
		 * @param finishReason the reason the model stopped generating tokens
		 */
		public Choice(String content, String finishReason) {
			this.content = content;
			this.finishReason = finishReason;
		}

		/**
		 * Gets the content of the message.
		 * @return the message content
		 */
		public String getContent() {
			return content;
		}

		/**
		 * <p>
		 * Gets the reason the model stopped generating tokens.
		 * </p>
		 * <ul>
		 * <li>"stop": If the model hit a natural stop point or a provided stop
		 * sequence.</li>
		 * <li>"length": If the maximum number of tokens specified in the
		 * request was reached.</li>
		 * <li>"content_filter": If content was omitted due to a
		 * flag from our content filters.</li>
		 * <li>"tool_calls": If the model called a tool.</li>
		 * <li>"function_call" (deprecated): If the model called a function.
		 * </ul>
		 * @return the finish reason
		 */
		public String getFinishReason() {
			return finishReason;
		}
	}
}
