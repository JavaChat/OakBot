package oakbot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Angstadt
 */
class StringUtilsTest {
	@Test
	void plural() {
		assertEquals("cats", StringUtils.plural("cat", 0));
		assertEquals("cat", StringUtils.plural("cat", 1));
		assertEquals("cats", StringUtils.plural("cat", 2));

		assertEquals("buses", StringUtils.plural("bus", 0));
		assertEquals("bus", StringUtils.plural("bus", 1));
		assertEquals("buses", StringUtils.plural("bus", 2));
	}

	@Test
	void possessive() {
		assertEquals("cat's", StringUtils.possessive("cat"));
		assertEquals("cats'", StringUtils.possessive("cats"));
	}

	@Test
	void countWords() {
		assertEquals(0, StringUtils.countWords(""));
		assertEquals(0, StringUtils.countWords("  "));
		assertEquals(1, StringUtils.countWords("  one  "));
		assertEquals(3, StringUtils.countWords("one two\tthree"));
	}

	@Test
	void a() {
		assertEquals("a", StringUtils.a(""));
		assertEquals("a", StringUtils.a("cat"));
		assertEquals("an", StringUtils.a("animal"));
		assertEquals("an", StringUtils.a("egg"));
		assertEquals("an", StringUtils.a("igloo"));
		assertEquals("an", StringUtils.a("olive"));
		assertEquals("an", StringUtils.a("umbrella"));
	}
}
