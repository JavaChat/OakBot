package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import oakbot.bot.IBot;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
class EchoCommandTest {
	@Test
	void onMessage() {
		assertOnMessage("foo bar", "foo bar", 0);
		assertOnMessage("", "Tell me what to say.", 1);
		assertOnMessage("<b>foo</b> bar", "**foo** bar", 0);
	}

	private static void assertOnMessage(String input, String expectedResponse, long expectedParentId) {
		var echo = new EchoCommand();

		//@formatter:off
		var chatCommand = new ChatCommandBuilder(echo)
			.messageId(1)
			.content(input)
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var actions = echo.onMessage(chatCommand, bot);

		assertMessage(expectedResponse, expectedParentId, actions);
	}
}
