package oakbot.listener;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.IBot;

/**
 * @author Michael Angstadt
 */
class MentionListenerTest {
	@Test
	void respond() {
		assertResponse("Hey @OakBot", ":0 Type `/help` to see all my commands.");
		assertResponse("Hey @Oak", ":0 Type `/help` to see all my commands.");
		assertResponse("@Oak Thank  you", ":0 You're welcome.");
		assertResponse("Thanks @Oak", ":0 You're welcome.");
		assertResponse("@Oak  thx!", ":0 You're welcome.");
		assertNoResponse("Hey @OakBott");
		assertNoResponse("Hey");
	}

	private static void assertNoResponse(String message) {
		//@formatter:off
		var chatMessage = new ChatMessage.Builder()
			.content(message)
		.build();
		//@formatter:on
		
		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		when(bot.getUsername()).thenReturn("OakBot");

		var listener = new MentionListener();
		
		var actions = listener.onMessage(chatMessage, bot);
		assertTrue(actions.isEmpty());
	}

	private static void assertResponse(String message, String expectedResponse) {
		//@formatter:off
		var chatMessage = new ChatMessage.Builder()
			.content(message)
		.build();
		//@formatter:on
		
		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		when(bot.getUsername()).thenReturn("OakBot");

		var listener = new MentionListener();
		
		var actions = listener.onMessage(chatMessage, bot);
		assertMessage(expectedResponse, actions);
	}

	@Test
	void prevent_spam() {
		//@formatter:off
		var chatMessage = new ChatMessage.Builder()
			.content("Hey @Oakbot")
		.build();
		//@formatter:on
		
		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		when(bot.getUsername()).thenReturn("OakBot");

		var listener = new MentionListener();

		var response = listener.onMessage(chatMessage, bot);
		assertFalse(response.isEmpty());

		response = listener.onMessage(chatMessage, bot);
		assertTrue(response.isEmpty());
	}

	@Test
	void ignore_next_message() {
		//@formatter:off
		var chatMessage = new ChatMessage.Builder()
			.content("Hey @Oakbot")
		.build();
		//@formatter:on
		
		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		when(bot.getUsername()).thenReturn("OakBot");

		var listener = new MentionListener();
		listener.ignoreNextMessage();

		var response = listener.onMessage(chatMessage, bot);
		assertTrue(response.isEmpty());

		response = listener.onMessage(chatMessage, bot);
		assertFalse(response.isEmpty());
	}
}
