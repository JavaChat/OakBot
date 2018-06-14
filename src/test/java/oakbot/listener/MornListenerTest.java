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
public class MornListenerTest {
	private final static BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), 0);
	private final static String botName = "OakBot";

	@Test
	public void onMessage() {
		assertMorn("morn", "morn", false);
		assertMorn("Morn", "morn", false);
		assertMorn("morno", "morno", false);
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
		ChatMessage chatMessage = new ChatMessage.Builder()
			.roomId(1)
			.content(message)
		.build();
		//@formatter:on

		MentionListenerMock mentionListener = new MentionListenerMock();
		MornListener listener = new MornListener(botName, 0, mentionListener);

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
			.content("morn")
		.build();
		//@formatter:on

		MentionListenerMock mentionListener = new MentionListenerMock();
		MornListener listener = new MornListener("OakBot", 0, mentionListener);

		ChatResponse chatResponse = listener.onMessage(chatMessage, context);
		assertEquals("morn", chatResponse.getMessage());
		chatResponse = listener.onMessage(chatMessage, context);
		assertNull(chatResponse);
	}

	@Test
	public void room_specific() {
		//@formatter:off
		ChatMessage chatMessage1 = new ChatMessage.Builder()
			.roomId(1)
			.content("morn")
		.build();
		ChatMessage chatMessage2 = new ChatMessage.Builder()
			.roomId(2)
			.content("morn")
		.build();
		//@formatter:on

		MentionListenerMock mentionListener = new MentionListenerMock();
		MornListener listener = new MornListener("OakBot", 0, mentionListener);

		ChatResponse chatResponse = listener.onMessage(chatMessage1, context);
		assertEquals("morn", chatResponse.getMessage());

		chatResponse = listener.onMessage(chatMessage2, context);
		assertEquals("morn", chatResponse.getMessage());

		chatResponse = listener.onMessage(chatMessage1, context);
		assertNull(chatResponse);

		chatResponse = listener.onMessage(chatMessage2, context);
		assertNull(chatResponse);
	}

	private static class MentionListenerMock extends MentionListener {
		private boolean ignored = false;

		public MentionListenerMock() {
			super(botName);
		}

		@Override
		public void ignoreNextMessage() {
			ignored = true;
		}
	}
}
