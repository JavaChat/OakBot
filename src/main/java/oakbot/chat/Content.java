package oakbot.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the message content of a chat message.
 * @author Michael Angstadt
 */
public class Content {
	private static final Pattern fixedWidthRegex = Pattern.compile("^<pre class='(full|partial)'>(.*?)</pre>$", Pattern.DOTALL);
	private static final Pattern multiLineRegex = Pattern.compile("^<div class='(full|partial)'>(.*?)</div>$", Pattern.DOTALL);
	private static final Predicate<String> oneboxRegex = Pattern.compile("^<div class=\"([^\"]*?)onebox([^\"]*?)\"[^>]*?>").asPredicate();

	private final String rawContent;
	private final String content;
	private final boolean fixedFont;

	private Content(String rawContent, String content, boolean fixedFont) {
		this.rawContent = rawContent;
		this.content = content;
		this.fixedFont = fixedFont;
	}

	/**
	 * @param content the message content (may contain HTML formatting)
	 * @param fixedFont true if the message content is in fixed font, false if
	 * not
	 */
	public Content(String content, boolean fixedFont) {
		this(null, content, fixedFont);
	}

	/**
	 * Parses the content from off the wire.
	 * @param rawContent the raw content from off the wire (may contain HTML)
	 * @return the parsed content
	 */
	public static Content parse(String rawContent) {
		String content = extractFixedFontContent(rawContent);
		boolean fixedFont = (content != null);
		if (!fixedFont) {
			content = extractMultiLineContent(rawContent);
			if (content == null) {
				content = rawContent;
			}
		}

		return new Content(rawContent, content, fixedFont);
	}

	/**
	 * Gets the message content that was retrieved directly off the wire. This
	 * method contrasts with the {@link #getContent} method, which contains a
	 * tweaked version of the raw content to make it more usable (see the source
	 * code of the {@link #parse} method for details on how the content is
	 * tweaked).
	 * @return the raw content or null if this object was not created from a
	 * parsed message
	 */
	public String getRawContent() {
		return rawContent;
	}

	/**
	 * Determines whether the message content is a onebox.
	 * @return true if the message content is a onebox, false if not
	 */
	public boolean isOnebox() {
		return oneboxRegex.test(content);
	}

	/**
	 * Determines whether the message content is formatted in a monospace font.
	 * @return true if it's formatted in a monospace font, false if not
	 */
	public boolean isFixedFont() {
		return fixedFont;
	}

	/**
	 * <p>
	 * Gets the message content. If you need the raw content as it was parsed
	 * off the wire, use {@link #getRawContent}.
	 * </p>
	 * <p>
	 * Messages that consist of a single line of content may contain basic HTML
	 * formatting (even though Stack Overflow Chat only accepts messages
	 * formatted in Markdown syntax, when chat messages are retrieved from the
	 * API, they are formatted in HTML).
	 * </p>
	 * <p>
	 * Messages that contain multiple lines of text will not contain any
	 * formatting because Stack Overflow Chat does not allow multi-lined
	 * messages to contain formatting.
	 * </p>
	 * <p>
	 * Messages that are formatted using a fixed font will not contain any
	 * formatting either. Fixed font messages may contain multiple lines of
	 * text. If a message is formatted in fixed font, the {@link #isFixedFont}
	 * method will return true.
	 * </p>
	 * <p>
	 * Note that messages that contain a onebox will contain significant HTML
	 * code. Use the {@link #isOnebox} method to determine if the message is a
	 * onebox.
	 * </p>
	 * @return the message content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * <p>
	 * Parses any mentions out of the message.
	 * </p>
	 * <p>
	 * "Mentioning" someone in chat will make a "ping" sound on the mentioned
	 * user's computer. A mention consists of an "at" symbol followed by a
	 * username. For example, this chat message contains two mentions:
	 * </p>
	 * 
	 * <pre>
	 * Good morning, {@literal @}Frank and {@literal @}Bob!
	 * </pre>
	 * <p>
	 * Mentions cannot contain spaces, so if a username contains spaces, those
	 * spaces are removed from the mention.
	 * </p>
	 * <p>
	 * A mention does not have to contain a user's entire username. It may only
	 * contain the beginning of the username. For example, if someone's username
	 * is "Bob Smith", then typing "{@literal @}BobS" will ping that user.
	 * </p>
	 * <p>
	 * Because mentions can contain only part of a person's username, and
	 * because usernames are not unique on Stack Overflow, it's possible for a
	 * mention to refer to more than one user.
	 * </p>
	 * <p>
	 * Mentions must be at least 3 characters long (not including the "at"
	 * symbol). Mentions less than 3 characters long are treated as normal text.
	 * </p>
	 * @return the mentions or empty list of none were found. The "at" symbol is
	 * not included in the returned output.
	 */
	public List<String> getMentions() {
		final int minLength = 3;
		List<String> mentions = new ArrayList<>(1);

		boolean inMention = false;
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);

			if (inMention) {
				if (Character.isLetter(c) || Character.isDigit(c)) {
					buffer.append(c);
					continue;
				}

				inMention = false;
				if (buffer.length() >= minLength) {
					mentions.add(buffer.toString());
				}
			}

			if (c == '@') {
				inMention = true;
				buffer.setLength(0);
				continue;
			}
		}

		if (inMention) {
			if (buffer.length() >= minLength) {
				mentions.add(buffer.toString());
			}
		}

		return mentions;
	}

	/**
	 * Determines if a user is mentioned in the message.
	 * @param username the username to look for
	 * @return true if the user is mentioned, false if not
	 */
	public boolean isMentioned(String username) {
		List<String> mentions = getMentions();
		if (mentions.isEmpty()) {
			return false;
		}

		username = username.toLowerCase().replace(" ", "");
		for (String mention : mentions) {
			mention = mention.toLowerCase();
			if (username.startsWith(mention)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "[fixedFont=" + fixedFont + "] " + content;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + content.hashCode();
		result = prime * result + (fixedFont ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Content other = (Content) obj;
		if (!content.equals(other.content)) return false;
		if (fixedFont != other.fixedFont) return false;
		return true;
	}

	/**
	 * Extracts the message content from a message that is formatted in fixed
	 * font. Fixed font messages are enclosed in a &lt;pre&gt; tag.
	 * @param content the complete chat message content
	 * @return the extracted message content or null if the message is not
	 * fixed-font
	 */
	private static String extractFixedFontContent(String content) {
		Matcher m = fixedWidthRegex.matcher(content);
		return m.find() ? m.group(2) : null;
	}

	/**
	 * Extracts the message content from a multi-line message. Multi-line
	 * messages are enclosed in a &lt;div&gt; tag. Also converts &lt;br&gt; tags
	 * to newlines.
	 * @param content the complete chat message content
	 * @return the extracted message content or null if the message is not
	 * multi-line
	 */
	private static String extractMultiLineContent(String content) {
		Matcher m = multiLineRegex.matcher(content);
		if (!m.find()) {
			return null;
		}

		return m.group(2).replace(" <br> ", "\n");
	}
}
