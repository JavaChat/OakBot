package oakbot.chat;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class ChatMessageTest {
	@Test
	public void getMentions() {
		assertMention("Hello, @Frank.", "Frank");
		assertMention("Hello, @Frank2Cool.", "Frank2Cool");
		assertMention("Hello@Frank.", "Frank");
		assertMention("Hello, @Frank", "Frank");
		assertMention("Hello, @@Frank", "Frank");
		assertMention("Hello, @Frank and @Robert", "Frank", "Robert");
		assertMention("Hello, @Fr an");
		assertMention("Hello.");
	}

	private static void assertMention(String message, String... expectedMentions) {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.content(message)
		.build();
		//@formatter:on

		assertEquals(Arrays.asList(expectedMentions), chatMessage.getMentions());
	}

	@Test
	public void isMentioned() {
		assertIsMentioned("Hello, @FrankSmi", "frank smith", true);
		assertIsMentioned("Hello", "frank smith", false);
		assertIsMentioned("Hello, @FrankSmi", "bob", false);
	}

	private static void assertIsMentioned(String message, String username, boolean expected) {
		//@formatter:off
		ChatMessage chatMessage = new ChatMessage.Builder()
			.content(message)
		.build();
		//@formatter:on

		assertEquals(expected, chatMessage.isMentioned(username));
	}
}
