package oakbot.ai.openai;

import java.util.Set;

/**
 * @author Michael Angstadt
 */
public class OpenAIModerationException extends OpenAIException {
	private static final long serialVersionUID = 1L;

	private final Set<String> flaggedCategories;

	public OpenAIModerationException(String message, String type, String param, String code, Set<String> flaggedCategories) {
		super(message, type, param, code);
		this.flaggedCategories = flaggedCategories;
	}

	public Set<String> getFlaggedCategories() {
		return flaggedCategories;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " Flagged categories: " + flaggedCategories;
	}
}
