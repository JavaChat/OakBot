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

	@Test
	public void empty() {
		ChatCommand message = new ChatCommandBuilder(command).messageId(1).build();

		ChatResponse response = command.onMessage(message, null);
		assertEquals(":1 Please specify the term you'd like to display.", response.getMessage());
	}

	@Test
	public void spaces() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("John Doe")
		.build(); //@formatter:on

		ChatResponse response = command.onMessage(message, null);
		assertEquals("http://en.wikipedia.org/wiki/John_Doe", response.getMessage());
	}

	@Test
	public void url_safe() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("I/O")
		.build(); //@formatter:on

		ChatResponse response = command.onMessage(message, null);
		assertEquals("http://en.wikipedia.org/wiki/I%2FO", response.getMessage());
	}
}
