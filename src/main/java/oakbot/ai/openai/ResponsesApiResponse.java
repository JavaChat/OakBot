package oakbot.ai.openai;

import java.time.Instant;
import java.util.List;

/**
 * A Responses API response.
 * @author Michael Angstadt
 * @see OpenAIClient#responsesApi
 * @see "https://developers.openai.com/api/reference/resources/responses/methods/create"
 */
public class ResponsesApiResponse {
	private final Instant created;
	private final String status;
	private final int inputTokens;
	private final int outputTokens;
	private final List<Output> output;

	public Instant getCreated() {
		return created;
	}

	public String getStatus() {
		return status;
	}

	public int getInputTokens() {
		return inputTokens;
	}

	public int getOutputTokens() {
		return outputTokens;
	}

	public List<Output> getOutput() {
		return output;
	}

	private ResponsesApiResponse(ResponsesApiResponse.Builder builder) {
		created = builder.created;
		status = builder.status;
		inputTokens = builder.inputTokens;
		outputTokens = builder.outputTokens;
		output = List.copyOf(builder.output);
	}

	public static class Builder {
		private Instant created;
		private String status;
		private int inputTokens;
		private int outputTokens;
		private List<Output> output;

		public Builder created(Instant created) {
			this.created = created;
			return this;
		}

		public Builder status(String status) {
			this.status = status;
			return this;
		}

		public Builder inputTokens(int inputTokens) {
			this.inputTokens = inputTokens;
			return this;
		}

		public Builder outputTokens(int outputTokens) {
			this.outputTokens = outputTokens;
			return this;
		}

		public Builder output(List<Output> output) {
			this.output = output;
			return this;
		}

		public ResponsesApiResponse build() {
			return new ResponsesApiResponse(this);
		}
	}

	public static record Output(String status, String content) {
	}
}
