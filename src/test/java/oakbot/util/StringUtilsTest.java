package oakbot.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class StringUtilsTest {
	@Test
	public void plural() {
		assertEquals("cats", StringUtils.plural("cat", 0));
		assertEquals("cat", StringUtils.plural("cat", 1));
		assertEquals("cats", StringUtils.plural("cat", 2));

		assertEquals("buses", StringUtils.plural("bus", 0));
		assertEquals("bus", StringUtils.plural("bus", 1));
		assertEquals("buses", StringUtils.plural("bus", 2));
	}
}
