package oakbot.listener;

import static org.junit.Assert.assertEquals;
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
	public void mentioned() {
		assertMentioned("Hey @OakBot", true);
		assertMentioned("Hey @Oak", true);
		assertMentioned("Hey @OakBott", false);
		assertMentioned("Hey", false);
	}

	private static void assertMentioned(String message, boolean mentioned) {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.content(message)
		.build();
		//@formatter:on

		MentionListener listener = new MentionListener("OakBot");
		ChatActions response = listener.onMessage(chatMessage, context);
		assertEquals(mentioned, !response.isEmpty());
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
