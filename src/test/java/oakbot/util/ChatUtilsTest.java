package oakbot.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class ChatUtilsTest {
	@Test
	public void parseFkey() {
		assertEquals("abc123", ChatUtils.parseFkey("<input name=\"fkey\" value=\"abc123\">"));
		assertEquals("abc123", ChatUtils.parseFkey("<input value=\"abc123\" name=\"fkey\">"));
		assertEquals("abc123", ChatUtils.parseFkey("<INpUT  name=\"fkey\" \t value=\"abc123\" >"));

		assertNull(ChatUtils.parseFkey("<input name=\"foo\" value=\"abc123\">"));
		assertNull(ChatUtils.parseFkey("<a name=\"fkey\" value=\"abc123\">"));
		assertNull(ChatUtils.parseFkey("{\"not\":\"html\"}"));
	}
}
