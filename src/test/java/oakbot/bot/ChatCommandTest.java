package oakbot.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.command.Command;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
class ChatCommandTest {
	@Test
	void fromMessage() {
		//typical usage
		assertFromMessage("/name value", "name", "value", "/");
		assertFromMessage("/name one two", "name", "one two", "/");
		assertFromMessage("/name", "name", "", "/");

		//ignore whitespace at the beginning of the message
		assertFromMessage("  /name value", "name", "value", "/");

		//no command name after the trigger
		assertFromMessageNull("/ value", "/");

		//chat message does not start with the trigger
		assertFromMessageNull("name /value", "/");

		//remove formatting around command name
		assertFromMessage("<b>/name</b> <i>value</i>", "name", "<i>value</i>", "/");

		//remove formatted around command name, but preserve the formatting tags that wrap both the command name and value
		assertFromMessage("<b><i>/name</i> value</b>", "name", "<b>value</b>", "/");

		//do not treat whitespace inside of a tag as the delimiter between the command name and value
		assertFromMessage("<a href=\"http://google.com\" target=\"_blank\">/name <i>value</i></a>", "name", "<a href=\"http://google.com\" target=\"_blank\"><i>value</i></a>", "/");

		//<i> tag effectively ends when the </b> tag is reached
		assertFromMessage("<b><i>/name</b> value", "name", "value", "/");

		//decode HTML entities in command name, but not command value
		assertFromMessage("/&lt;name &lt;value", "<name", "&lt;value", "/");

		//don't think all forward-slashes part of an "end" HTML tag
		assertFromMessage("/name one/two", "name", "one/two", "/");

		//null message content (user deleted the message)
		assertFromMessageNull(null, "/");

		//multi-line message, treat newlines as whitespace delimiter between command name and value
		assertFromMessage("/name\n\none\ntwo", "name", "one\ntwo", "/", false);

		//use the first word as the command when no trigger is specified
		assertFromMessage("name value", "name", "value", null);

		//fixed font messages should not be trimmed
		assertFromMessage("/name\n one\n  two", "name", "one\n  two", "/", false);
		assertFromMessage("/name\n one\n  two", "name", " one\n  two", "/", true);
	}

	private static void assertFromMessageNull(String content, String trigger) {
		for (var fixedFont : new boolean[] { false, true }) {
			var message = new ChatMessage.Builder().content(content, fixedFont).build();
			var command = ChatCommand.fromMessage(message, trigger);
			assertNull(command);
		}
	}

	private static void assertFromMessage(String content, String name, String text, String trigger) {
		for (var fixedFont : new boolean[] { false, true }) {
			assertFromMessage(content, name, text, trigger, fixedFont);
		}
	}

	private static void assertFromMessage(String content, String name, String text, String trigger, boolean fixedFont) {
		var message = new ChatMessage.Builder().content(content, fixedFont).build();
		var command = ChatCommand.fromMessage(message, trigger);
		assertEquals(name, command.getCommandName());
		assertEquals(text, command.getContent());
		assertSame(message, command.getMessage());
	}

	@Test
	void getContentAsArgs() {
		assertContentAsArgs("", List.of());
		assertContentAsArgs("one", List.of("one"));
		assertContentAsArgs("one two", List.of("one", "two"));
		
		//trim whitespace
		assertContentAsArgs("  one two  ", List.of("one", "two"));
		
		//multiple whitespace characters
		assertContentAsArgs("one \t two", List.of("one", "two"));
		
		//convert HTML to markdown
		assertContentAsArgs("<b>one</b> two", List.of("**one**", "two"));
		
		//double-quotes
		assertContentAsArgs("one \"two three\"", List.of("one", "two three"));
		
		//double-quotes without terminating quote
		assertContentAsArgs("one \"two three", List.of("one", "two three"));
		
		//escaped double quote
		assertContentAsArgs("one \"two \\\"three\"", List.of("one", "two \"three"));
	}

	private static void assertContentAsArgs(String content, List<String> expected) {
		var command = mock(Command.class);
		when(command.name()).thenReturn("cmd");

		var chatCommand = new ChatCommandBuilder(command).content(content).build();
		var actual = chatCommand.getContentAsArgs();
		assertEquals(expected, actual);
	}
}
