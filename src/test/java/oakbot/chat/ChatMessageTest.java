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
}
