package oakbot.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class RelativeDateFormatTest {
	private final RelativeDateFormat relativeDf = new RelativeDateFormat();

	@Test
	public void format() {
		Calendar c = Calendar.getInstance();
		assertEquals("A moment ago", relativeDf.format(c.getTime()));

		c.add(Calendar.MINUTE, -30);
		assertTrue(relativeDf.format(c.getTime()).matches("\\d+ minutes ago"));

		c.add(Calendar.HOUR, -1);
		assertTrue(relativeDf.format(c.getTime()).matches("Today at .*"));

		c.add(Calendar.DATE, -1);
		assertTrue(relativeDf.format(c.getTime()).matches("Yesterday at .*"));

		c.add(Calendar.DATE, -1);
		assertEquals("About 2 days ago.", relativeDf.format(c.getTime()));

		c.add(Calendar.DATE, -7);
		assertEquals("About a week ago.", relativeDf.format(c.getTime()));

		c.add(Calendar.DATE, -7);
		assertEquals("About 2 weeks ago.", relativeDf.format(c.getTime()));

		c.add(Calendar.DATE, -21);
		assertEquals("About a month ago.", relativeDf.format(c.getTime()));

		c.add(Calendar.DATE, -30);
		assertEquals("About 2 months ago.", relativeDf.format(c.getTime()));
	}
}
