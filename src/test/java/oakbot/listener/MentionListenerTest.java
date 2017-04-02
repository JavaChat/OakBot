package oakbot.listener;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;

/**
 * @author Michael Angstadt
 */
public class MentionListenerTest {
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

		MentionListener listener = new MentionListener("OakBot", "/");
		ChatResponse response = listener.onMessage(chatMessage, null);
		assertEquals(mentioned, response != null);
	}
}
