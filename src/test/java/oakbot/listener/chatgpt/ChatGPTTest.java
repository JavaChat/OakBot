package oakbot.listener.chatgpt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Angstadt
 */
class ChatGPTTest {
	@Test
	void removeMentionsFromBeginningOfMessage() {
		assertRemoveMentionsFromBeginningOfMessage("@Foo @Bar", "");
		assertRemoveMentionsFromBeginningOfMessage("@Foo @Bar Hello @Bob world", "Hello @Bob world");
		assertRemoveMentionsFromBeginningOfMessage("@Foo @Bar Hello world", "Hello world");
		assertRemoveMentionsFromBeginningOfMessage("Hello @Bob world", "Hello @Bob world");
		assertRemoveMentionsFromBeginningOfMessage("Hello world", "Hello world");
	}

	private static void assertRemoveMentionsFromBeginningOfMessage(String input, String expected) {
		var actual = ChatGPT.removeMentionsFromBeginningOfMessage(input);
		assertEquals(expected, actual);
	}

	@Test
	void removeReplySyntaxFromBeginningOfMessage() {
		assertRemoveReplySyntaxFromBeginningOfMessage(":123456", "");
		assertRemoveReplySyntaxFromBeginningOfMessage(":123456 Hello world", "Hello world");
		assertRemoveReplySyntaxFromBeginningOfMessage("Hello world", "Hello world");
	}

	private static void assertRemoveReplySyntaxFromBeginningOfMessage(String input, String expected) {
		var actual = ChatGPT.removeReplySyntaxFromBeginningOfMessage(input);
		assertEquals(expected, actual);
	}

	@Test
	void formatMessagesWithCodeBlocks() {
		assertFormatMessagesWithCodeBlocks("No code blocks here", "No code blocks here");

		//@formatter:off
		assertFormatMessagesWithCodeBlocks(
		"""
		Line 1
		
		```php
		while (true) {
		  echo 'Foo';
		}
		```
		
		Line 2""",
		
		"""
		    Line 1
		   \s
		    while (true) {
		      echo 'Foo';
		    }
		   \s
		    Line 2
		"""
		);
		
		//no language identifier
		assertFormatMessagesWithCodeBlocks(
		"""
		Line 1
		
		```
		while (true) {
		  echo 'Foo';
		}
		```
		
		Line 2""",
		
		"""
		    Line 1
		   \s
		    while (true) {
		      echo 'Foo';
		    }
		   \s
		    Line 2
		"""
		);
		
		//no newline after terminating ```
		assertFormatMessagesWithCodeBlocks(
		"""
		Line 1
		
		```php
		while (true) {
		  echo 'Foo';
		}
		```""",
		
		"""
		    Line 1
		   \s
		    while (true) {
		      echo 'Foo';
		    }
		   \s
		"""
		);
		
		//indented code blocks
		assertFormatMessagesWithCodeBlocks(
		"""
		Line 1
		
		1. Step One
		  ```php
		  while (true) {
		    echo 'Foo';
		  }
		  ```
		
		Line 2""",
		
		"""
		    Line 1
		   \s
		    1. Step One
		      while (true) {
		        echo 'Foo';
		      }
		   \s
		    Line 2
		"""
		);
		//@formatter:on
	}

	private static void assertFormatMessagesWithCodeBlocks(String input, String expected) {
		/*
		 * Address issue with Java text blocks where they end with a newline
		 * when we don't want them to.
		 */
		if (expected.endsWith("\n")) {
			expected = expected.substring(0, expected.length() - 1);
		}

		var actual = ChatGPT.formatMessagesWithCodeBlocks(input);
		assertEquals(expected, actual);
	}

	@Test
	void extractUrls() {
		var actual = ChatGPT.extractUrls("Contains no image URLs.");
		var expected = List.of();
		assertEquals(expected, actual);

		actual = ChatGPT.extractUrls("Contains one image URL [http://www.example.com/image.png](http://www.example.com/image.png).");
		expected = List.of("http://www.example.com/image.png");
		assertEquals(expected, actual);

		actual = ChatGPT.extractUrls("http://www.example.com/image.png Beginning of string.");
		expected = List.of("http://www.example.com/image.png");
		assertEquals(expected, actual);

		actual = ChatGPT.extractUrls("Middle of http://www.example.com/image.png string.");
		expected = List.of("http://www.example.com/image.png");
		assertEquals(expected, actual);

		actual = ChatGPT.extractUrls("End of string http://www.example.com/image.png");
		expected = List.of("http://www.example.com/image.png");
		assertEquals(expected, actual);

		actual = ChatGPT.extractUrls("Non-image URL http://www.example.com/page.html.");
		expected = List.of("http://www.example.com/page.html");
		assertEquals(expected, actual);

		actual = ChatGPT.extractUrls("Two http://www.example.com/image.jpg URLs http://www.example.com/image.gif.");
		expected = List.of("http://www.example.com/image.jpg", "http://www.example.com/image.gif");
		assertEquals(expected, actual);
	}
}
