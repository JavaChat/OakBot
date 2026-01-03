package oakbot.util;

import java.util.stream.IntStream;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatCommand;

/**
 * Helper class for building chat messages that are formatted in Stack Overflow
 * Chat markdown.
 * @author Michael Angstadt
 * @see <a href="http://chat.stackoverflow.com/faq#formatting">Formatting
 * FAQ</a>
 */
public class ChatBuilder implements CharSequence {
	public static final String FIXED_WIDTH_PREFIX = "    ";

	private final StringBuilder sb;
	private long replyId = -1;
	private boolean fixedWidth = false;

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
	public ChatBuilder(CharSequence text) {
		sb = new StringBuilder(text);
	}

	/**
	 * <p>
	 * Applies "fixed width font" formatting to the entire message.
	 * </p>
	 * <p>
	 * In SO Chat, it's not possible to format parts of a message in fixed-width
	 * font. Only the entire message can be formatted in this way.
	 * </p>
	 * @return this
	 */
	public ChatBuilder fixedWidth() {
		fixedWidth = true;
		return this;
	}

	/**
	 * <p>
	 * Inserts an image into the message using explicit syntax. Note that images
	 * and text cannot be mixed.
	 * </p>
	 * <p>
	 * This method must be used if the image URL does not end with the file
	 * extension of a supported image format (e.g. ".jpg", ".gif"). If the URL
	 * ends with such as extension, then this syntax is not necessary.
	 * </p>
	 * @param url the image URL
	 * @return this
	 */
	public ChatBuilder image(CharSequence url) {
		return append('!').append(url);
	}

	/**
	 * Appends the character sequence for "blockquote". This must go at the
	 * beginning of the string.
	 * @return this
	 */
	public ChatBuilder quote() {
		return append("> ");
	}

	/**
	 * Wraps text in "blockquote" formatting.
	 * @param text the text to wrap
	 * @return this
	 */
	public ChatBuilder quote(CharSequence text) {
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
	public ChatBuilder bold(CharSequence text) {
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
	public ChatBuilder code(CharSequence text) {
		return code().append(text).code();
	}

	/**
	 * Appends the character sequence for "multi-line code block" (Discord
	 * only). Newline is included.
	 * @return this
	 */
	public ChatBuilder codeBlock() {
		return codeBlock("");
	}

	/**
	 * Appends the character sequence for "multi-line code block" (Discord
	 * only). Newline is included.
	 * @param language the language the code is in for syntax highlighting (e.g.
	 * "java")
	 * @return this
	 */
	public ChatBuilder codeBlock(String language) {
		return append("```").append(language).nl();
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
	public ChatBuilder italic(CharSequence text) {
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
	public ChatBuilder strike(CharSequence text) {
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
	 * Formats the message as a reply.
	 * @param id the ID of the parent message
	 * @return this
	 * @deprecated the preferred way of specifying the parent message ID is to
	 * include it as a field in the POST request
	 */
	@Deprecated(forRemoval = false)
	public ChatBuilder reply(long id) {
		replyId = id;
		return this;
	}

	/**
	 * Formats the message as a reply.
	 * @param message the parent message
	 * @return this
	 * @deprecated the preferred way of specifying the parent message ID is to
	 * include it as a field in the POST request
	 */
	@Deprecated(forRemoval = false)
	public ChatBuilder reply(ChatMessage message) {
		return reply(message.id());
	}

	/**
	 * Formats the message as a reply.
	 * @param command the parent message
	 * @return this
	 * @deprecated the preferred way of specifying the parent message ID is to
	 * include it as a field in the POST request
	 */
	@Deprecated(forRemoval = false)
	public ChatBuilder reply(ChatCommand command) {
		return reply(command.getMessage());
	}

	/**
	 * Appends a tag.
	 * @param tag the tag name
	 * @return this
	 */
	public ChatBuilder tag(CharSequence tag) {
		return append("[tag:").append(tag).append(']');
	}

	/**
	 * Appends a mention.
	 * @param username the username to mention (must be at least 3 characters
	 * long and must not contain spaces)
	 * @return this
	 */
	public ChatBuilder mention(CharSequence username) {
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
	 * Appends a raw character multiple times.
	 * @param c the character to append
	 * @param repeat the number of times to repeat the character
	 * @return this
	 */
	public ChatBuilder repeat(char c, int repeat) {
		IntStream.range(0, repeat).forEach(i -> append(c));
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
		var replyPrefix = (replyId < 0) ? "" : (":" + replyId + " ");

		/*
		 * Fixed-width syntax must come before reply syntax.
		 * 
		 * (No need for special handling of quote syntax because it comes after
		 * reply syntax.)
		 */
		if (fixedWidth) {
			var withIndents = sb.toString().replaceAll("(\r\n|\r|\n)", "$1" + FIXED_WIDTH_PREFIX);
			return FIXED_WIDTH_PREFIX + replyPrefix + withIndents;
		}

		return replyPrefix + sb.toString();
	}

	/**
	 * Converts HTML-formated content to Markdown syntax.
	 * @param content the HTML content
	 * @param fixedFont if the message is formatted with a fixed font
	 * @return the content as formatted in Markdown
	 */
	public static String toMarkdown(String content, boolean fixedFont) {
		return toMarkdown(content, fixedFont, true, null);
	}

	/**
	 * Converts HTML-formated content to Markdown syntax.
	 * @param content the HTML content
	 * @param fixedFont if the message is formatted with a fixed font
	 * @param includeTitleInLinks true to include the value of each &lt;a&gt;
	 * tag's "title" attribute in Markdown links, false not to
	 * @param baseUrl the base URL for resolving links with relative URLs. This
	 * should be set to the URL of the page that the content came from.
	 * @return the content as formatted in Markdown
	 */
	public static String toMarkdown(String content, boolean fixedFont, boolean includeTitleInLinks, String baseUrl) {
		if (content == null) {
			return null;
		}

		String markdown;
		if (fixedFont) {
			/*
			 * Any Markdown syntax in a fixed-font message is ignored and
			 * treated as plain-text, so there is no need to escape those
			 * special characters.
			 */
			markdown = "    " + content.replaceAll("(\r\n|\r|\n)", "$1    ");
		} else {
			var multiline = content.indexOf('\n') >= 0 || content.indexOf('\r') >= 0;
			if (multiline) {
				/*
				 * Multi-line messages will not contain HTML formatting tags
				 * because Stack Overflow Chat does not allow formatting in
				 * multi-line messages.
				 * 
				 * Any Markdown syntax in a multi-line message is ignored and
				 * treated as plain-text, so there is no need to escape those
				 * special characters.
				 */
				markdown = content;
			} else {
				/*
				 * Replace HTML formatting tags with Markdown syntax.
				 * 
				 * Note: Stack Overflow Chat does not convert "blockquote"
				 * syntax (">" character) to an HTML tag.
				 */
				if (baseUrl == null) {
					baseUrl = "";
				}

				var nodeVisitor = new MarkdownNodeVisitor(includeTitleInLinks);
				Jsoup.parse(content, baseUrl).traverse(nodeVisitor);
				markdown = nodeVisitor.getMarkdown();
			}
		}

		//decode HTML entities
		markdown = StringEscapeUtils.unescapeHtml4(markdown);

		return markdown;
	}

	private static class MarkdownNodeVisitor implements NodeVisitor {
		private final ChatBuilder chatBuilder = new ChatBuilder();
		private final boolean includeTitleInLinks;

		private boolean inTag = false;
		private ChatBuilder linkText = null;

		public MarkdownNodeVisitor(boolean includeTitleInLinks) {
			this.includeTitleInLinks = includeTitleInLinks;
		}

		@Override
		public void head(Node node, int depth) {
			var cb = cb();

			//escape characters used in Markdown syntax
			if (node instanceof TextNode text) {
				cb.append(text.text().replaceAll("[\\*_\\`()\\[\\]]", "\\\\$0"));
				return;
			}

			if (node instanceof Element element) {
				switch (element.tagName()) {
				case "b" -> cb.bold();
				case "i" -> cb.italic();
				case "code" -> cb.code();
				case "strike" -> cb.strike();
				case "a" -> linkText = new ChatBuilder();
				case "span" -> {
					if (linkText != null && element.classNames().contains("ob-post-tag")) {
						inTag = true;
					}
				}
				}
			}
		}

		@Override
		public void tail(Node node, int depth) {
			var cb = cb();

			if (node instanceof Element element) {
				switch (element.tagName()) {
				case "b" -> cb.bold();
				case "i" -> cb.italic();
				case "code" -> cb.code();
				case "strike" -> cb.strike();
				case "a" -> processLink(element);
				}
			}
		}

		private void processLink(Element element) {
			var text = linkText.toString();

			if (inTag) {
				chatBuilder.tag(text);
			} else {
				var url = element.absUrl("href");
				if (url.isEmpty()) {
					/*
					 * If the <a> tag doesn't have an "href" attribute, treat it
					 * as plain text.
					 */
					chatBuilder.append(text);
				} else {
					var title = includeTitleInLinks ? element.attr("title") : "";
					chatBuilder.link(text, url, title);
				}
			}

			linkText = null;
			inTag = false;
		}

		private ChatBuilder cb() {
			return (linkText == null) ? chatBuilder : linkText;
		}

		public String getMarkdown() {
			return chatBuilder.toString();
		}
	}
}
