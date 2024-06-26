package oakbot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Angstadt
 */
class RelativeDateFormatTest {
	private final RelativeDateFormat relativeDf = new RelativeDateFormat();

	@Test
	void format() {
		var now = LocalDateTime.now();
		assertEquals("A moment ago", relativeDf.format(now));

		now = now.minusMinutes(30);
		assertTrue(relativeDf.format(now).matches("\\d+ minutes ago"));

		now = now.minusHours(1);
		assertTrue(relativeDf.format(now).matches("Today at .*"));

		now = now.minusDays(1);
		assertTrue(relativeDf.format(now).matches("Yesterday at .*"));

		now = now.minusDays(1);
		assertEquals("About 2 days ago.", relativeDf.format(now));

		now = now.minusWeeks(1);
		assertEquals("Over a week ago.", relativeDf.format(now));

		now = now.minusWeeks(1);
		assertEquals("Over 2 weeks ago.", relativeDf.format(now));

		now = now.minusWeeks(3);
		assertEquals("Over a month ago.", relativeDf.format(now));

		now = now.minusMonths(1);
		assertEquals("Over 2 months ago.", relativeDf.format(now));
	}
}
