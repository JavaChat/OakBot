package oakbot.listener.chatgpt;

import static org.junit.Assert.assertEquals;

import java.util.List;

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

	@Test
	public void extractImageUrls() {
		List<String> actual = ChatGPT.extractImageUrls("Contains no image URLs.");
		List<String> expected = List.of();
		assertEquals(expected, actual);

		actual = ChatGPT.extractImageUrls("Contains one image URL [http://www.example.com/image.png](http://www.example.com/image.png).");
		expected = List.of("http://www.example.com/image.png");
		assertEquals(expected, actual);

		actual = ChatGPT.extractImageUrls("http://www.example.com/image.png Beginning of string.");
		expected = List.of("http://www.example.com/image.png");
		assertEquals(expected, actual);

		actual = ChatGPT.extractImageUrls("Middle of http://www.example.com/image.png string.");
		expected = List.of("http://www.example.com/image.png");
		assertEquals(expected, actual);

		actual = ChatGPT.extractImageUrls("End of string http://www.example.com/image.png");
		expected = List.of("http://www.example.com/image.png");
		assertEquals(expected, actual);

		actual = ChatGPT.extractImageUrls("Non-image URL http://www.example.com/page.html.");
		expected = List.of();
		assertEquals(expected, actual);

		actual = ChatGPT.extractImageUrls("Two http://www.example.com/image.jpg URLs http://www.example.com/image.gif.");
		expected = List.of("http://www.example.com/image.jpg", "http://www.example.com/image.gif");
		assertEquals(expected, actual);
	}
}
