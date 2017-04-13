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
			.roomId(1)
			.content(message)
		.build();
		//@formatter:on

		MentionListenerMock mentionListener = new MentionListenerMock("OakBot");
		WaveListener listener = new WaveListener("OakBot", mentionListener);

		ChatResponse chatResponse = listener.onMessage(chatMessage, null);
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
			.roomId(1)
			.content("o/")
		.build();
		//@formatter:on

		MentionListenerMock mentionListener = new MentionListenerMock("OakBot");
		WaveListener listener = new WaveListener("OakBot", mentionListener);

		ChatResponse chatResponse = listener.onMessage(chatMessage, null);
		assertEquals("\\o", chatResponse.getMessage());
		chatResponse = listener.onMessage(chatMessage, null);
		assertNull(chatResponse);
	}

	@Test
	public void room_specific() {
		//@formatter:off
		ChatMessage chatMessage1 = new ChatMessage.Builder()
			.roomId(1)
			.content("o/")
		.build();
		ChatMessage chatMessage2 = new ChatMessage.Builder()
			.roomId(2)
			.content("o/")
		.build();
		//@formatter:on

		MentionListenerMock mentionListener = new MentionListenerMock("OakBot");
		WaveListener listener = new WaveListener("OakBot", mentionListener);

		ChatResponse chatResponse = listener.onMessage(chatMessage1, null);
		assertEquals("\\o", chatResponse.getMessage());

		chatResponse = listener.onMessage(chatMessage2, null);
		assertEquals("\\o", chatResponse.getMessage());

		chatResponse = listener.onMessage(chatMessage1, null);
		assertNull(chatResponse);

		chatResponse = listener.onMessage(chatMessage2, null);
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
