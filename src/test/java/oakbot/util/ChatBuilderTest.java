package oakbot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Angstadt
 */
class ChatBuilderTest {
	@Test
	void toString_reply_method_can_be_called_any_time() {
		var cb = new ChatBuilder();
		cb.append("one");
		cb.reply(1);
		cb.append(" two");
		assertEquals(":1 one two", cb.toString());
	}

	@Test
	void toString_fixed_width_syntax_comes_before_reply() {
		var cb = new ChatBuilder();
		cb.fixedWidth();
		cb.append("one");
		cb.reply(1);
		cb.append(" two");
		assertEquals("    :1 one two", cb.toString());
	}

	@Test
	void toString_fixed_width() {
		var cb = new ChatBuilder();
		cb.fixedWidth();
		cb.append("one");
		cb.nl();
		cb.append("two\r\nthree");
		assertEquals("    one\n    two\r\n    three", cb.toString());
	}

	@Test
	void toMarkdown() {
		//convert HTML tags to Markdown
		assertToMarkdown("<b>value</b>", "**value**", false);
		assertToMarkdown("<i>value</i>", "*value*", false);
		assertToMarkdown("<code>value</code>", "`value`", false);
		assertToMarkdown("<strike>value</strike>", "---value---", false);

		assertToMarkdown("<a href=\"http://google.com\">value</a>", "[value](http://google.com)", false);
		assertToMarkdown("<a href=\"http://google.com\" title=\"title\">value</a>", "[value](http://google.com \"title\")", false);
		assertToMarkdown("<a href=\"http://google.com\" title=\"title\">value</a>", "[value](http://google.com)", false, false, "");
		assertToMarkdown("<a href=\"http://google.com\"><b>one</b> two</a>", "[**one** two](http://google.com)", false);
		assertToMarkdown("<a href=\"//stackoverflow.com/questions/tagged/java\"><span class=\"ob-post-tag\" style=\"background-color: #E0EAF1; color: #3E6D8E; border-color: #3E6D8E; border-style: solid;\">java</span></a>", "[tag:java]", false);

		assertToMarkdown("<a href=\"page.html\">value</a>", "[value](http://example.com/test/page.html)", false, true, "http://example.com/test/index.html");
		assertToMarkdown("<a href=\"/page.html\">value</a>", "[value](http://example.com/page.html)", false, true, "http://example.com/test/index.html");
		assertToMarkdown("<a href=\"../page.html\">value</a>", "[value](http://example.com/page.html)", false, true, "http://example.com/test/index.html");
		assertToMarkdown("<a href=\"//google.com/page.html\">value</a>", "[value](http://google.com/page.html)", false, true, "http://example.com/test/index.html");

		//multi-line text
		assertToMarkdown("one\ntwo\rthree\r\nfour", "one\ntwo\rthree\r\nfour", false);
		assertToMarkdown("one\ntwo\rthree\r\nfour", "    one\n    two\r    three\r\n    four", true);

		//do not perform Markdown conversion on multi-line and fixed font messages 
		assertToMarkdown("<b>one *two*</b>\nthree", "<b>one *two*</b>\nthree", false);
		assertToMarkdown("<b>one *two*</b>\rthree", "<b>one *two*</b>\rthree", false);
		assertToMarkdown("<b>one *two*</b>\r\nthree", "<b>one *two*</b>\r\nthree", false);
		assertToMarkdown("<b>one *two*</b>\nthree", "    <b>one *two*</b>\n    three", true);

		//accept all forms of newline sequences
		assertToMarkdown("one\ntwo", "one\ntwo", false);
		assertToMarkdown("one\rtwo", "one\rtwo", false);
		assertToMarkdown("one\r\ntwo", "one\r\ntwo", false);

		//escape Markdown special characters
		assertToMarkdown("* _ ` ( ) [ ]", "\\* \\_ \\` \\( \\) \\[ \\]", false);

		//do not escape Markdown characters in URLs
		assertToMarkdown("<a href=\"http://google.com/foo_bar\">one_two</a>", "[one\\_two](http://google.com/foo_bar)", false);

		//decode HTML entities
		assertToMarkdown("&lt;value&gt;", "<value>", false);
		assertToMarkdown("&lt;one&gt;\ntwo", "<one>\ntwo", false);
		assertToMarkdown("&lt;value&gt;", "    <value>", true);

		//return null when input is null
		assertToMarkdown(null, null, false);
	}

	private static void assertToMarkdown(String html, String expected, boolean fixedFont) {
		var actual = ChatBuilder.toMarkdown(html, fixedFont);
		assertEquals(expected, actual);
	}

	private static void assertToMarkdown(String html, String expected, boolean fixedFont, boolean includeTitleInLinks, String baseUrl) {
		var actual = ChatBuilder.toMarkdown(html, fixedFont, includeTitleInLinks, baseUrl);
		assertEquals(expected, actual);
	}
}
