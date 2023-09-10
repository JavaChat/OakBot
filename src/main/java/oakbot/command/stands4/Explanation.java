package oakbot.command.stands4;

/**
 * An explanation of a common idiom.
 * @author Michael Angstadt
 * @see Stands4Client#explain
 */
public class Explanation {
	private final String explanation;
	private final String example;

	public Explanation(String explanation, String example) {
		this.explanation = explanation;
		this.example = example;
	}

	public String getExplanation() {
		return explanation;
	}

	public String getExample() {
		return example;
	}
}
