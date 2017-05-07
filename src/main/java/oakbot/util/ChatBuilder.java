package oakbot.util;

import org.apache.commons.lang3.StringEscapeUtils;

import oakbot.bot.ChatCommand;
import oakbot.chat.ChatMessage;

/**
 * Helper class for building chat messages that are formatted in Stack Overflow
 * Chat markdown.
 * @author Michael Angstadt
 * @see <a href="http://chat.stackoverflow.com/faq#formatting">Formatting
 * FAQ</a>
 */
public class ChatBuilder implements CharSequence {
	private final StringBuilder sb;

	/**
	 * Creates a new chat builder.
	 */
	public ChatBuilder() {
		sb = new StringBuilder();
	}

	/**
	 * Creates a new chat builder.
	 * @param text the text to initialize the builder with
	 */
	public ChatBuilder(String text) {
		sb = new StringBuilder(text);
	}

	/**
	 * Appends the character sequence for "fixed font". Every line must begin
	 * with this sequence in order to obtain this formatting style.
	 * @return this
	 */
	public ChatBuilder fixed() {
		return append("    ");
	}

	/**
	 * Appends the character sequence for "blockquote". This must go at the
	 * beginning of the string.
	 * @return this
	 */
	public ChatBuilder quote() {
		return append('>');
	}

	/**
	 * Wraps text in "blockquote" formatting.
	 * @param text the text to wrap
	 * @return this
	 */
	public ChatBuilder quote(String text) {
		return quote().append(text);
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
	 * Appends the character sequence for "monospace".
	 * @return this
	 */
	public ChatBuilder code() {
		return append('`');
	}

	/**
	 * Wraps text in "monospace" formatting.
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
	 * Appends a hyperlink.
	 * @param display the display text
	 * @param url the URL
	 * @return this
	 */
	public ChatBuilder link(String display, String url) {
		return link(display, url, null);
	}

	/**
	 * Appends a hyperlink.
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
	 * Appends the "reply to message" syntax. This must go at the beginning of
	 * the string.
	 * @param message the message to reply to
	 * @return this
	 */
	public ChatBuilder reply(ChatMessage message) {
		return append(':').append(message.getMessageId()).append(' ');
	}

	/**
	 * Appends the "reply to message" syntax. This must go at the beginning of
	 * the string.
	 * @param command the message to reply to
	 * @return this
	 */
	public ChatBuilder reply(ChatCommand command) {
		return reply(command.getMessage());
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
	 * Appends a mention.
	 * @param username the username to mention (must be at least 3 characters
	 * long and must not contain spaces)
	 * @return this
	 */
	public ChatBuilder mention(String username) {
		return append('@').append(username);
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
	 * Appends a number.
	 * @param i the number to append
	 * @return this
	 */
	public ChatBuilder append(int i) {
		sb.append(i);
		return this;
	}

	/**
	 * Appends a number.
	 * @param l the number to append
	 * @return this
	 */
	public ChatBuilder append(long l) {
		sb.append(l);
		return this;
	}

	/**
	 * Appends a raw string.
	 * @param text the string to append
	 * @return this
	 */
	public ChatBuilder append(CharSequence text) {
		sb.append(text);
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

	/**
	 * Converts HTML-formated content to Markdown syntax.
	 * @param content the HTML content
	 * @param fixedFont if the message is formatted with a fixed font
	 * @return the content as formatted in Markdown
	 */
	public static String toMarkdown(String content, boolean fixedFont) {
		if (content == null) {
			return null;
		}

		String markdown;
		if (fixedFont) {
			/*
			 * Any Markdown syntax in a fixed-font message is ignored and
			 * treated as plain-text.
			 */
			markdown = "    " + content.replaceAll("(\r\n|\r|\n)", "$1    ");
		} else {
			boolean multiline = content.indexOf('\n') >= 0 || content.indexOf('\r') >= 0;
			if (multiline) {
				/*
				 * Any Markdown syntax in a multi-line message is ignored and
				 * treated as plain-text.
				 */
				markdown = content;
			} else {
				//@formatter:off
				markdown = content
				
				//escape characters used in Markdown syntax
				.replaceAll("[\\*_\\`()\\[\\]]", "\\\\$0")
				
				//replace formatting tags with Markdown
				.replaceAll("</?i>", "*")
				.replaceAll("</?b>", "**")
				.replaceAll("</?code>", "`")
				.replaceAll("</?strike>", "---")
				.replaceAll("<a.*?><span class=\"ob-post-tag\".*?>(.*?)</span></a>", "[tag:$1]")
				.replaceAll("<a href=\"(.*?)\".*?>(.*?)</a>", "[$2]($1)");
				//@formatter:on

				/*
				 * Note: Stack Overflow Chat does not convert "blockquote"
				 * syntax (">" character) to an HTML tag.
				 */
			}
		}

		//decode HTML entities
		markdown = StringEscapeUtils.unescapeHtml4(markdown);

		return markdown;
	}
}
