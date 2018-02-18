package oakbot.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class ContentTest {
	@Test
	public void getMentions() {
		assertMention("Hello, @Frank.", "Frank");
		assertMention("Hello, @Frank2Cool.", "Frank2Cool");
		assertMention("Hello@Frank.", "Frank");
		assertMention("Hello, @Frank", "Frank");
		assertMention("Hello, @@Frank", "Frank");
		assertMention("Hello, @Frank and @Robert", "Frank", "Robert");
		assertMention("Hello, @Fr an");
		assertMention("Hello.");
	}

	private static void assertMention(String message, String... expectedMentions) {
		Content content = new Content(message, false);
		assertEquals(Arrays.asList(expectedMentions), content.getMentions());
	}

	@Test
	public void isMentioned() {
		assertIsMentioned("Hello, @FrankSmi", "frank smith", true);
		assertIsMentioned("Hello", "frank smith", false);
		assertIsMentioned("Hello, @FrankSmi", "bob", false);
	}

	private static void assertIsMentioned(String message, String username, boolean expected) {
		Content content = new Content(message, false);
		assertEquals(expected, content.isMentioned(username));
	}

	@Test
	public void isOnebox() {
		Content content;

		content = new Content("<div class=\"foooneboxbar\">onebox</div>", false);
		assertTrue(content.isOnebox());

		content = new Content("foobar", false);
		assertFalse(content.isOnebox());
	}

	@Test
	public void parse() {
		Content content;

		content = Content.parse("one\ntwo");
		assertFalse(content.isFixedFont());
		assertEquals("one\ntwo", content.getContent());
		assertEquals("one\ntwo", content.getRawContent());

		content = Content.parse("<pre class='full'>one\ntwo</pre>");
		assertTrue(content.isFixedFont());
		assertEquals("one\ntwo", content.getContent());
		assertEquals("<pre class='full'>one\ntwo</pre>", content.getRawContent());

		content = Content.parse("<pre class='partial'>one\ntwo</pre>");
		assertTrue(content.isFixedFont());
		assertEquals("one\ntwo", content.getContent());
		assertEquals("<pre class='partial'>one\ntwo</pre>", content.getRawContent());

		content = Content.parse("<div class='full'>one <br> two</div>");
		assertFalse(content.isFixedFont());
		assertEquals("one\ntwo", content.getContent());
		assertEquals("<div class='full'>one <br> two</div>", content.getRawContent());

		content = Content.parse("<div class='partial'>one <br> two</div>");
		assertFalse(content.isFixedFont());
		assertEquals("one\ntwo", content.getContent());
		assertEquals("<div class='partial'>one <br> two</div>", content.getRawContent());
	}
}
