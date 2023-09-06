package oakbot.util;

/**
 * Iterates over the characters in a String. Provides additional functionality
 * over {@link String#chars}.
 * @author Michael Angstadt
 */
public class CharIterator {
	private final String s;
	private int i = -1;

	/**
	 * @param s the string to iterate over
	 */
	public CharIterator(String s) {
		this.s = s;
	}

	/**
	 * Determines if there are more characters to iterate over.
	 * @return true if there are more characters, false if not
	 */
	public boolean hasNext() {
		return i + 1 < s.length();
	}

	/**
	 * Advances to the next character.
	 * @return the next character
	 */
	public char next() {
		return s.charAt(++i);
	}

	/**
	 * Gets the previous character.
	 * @return the previous character or 0 if the iterator is at the beginning
	 * of the string
	 */
	public char prev() {
		return (i <= 0) ? 0 : s.charAt(i - 1);
	}

	/**
	 * Gets the index of the current character in the string.
	 * @return the index
	 */
	public int index() {
		return i;
	}
}
