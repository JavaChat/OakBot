package oakbot.listener.chatgpt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class ChatGPTTest {
	@Test
	public void removeMentionsFromBeginningOfMessage() {
		assertRemoveMentionsFromBeginningOfMessage("@Foo @Bar", "");
		assertRemoveMentionsFromBeginningOfMessage("@Foo @Bar Hello @Bob world", "Hello @Bob world");
		assertRemoveMentionsFromBeginningOfMessage("@Foo @Bar Hello world", "Hello world");
		assertRemoveMentionsFromBeginningOfMessage("Hello @Bob world", "Hello @Bob world");
		assertRemoveMentionsFromBeginningOfMessage("Hello world", "Hello world");
	}

	private static void assertRemoveMentionsFromBeginningOfMessage(String input, String expected) {
		String actual = ChatGPT.removeMentionsFromBeginningOfMessage(input);
		assertEquals(expected, actual);
	}

	@Test
	public void removeReplySyntaxFromBeginningOfMessage() {
		assertRemoveReplySyntaxFromBeginningOfMessage(":123456", "");
		assertRemoveReplySyntaxFromBeginningOfMessage(":123456 Hello world", "Hello world");
		assertRemoveReplySyntaxFromBeginningOfMessage("Hello world", "Hello world");
	}

	private static void assertRemoveReplySyntaxFromBeginningOfMessage(String input, String expected) {
		String actual = ChatGPT.removeReplySyntaxFromBeginningOfMessage(input);
		assertEquals(expected, actual);
	}
	
	@Test
	public void formatMessagesWithCodeBlocks() {
		assertFormatMessagesWithCodeBlocks("No code blocks here", "No code blocks here");
		
		//@formatter:off
		assertFormatMessagesWithCodeBlocks(
		"Line 1\n" +
		"\n" +
		"```php\n" +
		"while (true) {\n" +
		"  echo 'Foo';\n" +
		"}\n" +
		"```\n" +
		"\n" +
		"Line 2",
		
		"    Line 1\n" +
		"    \n" +
		"    while (true) {\n" +
		"      echo 'Foo';\n" +
		"    }\n" +
		"    \n" +
		"    Line 2"
		);
		
		//no language identifier
		assertFormatMessagesWithCodeBlocks(
		"Line 1\n" +
		"\n" +
		"```\n" +
		"while (true) {\n" +
		"  echo 'Foo';\n" +
		"}\n" +
		"```\n" +
		"\n" +
		"Line 2",
		
		"    Line 1\n" +
		"    \n" +
		"    while (true) {\n" +
		"      echo 'Foo';\n" +
		"    }\n" +
		"    \n" +
		"    Line 2"
		);
		
		//no newline after terminating ```
		assertFormatMessagesWithCodeBlocks(
		"Line 1\n" +
		"\n" +
		"```php\n" +
		"while (true) {\n" +
		"  echo 'Foo';\n" +
		"}\n" +
		"```",
		
		"    Line 1\n" +
		"    \n" +
		"    while (true) {\n" +
		"      echo 'Foo';\n" +
		"    }\n" +
		"    "
		);
		
		//indented code blocks
		assertFormatMessagesWithCodeBlocks(
		"Line 1\n" +
		"\n" +
		"1. Step One\n" +
		"  ```php\n" +
		"  while (true) {\n" +
		"    echo 'Foo';\n" +
		"  }\n" +
		"  ```\n" +
		"\n" +
		"Line 2",
		
		"    Line 1\n" +
		"    \n" +
		"    1. Step One\n" +
		"      while (true) {\n" +
		"        echo 'Foo';\n" +
		"      }\n" +
		"    \n" +
		"    Line 2"
		);
		//@formatter:on
	}
	
	private static void assertFormatMessagesWithCodeBlocks(String input, String expected) {
		String actual = ChatGPT.formatMessagesWithCodeBlocks(input);
		assertEquals(expected, actual);
	}
}
