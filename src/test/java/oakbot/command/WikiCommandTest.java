package oakbot.command;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class WikiCommandTest {
	private final WikiCommand command = new WikiCommand();
	private final ChatCommandBuilder ccb = new ChatCommandBuilder(command.name());

	@Test
	public void empty() {
		ChatCommand cc = ccb.build(1, "");
		ChatResponse response = command.onMessage(cc, null);
		assertEquals(":1 Please specify the term you'd like to display.", response.getMessage());
	}

	@Test
	public void spaces() {
		ChatCommand cc = ccb.build(1, "John Doe");
		ChatResponse response = command.onMessage(cc, null);
		assertEquals("http://en.wikipedia.org/wiki/John_Doe", response.getMessage());
	}

	@Test
	public void url_safe() {
		ChatCommand cc = ccb.build(1, "I/O");
		ChatResponse response = command.onMessage(cc, null);
		assertEquals("http://en.wikipedia.org/wiki/I%2FO", response.getMessage());
	}
}
