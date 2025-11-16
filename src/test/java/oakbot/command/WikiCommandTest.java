package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;

import org.junit.jupiter.api.Test;

import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
class WikiCommandTest {
	private final WikiCommand command = new WikiCommand();

	@Test
	void empty() {
		var message = new ChatCommandBuilder(command).messageId(1).build();

		var response = command.onMessage(message, null);
		assertMessage("Please specify the term you'd like to display.", 1, response);
	}

	@Test
	void spaces() {
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
	void url_safe() {
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
