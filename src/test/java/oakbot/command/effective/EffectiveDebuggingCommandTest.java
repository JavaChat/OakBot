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
		int expected = 1;
		for (EffectiveDebuggingCommand.Item item : command.items) {
			assertEquals("Item numbers are not sequential at index " + (expected - 1) + ".", expected, item.number);
			expected++;
		}
	}

	@Test
	public void pageNumbers() throws Exception {
		int prevPage = 0;

		for (EffectiveDebuggingCommand.Item item : command.items) {
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
