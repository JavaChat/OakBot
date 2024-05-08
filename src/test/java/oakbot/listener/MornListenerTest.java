package oakbot.listener;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.IBot;

/**
 * @author Michael Angstadt
 */
public class MornListenerTest {
	@Test
	public void onMessage() {
		assertMorn("morn", "morn", false);
		assertMorn("Morn", "morn", false);
		assertMorn("morning", "Morning.", false);
		assertMorn("good morning", "Good morning.", false);
		assertMorn("Morn!., ", "morn", false);
		assertMorn("Morning, @Bob! ", null, false);
		assertMorn("Morning, @Oak! ", ":0 Morning.", true);
		assertMorn("@Oak Morning! ", ":0 Morning.", true);
		assertMorn("Morning, @Oak @Bob! ", ":0 Morning.", true);
		assertMorn("hey", null, false);
		assertMorn("morning guys", null, false);
	}

	private static void assertMorn(String message, String response, boolean ignoreNextMentionListenerMessage) {
		//@formatter:off
		var chatMessage = new ChatMessage.Builder()
			.roomId(1)
			.content(message)
		.build();
		//@formatter:on
		
		var bot = mock(IBot.class);
		when(bot.getUsername()).thenReturn("OakBot");

		var mentionListener = new MentionListenerMock();
		var listener = new MornListener("PT0S", mentionListener);

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
			.content("morn")
		.build();
		//@formatter:on
		
		var bot = mock(IBot.class);
		when(bot.getUsername()).thenReturn("OakBot");

		var mentionListener = new MentionListenerMock();
		var listener = new MornListener("PT0S", mentionListener);

		var chatResponse = listener.onMessage(chatMessage, bot);
		assertMessage("morn", chatResponse);
		chatResponse = listener.onMessage(chatMessage, bot);
		assertTrue(chatResponse.isEmpty());
	}

	@Test
	public void room_specific() {
		//@formatter:off
		var chatMessage1 = new ChatMessage.Builder()
			.roomId(1)
			.content("morn")
		.build();
		var chatMessage2 = new ChatMessage.Builder()
			.roomId(2)
			.content("morn")
		.build();
		//@formatter:on
		
		var bot = mock(IBot.class);
		when(bot.getUsername()).thenReturn("OakBot");

		var mentionListener = new MentionListenerMock();
		var listener = new MornListener("PT0S", mentionListener);

		var chatResponse = listener.onMessage(chatMessage1, bot);
		assertMessage("morn", chatResponse);

		chatResponse = listener.onMessage(chatMessage2, bot);
		assertMessage("morn", chatResponse);

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
