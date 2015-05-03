package oakbot.command.define;

/**
 * A dictionary definition for a word.
 * @author Michael Angstadt
 */
public class Definition {
	private String wordType;
	private String definition, example;

	public String getWordType() {
		return wordType;
	}

	public void setWordType(String wordType) {
		this.wordType = wordType;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public String getExample() {
		return example;
	}

	public void setExample(String example) {
		this.example = example;
	}
}
