package oakbot.listener;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.IBot;

/**
 * @author Michael Angstadt
 */
class MornListenerTest {
	@Test
	void onMessage() {
		assertResponse("morn", "morn");
		assertResponse("Morn", "morn");
		assertResponse("morning", "Morning.");
		assertResponse("good morning", "Good morning.");
		assertResponse("good mourning", "Good mourning.");
		assertResponse("good moaning", "Good moaning.");
		assertResponse("goat morning", "Goat morning.");
		assertResponse("goat mourning", "Goat mourning.");
		assertResponse("goat moaning", "Goat moaning.");
	}

	@Test
	void onMessage_trim() {
		assertResponse(" morn  ", "morn");
	}

	@Test
	void onMessage_ignore_case() {
		assertResponse("MoRn", "morn");
	}

	@Test
	void onMessage_ignore_puncuation() {
		assertResponse("morn!", "morn");
		assertResponse("morn.", "morn");
		assertResponse("morn,", "morn");
	}

	@Test
	void onMessage_do_not_respond_when_user_is_mentioned() {
		assertNoResponse("Morning, @Bob!");
	}

	@Test
	void onMessage_reply_when_bot_mentioned() {
		assertResponseMentionListenerShouldIgnoreNextMessage("Morning, @Oak!", ":0 Morning.");
		assertResponseMentionListenerShouldIgnoreNextMessage("@Oak Morning!", ":0 Morning.");
		assertResponseMentionListenerShouldIgnoreNextMessage("Morning, @Oak @Bob!", ":0 Morning.");
	}

	@Test
	void onMessage_no_response() {
		assertNoResponse("hey");
		assertNoResponse("morning guys");
	}

	@Test
	void onMessage_yee() {
		assertResponse("yee", "[yee](https://youtu.be/q6EoRBvdVPQ)");
		assertResponse("yeeeeeee", "[yee](https://youtu.be/q6EoRBvdVPQ)");
		assertResponse("Yee.", "[yee](https://youtu.be/q6EoRBvdVPQ)");
		assertResponse("Yeeeeeee.", "[yee](https://youtu.be/q6EoRBvdVPQ)");
		assertNoResponse("ye");
	}

	private static void assertResponse(String message, String response) {
		assertResponse(message, response, false);
	}

	private static void assertResponseMentionListenerShouldIgnoreNextMessage(String message, String response) {
		assertResponse(message, response, true);
	}

	private static void assertNoResponse(String message) {
		assertResponse(message, null, false);
	}

	private static void assertResponse(String message, String response, boolean ignoreNextMentionListenerMessage) {
		//@formatter:off
		var chatMessage = new ChatMessage.Builder()
			.roomId(1)
			.content(message)
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getUsername()).thenReturn("OakBot");
		when(bot.getUserId()).thenReturn(1);

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
	void spam_protection() {
		//@formatter:off
		var chatMessage = new ChatMessage.Builder()
			.roomId(1)
			.content("morn")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getUsername()).thenReturn("OakBot");
		when(bot.getUserId()).thenReturn(1);

		var mentionListener = new MentionListenerMock();
		var listener = new MornListener("PT0S", mentionListener);

		var chatResponse = listener.onMessage(chatMessage, bot);
		assertMessage("morn", chatResponse);
		chatResponse = listener.onMessage(chatMessage, bot);
		assertTrue(chatResponse.isEmpty());
	}

	@Test
	void room_specific() {
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
		when(bot.getUserId()).thenReturn(1);

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
