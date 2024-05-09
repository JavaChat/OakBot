package oakbot.listener;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.IBot;

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
		var chatMessage = new ChatMessage.Builder()
			.roomId(1)
			.content(message)
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getUsername()).thenReturn("OakBot");

		var mentionListener = new MentionListenerMock();
		var listener = new WaveListener("PT0S", mentionListener);

		var chatResponse = listener.onMessage(chatMessage, bot);
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
		var chatMessage = new ChatMessage.Builder()
			.roomId(1)
			.content("o/")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var mentionListener = new MentionListenerMock();
		var listener = new WaveListener("PT0S", mentionListener);

		var chatResponse = listener.onMessage(chatMessage, bot);
		assertMessage("\\o", chatResponse);
		chatResponse = listener.onMessage(chatMessage, bot);
		assertTrue(chatResponse.isEmpty());
	}

	@Test
	public void always_wave_to_admins() {
		//@formatter:off
		var chatMessage = new ChatMessage.Builder()
			.roomId(1)
			.userId(10)
			.content("o/")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getAdminUsers()).thenReturn(List.of(10));

		var mentionListener = new MentionListenerMock();
		var listener = new WaveListener("PT0S", mentionListener);

		var chatResponse = listener.onMessage(chatMessage, bot);
		assertMessage("\\o", chatResponse);
		chatResponse = listener.onMessage(chatMessage, bot);
		assertMessage("\\o", chatResponse);
	}

	@Test
	public void room_specific() {
		//@formatter:off
		var chatMessage1 = new ChatMessage.Builder()
			.roomId(1)
			.content("o/")
		.build();
		var chatMessage2 = new ChatMessage.Builder()
			.roomId(2)
			.content("o/")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var mentionListener = new MentionListenerMock();
		var listener = new WaveListener("PT0S", mentionListener);

		var chatResponse = listener.onMessage(chatMessage1, bot);
		assertMessage("\\o", chatResponse);

		chatResponse = listener.onMessage(chatMessage2, bot);
		assertMessage("\\o", chatResponse);

		chatResponse = listener.onMessage(chatMessage1, bot);
		assertTrue(chatResponse.isEmpty());

		chatResponse = listener.onMessage(chatMessage2, bot);
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
