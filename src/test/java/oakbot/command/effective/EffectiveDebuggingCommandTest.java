package oakbot.command.effective;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Angstadt
 */
public class EffectiveDebuggingCommandTest {
	private final EffectiveDebuggingCommand command = new EffectiveDebuggingCommand();

	@Test
	public void itemNumbers() throws Exception {
		var expected = 1;
		for (var item : command.items) {
			final var expected2 = expected;
			assertEquals(expected, item.number, () -> "Item numbers are not sequential at index " + (expected2 - 1) + ".");
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
