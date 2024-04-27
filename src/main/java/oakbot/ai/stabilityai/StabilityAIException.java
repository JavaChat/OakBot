package oakbot.ai.stabilityai;

import java.util.List;

/**
 * @author Michael Angstadt
 */
public class StabilityAIException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final String name;
	private final List<String> errors;

	public StabilityAIException(String name, List<String> errors) {
		super(name + ": " + errors);
		this.name = name;
		this.errors = errors;
	}

	public String getName() {
		return name;
	}

	public List<String> getErrors() {
		return errors;
	}
}
