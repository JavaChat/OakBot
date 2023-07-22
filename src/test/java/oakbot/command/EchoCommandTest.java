package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class EchoCommandTest {
	@Test
	public void onMessage() {
		assertOnMessage("foo bar", "foo bar");
		assertOnMessage("", ":1 Tell me what to say.");
		assertOnMessage("<b>foo</b> bar", "**foo** bar");
	}

	private static void assertOnMessage(String input, String expectedResponse) {
		EchoCommand echo = new EchoCommand();

		//@formatter:off
		ChatCommand chatCommand = new ChatCommandBuilder(echo)
			.messageId(1)
			.content(input)
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions actions = echo.onMessage(chatCommand, bot);

		assertMessage(expectedResponse, actions);
	}
}
