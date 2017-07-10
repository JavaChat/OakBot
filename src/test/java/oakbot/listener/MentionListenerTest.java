package oakbot.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
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
		ChatResponse response = listener.onMessage(chatMessage, context);
		assertEquals(mentioned, response != null);
	}

	@Test
	public void prevent_spam() {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.content("Hey @Oakbot")
		.build();
		//@formatter:on

		MentionListener listener = new MentionListener("OakBot");

		ChatResponse response = listener.onMessage(chatMessage, context);
		assertNotNull(response);

		response = listener.onMessage(chatMessage, context);
		assertNull(response);
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

		ChatResponse response = listener.onMessage(chatMessage, context);
		assertNull(response);

		response = listener.onMessage(chatMessage, context);
		assertNotNull(response);
	}
}
