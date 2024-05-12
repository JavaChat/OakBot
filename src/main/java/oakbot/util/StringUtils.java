package oakbot.util;

import java.util.regex.Pattern;

/**
 * Contains miscellaneous string utility methods.
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

	/**
	 * Counts the number of words in a given string.
	 * @param phrase the string
	 * @return the number of words
	 */
	public static int countWords(String phrase) {
		phrase = phrase.trim();
		if (phrase.isEmpty()) {
			return 0;
		}

		var p = Pattern.compile("\\s+");
		var m = p.matcher(phrase);
		return (int) m.results().count() + 1;
	}
}
