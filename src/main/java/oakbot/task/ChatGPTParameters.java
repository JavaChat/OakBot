package oakbot.task;

/**
 * Defines various parameters for sending requests to ChatGPT.
 * @author Michael Angstadt
 */
public class ChatGPTParameters {
	private final String apiKey, prompt;
	private final int maxTokensForCompletion;

	/**
	 * @param apiKey the ChatGPT API key
	 * @param prompt one or more sentences that define the bot's personality
	 * (e.g. "You are a helpful assistant"). This counts against your usage
	 * quota. Each word costs around 1.33 tokens.
	 * @param maxTokensForCompletion places a limit on the length of ChatGPT's
	 * completion (response). If this number is too short, then the completion
	 * will end abruptly (e.g. in an unfinished sentence). Each word costs
	 * around 1.33 tokens.
	 * @see <a href="https://platform.openai.com/account/api-keys">API keys</a>
	 */
	public ChatGPTParameters(String apiKey, String prompt, int maxTokensForCompletion) {
		this.apiKey = apiKey;
		this.prompt = prompt;
		this.maxTokensForCompletion = maxTokensForCompletion;
	}

	/**
	 * Gets the API key.
	 * @return the API key
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * <p>
	 * Gets the prompt. The prompt consists of one or more sentences that define
	 * the bot's personality (e.g. "You are a helpful assistant").
	 * </p>
	 * <p>
	 * This counts against your usage quota. Each word costs around 1.33 tokens.
	 * </p>
	 * @return the prompt
	 */
	public String getPrompt() {
		return prompt;
	}

	/**
	 * Gets the max number of tokens the completion can be. If this number is
	 * too short, then the completion will end abruptly (e.g. in an unfinished
	 * sentence). Each word costs around 1.33 tokens.
	 * @return the max number of tokens the completion can be
	 */
	public int getMaxTokensForCompletion() {
		return maxTokensForCompletion;
	}
}
