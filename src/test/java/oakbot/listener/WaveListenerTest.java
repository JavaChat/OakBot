package oakbot.listener;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.chat.ChatMessage;

/**
 * @author Michael Angstadt
 */
public class WaveListenerTest {
	private final static BotContext context = new BotContext(false, "/", "OakBot", 0, null, Collections.emptyList(), Collections.emptyList(), 0);

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

		MentionListenerMock mentionListener = new MentionListenerMock();
		WaveListener listener = new WaveListener("PT0S", mentionListener);

		ChatActions chatResponse = listener.onMessage(chatMessage, context);
		if (response == null) {
			assertTrue(chatResponse.isEmpty());
		} else {
			assertMessage(response, chatResponse);
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

		MentionListenerMock mentionListener = new MentionListenerMock();
		WaveListener listener = new WaveListener("PT0S", mentionListener);

		ChatActions chatResponse = listener.onMessage(chatMessage, context);
		assertMessage("\\o", chatResponse);
		chatResponse = listener.onMessage(chatMessage, context);
		assertTrue(chatResponse.isEmpty());
	}

	@Test
	public void always_wave_to_admins() {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.roomId(1)
			.content("o/")
		.build();
		//@formatter:on

		MentionListenerMock mentionListener = new MentionListenerMock();
		WaveListener listener = new WaveListener("PT0S", mentionListener);

		BotContext context = new BotContext(true, "/", "", 0, null, Collections.emptyList(), Collections.emptyList(), 0);
		ChatActions chatResponse = listener.onMessage(chatMessage, context);
		assertMessage("\\o", chatResponse);
		chatResponse = listener.onMessage(chatMessage, context);
		assertMessage("\\o", chatResponse);
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

		MentionListenerMock mentionListener = new MentionListenerMock();
		WaveListener listener = new WaveListener("PT0S", mentionListener);

		ChatActions chatResponse = listener.onMessage(chatMessage1, context);
		assertMessage("\\o", chatResponse);

		chatResponse = listener.onMessage(chatMessage2, context);
		assertMessage("\\o", chatResponse);

		chatResponse = listener.onMessage(chatMessage1, context);
		assertTrue(chatResponse.isEmpty());

		chatResponse = listener.onMessage(chatMessage2, context);
		assertTrue(chatResponse.isEmpty());
	}

	private static class MentionListenerMock extends MentionListener {
		private boolean ignored = false;

		@Override
		public void ignoreNextMessage() {
			ignored = true;
		}
	}
}
