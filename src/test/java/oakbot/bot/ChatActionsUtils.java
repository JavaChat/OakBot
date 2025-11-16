package oakbot.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		assertMessage(expected, 0, actions);
	}

	/**
	 * Asserts the contents of a {@link ChatActions} object that contains only a
	 * single {@link PostMessage} action.
	 * @param expected the expected message
	 * @param expectedParentId the expected parent ID
	 * @param actions the actions
	 */
	public static void assertMessage(String expected, long expectedParentId, ChatActions actions) {
		var actual = getFirstPostMessage(actions);
		assertEquals(expected, actual.message());
		assertEquals(expectedParentId, actual.parentId());
	}

	/**
	 * Asserts the contents of a {@link ChatActions} object that contains only a
	 * single {@link PostMessage} action.
	 * @param expected the text that the message should start with
	 * @param actions the actions
	 */
	public static void assertMessageStartsWith(String expected, ChatActions actions) {
		assertMessageStartsWith(expected, 0, actions);
	}

	/**
	 * Asserts the contents of a {@link ChatActions} object that contains only a
	 * single {@link PostMessage} action.
	 * @param expected the text that the message should start with
	 * @param expectedParentId the expected parent ID
	 * @param actions the actions
	 */
	public static void assertMessageStartsWith(String expected, long expectedParentId, ChatActions actions) {
		var actual = getFirstPostMessage(actions);
		assertTrue(actual.message().startsWith(expected));
		assertEquals(expectedParentId, actual.parentId());
	}

	/**
	 * Asserts the contents of a {@link ChatActions} object that contains only a
	 * single {@link PostMessage} action.
	 * @param expected the expected object
	 * @param actions the actions
	 */
	public static void assertPostMessage(PostMessage expected, ChatActions actions) {
		var actual = getFirstPostMessage(actions);
		assertEquals(expected, actual);
	}

	private static PostMessage getFirstPostMessage(ChatActions actions) {
		assertEquals(1, actions.getActions().size(), () -> "ChatActions object is empty or contains more than one action.");
		return (PostMessage) actions.getActions().get(0);
	}
}
