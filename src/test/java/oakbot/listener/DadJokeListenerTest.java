package oakbot.listener;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.chat.ChatMessage;
import oakbot.util.Sleeper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.assertTrue;

/**
 * @author Michael Angstadt
 */
public class DadJokeListenerTest {
	private final static BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), 0);

	@BeforeClass
	public static void beforeClass() {
		Sleeper.unitTest = true;
	}

	@AfterClass
	public static void afterClass() {
		Sleeper.unitTest = false;
	}

	@Test
	public void onMessage() {
		assertResponse("I'm tired of coding.", "Hi tired of coding, I'm Oak!");
		assertResponse("I'm tired of coding", "Hi tired of coding, I'm Oak!");
		assertResponse("i'm tired of coding.", "Hi tired of coding, I'm Oak!");
		assertResponse("I am tired of coding.", "Hi tired of coding, I'm Oak!");
		assertResponse("I'm tired of coding. I need a vacation!", "Hi tired of coding, I'm Oak!");
		assertResponse("I am tired of coding. I need a vacation!", "Hi tired of coding, I'm Oak!");
		assertResponse("is anybody here?? i am confused and need help!", "Hi confused and need help, I'm Oak!");

		assertNoResponse("is anybody here??");
	}

	private static void assertResponse(String message, String response) {
		ChatMessage chatMessage = new ChatMessage.Builder() //@formatter:off
			.messageId(1)
			.roomId(1)
			.content(message)
			.build(); //@formatter:on

		DadJokeListener listener = new DadJokeListener("Oak");
		ChatActions actions = listener.onMessage(chatMessage, context);
		assertMessage(":1 " + response, actions);
	}

	private static void assertNoResponse(String message) {
		ChatMessage chatMessage = new ChatMessage.Builder() //@formatter:off
			.messageId(1)
			.roomId(1)
			.content(message)
			.build(); //@formatter:on

		DadJokeListener listener = new DadJokeListener("Oak");
		ChatActions actions = listener.onMessage(chatMessage, context);
		assertTrue(actions.isEmpty());
	}
}
