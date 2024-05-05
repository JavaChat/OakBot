package oakbot.util;

/**
 * Contains miscellaneous static utility methods.
 * @author Michael Angstadt
 */
public interface StringUtils {
	/**
	 * Determines if a word should be plural.
	 * @param word the singular version of the word
	 * @param number the number
	 * @return the plural or singular version of the word, depending on the
	 * provided number
	 */
	public static String plural(String word, long number) {
		if (number == 1) {
			return word;
		}

		return word + (word.endsWith("s") ? "es" : "s");
	}

	/**
	 * Generates the possessive form of the given word.
	 * @param word the word (e.g. "cats")
	 * @return the possessive form (e.g. "cats'")
	 */
	public static String possessive(String word) {
		return word + (word.endsWith("s") ? "'" : "'s");
	}
}
