package oakbot.listener;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.chat.ChatMessage;

/**
 * @author Michael Angstadt
 */
public class MentionListenerTest {
	private final static BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), 0);

	@Test
	public void respond() {
		assertResponse("Hey @OakBot", ":0 Type `/help` to see all my commands.");
		assertResponse("Hey @Oak", ":0 Type `/help` to see all my commands.");
		assertResponse("@Oak Thank  you", ":0 You're welcome.");
		assertResponse("Thanks @Oak", ":0 You're welcome.");
		assertResponse("@Oak  thx!", ":0 You're welcome.");
		assertNoResponse("Hey @OakBott");
		assertNoResponse("Hey");
	}

	private static void assertNoResponse(String message) {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.content(message)
		.build();
		//@formatter:on

		MentionListener listener = new MentionListener("OakBot");
		ChatActions actions = listener.onMessage(chatMessage, context);
		assertTrue(actions.isEmpty());
	}

	private static void assertResponse(String message, String expectedResponse) {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.content(message)
		.build();
		//@formatter:on

		MentionListener listener = new MentionListener("OakBot");
		ChatActions actions = listener.onMessage(chatMessage, context);
		assertMessage(expectedResponse, actions);
	}

	@Test
	public void prevent_spam() {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.content("Hey @Oakbot")
		.build();
		//@formatter:on

		MentionListener listener = new MentionListener("OakBot");

		ChatActions response = listener.onMessage(chatMessage, context);
		assertFalse(response.isEmpty());

		response = listener.onMessage(chatMessage, context);
		assertTrue(response.isEmpty());
	}

	@Test
	public void ignore_next_message() {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.content("Hey @Oakbot")
		.build();
		//@formatter:on

		MentionListener listener = new MentionListener("OakBot");
		listener.ignoreNextMessage();

		ChatActions response = listener.onMessage(chatMessage, context);
		assertTrue(response.isEmpty());

		response = listener.onMessage(chatMessage, context);
		assertFalse(response.isEmpty());
	}
}
