package oakbot.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;

/**
 * @author Michael Angstadt
 */
public class WaveListenerTest {
	private final static BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), 0);

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
		WaveListener listener = new WaveListener("OakBot", 0, mentionListener);

		ChatResponse chatResponse = listener.onMessage(chatMessage, context);
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
		WaveListener listener = new WaveListener("OakBot", 0, mentionListener);

		ChatResponse chatResponse = listener.onMessage(chatMessage, context);
		assertEquals("\\o", chatResponse.getMessage());
		chatResponse = listener.onMessage(chatMessage, context);
		assertNull(chatResponse);
	}

	@Test
	public void always_wave_to_admins() {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.roomId(1)
			.content("o/")
		.build();
		//@formatter:on

		MentionListenerMock mentionListener = new MentionListenerMock("OakBot");
		WaveListener listener = new WaveListener("OakBot", 0, mentionListener);

		BotContext context = new BotContext(true, "/", null, Collections.emptyList(), Collections.emptyList(), 0);
		ChatResponse chatResponse = listener.onMessage(chatMessage, context);
		assertEquals("\\o", chatResponse.getMessage());
		chatResponse = listener.onMessage(chatMessage, context);
		assertEquals("\\o", chatResponse.getMessage());
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
		WaveListener listener = new WaveListener("OakBot", 0, mentionListener);

		ChatResponse chatResponse = listener.onMessage(chatMessage1, context);
		assertEquals("\\o", chatResponse.getMessage());

		chatResponse = listener.onMessage(chatMessage2, context);
		assertEquals("\\o", chatResponse.getMessage());

		chatResponse = listener.onMessage(chatMessage1, context);
		assertNull(chatResponse);

		chatResponse = listener.onMessage(chatMessage2, context);
		assertNull(chatResponse);
	}

	private static class MentionListenerMock extends MentionListener {
		private boolean ignored = false;

		public MentionListenerMock(String botUsername) {
			super(botUsername);
		}

		@Override
		public void ignoreNextMessage() {
			ignored = true;
		}
	}
}
