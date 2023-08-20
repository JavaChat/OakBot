package oakbot.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.command.Command;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class ChatCommandTest {
	@Test
	public void fromMessage() {
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
		for (boolean fixedFont : new boolean[] { false, true }) {
			ChatMessage message = new ChatMessage.Builder().content(content, fixedFont).build();
			ChatCommand command = ChatCommand.fromMessage(message, trigger);
			assertNull(command);
		}
	}

	private static void assertFromMessage(String content, String name, String text, String trigger) {
		for (boolean fixedFont : new boolean[] { false, true }) {
			assertFromMessage(content, name, text, trigger, fixedFont);
		}
	}

	private static void assertFromMessage(String content, String name, String text, String trigger, boolean fixedFont) {
		ChatMessage message = new ChatMessage.Builder().content(content, fixedFont).build();
		ChatCommand command = ChatCommand.fromMessage(message, trigger);
		assertEquals(name, command.getCommandName());
		assertEquals(text, command.getContent());
		assertSame(message, command.getMessage());
	}

	@Test
	public void getContentAsArgs() {
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
		Command command = mock(Command.class);
		when(command.name()).thenReturn("cmd");

		ChatCommand chatCommand = new ChatCommandBuilder(command).content(content).build();
		List<String> actual = chatCommand.getContentAsArgs();
		assertEquals(expected, actual);
	}
}
