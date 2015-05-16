package oakbot.chat;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class SplitStrategyTest {
	@Test
	public void no_max_length() {
		for (SplitStrategy strategy : SplitStrategy.values()) {
			List<String> actual = strategy.split("message", -1);
			List<String> expected = Arrays.asList("message");
			assertEquals(expected, actual);
		}
	}

	@Test
	public void message_does_not_exceed_max_length() {
		for (SplitStrategy strategy : SplitStrategy.values()) {
			List<String> actual = strategy.split("message", 50);
			List<String> expected = Arrays.asList("message");
			assertEquals(expected, actual);
		}
	}

	@Test
	public void word() {
		List<String> actual = SplitStrategy.WORD.split("Java is a general-purpose computer programming language that is concurrent, class-based, object-oriented, and specifically designed to have as few implementation dependencies as possible.", 100);
		//@formatter:off
		List<String> expected = Arrays.asList(
			"Java is a general-purpose computer programming language that is concurrent, class-based, ...",
			"object-oriented, and specifically designed to have as few implementation dependencies as ...",
			"possible."
		);
		//@formatter:on
		assertEquals(expected, actual);
	}

	@Test
	public void word_markdown() {
		List<String> actual = SplitStrategy.WORD.split("Java is a general-purpose computer programming language that is concurrent, class-based, object-oriented, and specifically designed to have **as few implementation dependencies as possible**.", 100);
		//@formatter:off
		List<String> expected = Arrays.asList(
			"Java is a general-purpose computer programming language that is concurrent, class-based, ...",
			"object-oriented, and specifically designed to have ...",
			"**as few implementation dependencies as possible**."
		);
		//@formatter:on
		assertEquals(expected, actual);
	}

	@Test
	public void word_with_markdown_section_that_exceeds_max_length() {
		List<String> actual = SplitStrategy.WORD.split("Java is a general-purpose computer programming language that is concurrent, class-based, object-oriented, and specifically designed to have **as few implementation dependencies as possible**.", 30);
		//@formatter:off
		List<String> expected = Arrays.asList(
			"Java is a general-purpose ...",
			"computer programming ...",
			"language that is ...",
			"concurrent, class-based, ...",
			"object-oriented, and ...",
			"specifically designed to ...",
			"have ...",
			"**as few implementation ...",
			"dependencies as ...",
			"possible**."
		);
		//@formatter:on
		assertEquals(expected, actual);
	}

	@Test
	public void newline() {
		List<String> actual = SplitStrategy.NEWLINE.split("Java is a general-purpose computer programming language that is\nconcurrent, class-based, object-oriented, and specifically designed to have as few implementation dependencies as possible.", 100);
		//@formatter:off
		List<String> expected = Arrays.asList(
			"Java is a general-purpose computer programming language that is",
			"concurrent, class-based, object-oriented, and specifically designed to have as few implementation de",
			"pendencies as possible."
		);
		//@formatter:on
		assertEquals(expected, actual);
	}
}
