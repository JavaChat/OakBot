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
}
