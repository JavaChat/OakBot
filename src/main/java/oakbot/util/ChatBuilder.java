package oakbot.util;

/**
 * Helper class for building chat messages with SO Chat markdown.
 * @author Michael Angstadt
 */
public class ChatBuilder {
	private final StringBuilder sb = new StringBuilder();

	/**
	 * Appends the character sequence for "fixed font".
	 * @return this
	 */
	public ChatBuilder fixed() {
		return append("    ");
	}

	/**
	 * Appends the character sequence for "bold".
	 * @return this
	 */
	public ChatBuilder bold() {
		return append("**");
	}

	/**
	 * Wraps text in "bold" formatting.
	 * @param text the text to wrap
	 * @return this
	 */
	public ChatBuilder bold(String text) {
		return bold().append(text).bold();
	}

	/**
	 * Appends the character sequence for "code".
	 * @return this
	 */
	public ChatBuilder code() {
		return append('`');
	}

	/**
	 * Wraps text in "code" formatting.
	 * @param text the text to wrap
	 * @return this
	 */
	public ChatBuilder code(String text) {
		return code().append(text).code();
	}

	/**
	 * Appends the character sequence for "italic".
	 * @return this
	 */
	public ChatBuilder italic() {
		return append('*');
	}

	/**
	 * Wraps text in "italic" formatting.
	 * @param text the text to wrap
	 * @return this
	 */
	public ChatBuilder italic(String text) {
		return italic().append(text).italic();
	}

	/**
	 * Appends a clickable link.
	 * @param display the display text
	 * @param url the URL
	 * @param title the link title or null/empty for no title
	 * @return this
	 */
	public ChatBuilder link(String display, String url, String title) {
		append('[').append(display.trim()).append("](").append(url.trim());
		if (title != null && !title.isEmpty()) {
			append(" \"").append(title.trim()).append('"');
		}
		return append(')');
	}

	/**
	 * Appends a newline character.
	 * @return this
	 */
	public ChatBuilder nl() {
		return append('\n');
	}

	/**
	 * Appends the character sequence for "strike through".
	 * @return this
	 */
	public ChatBuilder strike() {
		return append("---");
	}

	/**
	 * Wraps text in "strike through" formatting.
	 * @param text the text to wrap
	 * @return this
	 */
	public ChatBuilder strike(String text) {
		return strike().append(text).strike();
	}

	/**
	 * Appends a tag.
	 * @param tag the tag name
	 * @return this
	 */
	public ChatBuilder tag(String tag) {
		return append("[tag:").append(tag).append(']');
	}

	/**
	 * Appends a raw character.
	 * @param c the character to append
	 * @return this
	 */
	public ChatBuilder append(char c) {
		sb.append(c);
		return this;
	}

	/**
	 * Appends a raw string.
	 * @param text the string to append
	 * @return this
	 */
	public ChatBuilder append(String text) {
		sb.append(text);
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}
}
