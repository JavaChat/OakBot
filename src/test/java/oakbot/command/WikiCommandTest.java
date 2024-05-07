package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;

import org.junit.Test;

import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class WikiCommandTest {
	private final WikiCommand command = new WikiCommand();

	@Test
	public void empty() {
		var message = new ChatCommandBuilder(command).messageId(1).build();

		var response = command.onMessage(message, null);
		assertMessage(":1 Please specify the term you'd like to display.", response);
	}

	@Test
	public void spaces() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("John Doe")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("https://en.wikipedia.org/wiki/John_Doe", response);
	}

	@Test
	public void url_safe() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("I/O")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("https://en.wikipedia.org/wiki/I%2FO", response);
	}
}
