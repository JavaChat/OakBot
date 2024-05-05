package oakbot.command.effective;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class EffectiveDebuggingCommandTest {
	private final EffectiveDebuggingCommand command = new EffectiveDebuggingCommand();

	@Test
	public void itemNumbers() throws Exception {
		var expected = 1;
		for (var item : command.items) {
			assertEquals("Item numbers are not sequential at index " + (expected - 1) + ".", expected, item.number);
			expected++;
		}
	}

	@Test
	public void pageNumbers() throws Exception {
		var prevPage = 0;

		for (var item : command.items) {
			if (item.page <= 0) {
				fail("Invalid page number: " + item.page);
			}

			if (item.page < prevPage) {
				fail("Page number for item " + item.number + " is less than previous item's page number.");
			}
			prevPage = item.page;
		}
	}
}
