package oakbot.listener;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.PostMessage;
import oakbot.chat.ChatMessage;
import oakbot.util.Sleeper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.fail;

/**
 * @author Michael Angstadt
 */
public class DadJokeListenerTest {
	private final static BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), 0);

	@BeforeClass
	public static void beforeClass() {
		Sleeper.unitTest = true;
	}

	@AfterClass
	public static void afterClass() {
		Sleeper.unitTest = false;
	}

	@Test
	public void onMessage_i_or_i_am() {
		assertResponse("I'm working on a project.", "Hi working on a project, I'm Oak!");
		assertResponse("I am working on a project.", "Hi working on a project, I'm Oak!");
	}

	@Test
	public void onMessage_case_insensitive() {
		assertResponse("I'm working on a project.", "Hi working on a project, I'm Oak!");
		assertResponse("i'm working on a project.", "Hi working on a project, I'm Oak!");
	}

	@Test
	public void onMessage_termination() {
		assertResponse("I'm working on a project", "Hi working on a project, I'm Oak!");
		assertResponse("I'm working on a project. Please help!", "Hi working on a project, I'm Oak!");
		assertResponse("I'm working on a project! Please help!", "Hi working on a project, I'm Oak!");
		assertResponse("I'm working on a project? Please help!", "Hi working on a project, I'm Oak!");
		assertResponse("I'm working on a project; Please help!", "Hi working on a project, I'm Oak!");
		assertResponse("I'm working on a project, Please help!", "Hi working on a project, I'm Oak!");
		assertResponse("I'm working on a project\nPlease help!", "Hi working on a project, I'm Oak!");
		assertResponse("I'm working on a project and need help", "Hi working on a project, I'm Oak!");
		assertResponse("I'm mining andesite", "Hi mining andesite, I'm Oak!"); //"and" must be its own word
	}

	@Test
	public void onMessage_length() {
		assertNoResponse("I'm working on a really important project.");
	}

	@Test
	public void onMessage_middle_of_sentence() {
		assertNoResponse("My boss is an idiot and I'm really mad.");
	}

	@Test
	public void onMessage_html_entities() {
		assertResponse("I&#39;m working on a project", "Hi working on a project, I'm Oak!");
	}

	@Test
	public void onMessage_html_formatting() {
		assertResponse("I'm <i>working</i> on a project", "Hi *working* on a project, I'm Oak!");
	}

	@Test
	public void onMessage_remove_links() {
		assertResponse("I'm <a href=\"https://www.google.com\">working</a> on a project", "Hi working on a project, I'm Oak!");
		assertResponse("I'm working on this https://www.google.com", "Hi working on this, I'm Oak!");
	}

	@Test
	public void onMessage_empty_phrase() {
		assertNoResponse("I'm");
		assertNoResponse("I'm.");
		assertNoResponse("I'm and");
	}

	@Test
	public void onMessage_examples() {
		assertResponse("is anybody here?? i am confused and need help!", "Hi confused, I'm Oak!");
		assertResponse("I'm playing around with Java syntax and I got this compiler error", "Hi playing around with Java syntax, I'm Oak!");
		assertResponse("I'm working on a project and I need to record hystrix metrics and save it in my internal database", "Hi working on a project, I'm Oak!");
		assertNoResponse("is anybody here??");
	}

	@Test
	public void onMessage_replies_and_mentions() {
		assertResponse("@Michael I'm confused", "Hi confused, I'm Oak!");
		assertResponse(":1234 I'm playing around with Java syntax", "Hi playing around with Java syntax, I'm Oak!");
		assertNoResponse("@Michael abcd I'm working on a project and I need to record hystrix metrics and save it in my internal database");
		assertNoResponse(":1234 abcd I'm working on a project and blah blah");
		assertResponse(":1234 Hey! I'm Oak", "Hi Oak, I'm Oak!");
	}
	
	private static void assertResponse(String message, String response) {
		ChatMessage chatMessage = new ChatMessage.Builder() //@formatter:off
			.messageId(1)
			.roomId(1)
			.content(message)
			.build(); //@formatter:on

		DadJokeListener listener = new DadJokeListener("Oak");
		ChatActions actions = listener.onMessage(chatMessage, context);
		assertMessage(":1 " + response, actions);
	}

	private static void assertNoResponse(String message) {
		ChatMessage chatMessage = new ChatMessage.Builder() //@formatter:off
			.messageId(1)
			.roomId(1)
			.content(message)
			.build(); //@formatter:on

		DadJokeListener listener = new DadJokeListener("Oak");
		ChatActions actions = listener.onMessage(chatMessage, context);
		if (!actions.isEmpty()) {
			PostMessage postMessage = (PostMessage)actions.getActions().get(0);
			fail("The following response was returned when no response was expected: " + postMessage.message());
		}
	}
}
