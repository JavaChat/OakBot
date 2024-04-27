package oakbot.ai.stabilityai;

/**
 * Helper class for building Stability.ai prompts.
 * @author Michael Angstadt
 */
public class PromptBuilder implements CharSequence {
	private final StringBuilder sb;

	/**
	 * Creates a new prompt builder.
	 */
	public PromptBuilder() {
		sb = new StringBuilder();
	}

	/**
	 * Creates a new prompt builder.
	 * @param text the text to initialize the builder with
	 */
	public PromptBuilder(String text) {
		sb = new StringBuilder(text);
	}

	/**
	 * Appends a raw string.
	 * @param text the string to append
	 * @return this
	 */
	public PromptBuilder append(CharSequence text) {
		sb.append(text);
		return this;
	}

	/**
	 * Appends a weighted word. The higher the weight value, the more importance
	 * the system will assign to that word.
	 * @param word the word to append
	 * @param weight the weight to assign to the word (between 0 and 1)
	 * @return this
	 * @throws IllegalArgumentException if the weight is not between 0 and 1
	 */
	public PromptBuilder append(CharSequence word, double weight) {
		if (weight < 0.0 || weight > 1.0) {
			throw new IllegalArgumentException("Weight must be between 0 and 1.");
		}

		sb.append('(').append(word).append(':').append(weight).append(')');
		return this;
	}

	@Override
	public int length() {
		return sb.length();
	}

	@Override
	public char charAt(int index) {
		return sb.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return sb.subSequence(start, end);
	}

	@Override
	public String toString() {
		return sb.toString();
	}
}
