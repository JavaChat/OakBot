package oakbot.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Contains utility methods for asserting the contents of a {@link ChatActions}
 * object.
 * @author Michael Angstadt
 */
public class ChatActionsUtils {
	/**
	 * Asserts the contents of a {@link ChatActions} object that contains only a
	 * single {@link PostMessage} action.
	 * @param expected the expected message
	 * @param actions the actions
	 */
	public static void assertMessage(String expected, ChatActions actions) {
		PostMessage actual = getFirstPostMessage(actions);
		assertEquals(expected, actual.message());
	}

	/**
	 * Asserts the contents of a {@link ChatActions} object that contains only a
	 * single {@link PostMessage} action.
	 * @param expected the text that the message should start with
	 * @param actions the actions
	 */
	public static void assertMessageStartsWith(String expected, ChatActions actions) {
		PostMessage actual = getFirstPostMessage(actions);
		assertTrue(actual.message().startsWith(expected));
	}

	/**
	 * Asserts the contents of a {@link ChatActions} object that contains only a
	 * single {@link PostMessage} action.
	 * @param expected the expected object
	 * @param actions the actions
	 */
	public static void assertPostMessage(PostMessage expected, ChatActions actions) {
		PostMessage actual = getFirstPostMessage(actions);
		assertEquals(expected, actual);
	}

	private static PostMessage getFirstPostMessage(ChatActions actions) {
		assertEquals("ChatActions object contains more than one action.", 1, actions.getActions().size());
		return (PostMessage) actions.getActions().get(0);
	}
}
