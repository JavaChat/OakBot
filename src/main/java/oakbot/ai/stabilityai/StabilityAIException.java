package oakbot.ai.stabilityai;

import java.util.List;

/**
 * @author Michael Angstadt
 */
public class StabilityAIException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final int statusCode;
	private final String name;
	private final List<String> errors;

	public StabilityAIException(int statusCode, String name, List<String> errors) {
		super("HTTP " + statusCode + ": " + name + ": " + errors);
		this.statusCode = statusCode;
		this.name = name;
		this.errors = errors;
	}
	
	public int getStatusCode() {
		return statusCode;
	}

	public String getName() {
		return name;
	}

	public List<String> getErrors() {
		return errors;
	}
}
