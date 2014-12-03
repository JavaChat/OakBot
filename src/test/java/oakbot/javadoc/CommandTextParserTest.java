package oakbot.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import oakbot.javadoc.JavadocCommand.CommandTextParser;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class CommandTextParserTest {
	@Test
	public void onMessage() {
		CommandTextParser parser = new CommandTextParser("java.lang.string");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());

		parser = new CommandTextParser("java.lang.string#indexOf");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());

		parser = new CommandTextParser("java.lang.string#indexOf()");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());

		parser = new CommandTextParser("java.lang.string#indexOf(int)");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("int"), parser.getParameters());

		parser = new CommandTextParser("java.lang.string#indexOf(int, int)");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("int", "int"), parser.getParameters());
		
		parser = new CommandTextParser("java.lang.string#indexOf(int, int) 2");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(2, parser.getParagraph());
		assertEquals(Arrays.asList("int", "int"), parser.getParameters());

		parser = new CommandTextParser("java.lang.string#indexOf(int,int)");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("int", "int"), parser.getParameters());

		parser = new CommandTextParser("string()");
		assertEquals("string", parser.getClassName());
		assertEquals("string", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());

		parser = new CommandTextParser("string(string)");
		assertEquals("string", parser.getClassName());
		assertEquals("string", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("string"), parser.getParameters());

		parser = new CommandTextParser("java.lang.string()");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("string", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());

		parser = new CommandTextParser("java.lang.string(string)");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("string", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("string"), parser.getParameters());
	}

	@Test
	public void onMessage_invalid_paragraph() {
		CommandTextParser parser = new CommandTextParser("java.lang.string foo");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());

		parser = new CommandTextParser("java.lang.string -1");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());
		
		parser = new CommandTextParser("java.lang.string 1.2");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());
	}
}
