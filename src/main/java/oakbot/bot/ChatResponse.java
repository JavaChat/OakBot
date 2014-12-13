package oakbot.bot;

import oakbot.chat.SplitStrategy;

/**
 * A message to send in response to a command.
 * @author Michael Angstadt
 */
public class ChatResponse {
	private final String message;
	private final SplitStrategy splitStrategy;

	public ChatResponse(String message) {
		this(message, SplitStrategy.NONE);
	}

	public ChatResponse(String message, SplitStrategy splitStrategy) {
		this.message = message;
		this.splitStrategy = splitStrategy;
	}

	public String getMessage() {
		return message;
	}

	public SplitStrategy getSplitStrategy() {
		return splitStrategy;
	}
}
