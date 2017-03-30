package oakbot.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;

/**
 * @author Michael Angstadt
 */
public class WaveListenerTest {
	@Test
	public void onMessage() {
		assertWave("o/", "\\o", false);
		assertWave("\\o", "o/", false);
		assertWave("hey o/", null, false);
		assertWave("@OakBot hey o/", "\\o", true);
		assertWave("@Oak http://www.g.oo/ link", null, false);
		assertWave("hey", null, false);
	}

	private static void assertWave(String message, String response, boolean ignoreNextMentionListenerMessage) {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.content(message)
		.build();
		//@formatter:on

		MentionListenerMock mentionListener = new MentionListenerMock("OakBot");
		WaveListener listener = new WaveListener("OakBot", mentionListener);

		ChatResponse chatResponse = listener.onMessage(chatMessage, false);
		if (response == null) {
			assertNull(chatResponse);
		} else {
			assertEquals(response, chatResponse.getMessage());
		}

		assertEquals(ignoreNextMentionListenerMessage, mentionListener.ignored);
	}

	@Test
	public void spam_protection() {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.content("o/")
		.build();
		//@formatter:on

		MentionListenerMock mentionListener = new MentionListenerMock("OakBot");
		WaveListener listener = new WaveListener("OakBot", mentionListener);

		ChatResponse chatResponse = listener.onMessage(chatMessage, false);
		assertEquals("\\o", chatResponse.getMessage());
		chatResponse = listener.onMessage(chatMessage, false);
		assertNull(chatResponse);
	}

	private static class MentionListenerMock extends MentionListener {
		private boolean ignored = false;

		public MentionListenerMock(String botUsername) {
			super(botUsername, "/");
		}

		@Override
		public void ignoreNextMessage() {
			ignored = true;
		}
	}
}
