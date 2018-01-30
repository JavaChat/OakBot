package oakbot.command.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class JavadocCommandArgumentsTest {
	@Test
	public void parse() {
		JavadocCommandArguments parser = JavadocCommandArguments.parse("java.lang.string");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertNull(parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string#indexOf");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertNull(parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string#indexOf(*)");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertNull(parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string#indexOf()");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string#indexOf(int)");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("int"), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string#indexOf(int[])");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("int[]"), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string#indexOf(int...)");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("int..."), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string#indexOf(int, int)");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("int", "int"), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string#indexOf(int, int) 2");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(2, parser.getParagraph());
		assertEquals(Arrays.asList("int", "int"), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string#indexOf(int,int)");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("indexOf", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("int", "int"), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("string()");
		assertEquals("string", parser.getClassName());
		assertEquals("string", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("string(string)");
		assertEquals("string", parser.getClassName());
		assertEquals("string", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("string"), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string()");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("string", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList(), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string(string)");
		assertEquals("java.lang.string", parser.getClassName());
		assertEquals("string", parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertEquals(Arrays.asList("string"), parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string 2");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(2, parser.getParagraph());
		assertNull(parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string @Michael");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertNull(parser.getParameters());
		assertEquals("Michael", parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string Michael");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertNull(parser.getParameters());
		assertEquals("Michael", parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string 2 @Michael");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(2, parser.getParagraph());
		assertNull(parser.getParameters());
		assertEquals("Michael", parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string 2 Michael");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(2, parser.getParagraph());
		assertNull(parser.getParameters());
		assertEquals("Michael", parser.getTargetUser());
	}

	@Test
	public void parse_invalid_paragraph() {
		JavadocCommandArguments parser = JavadocCommandArguments.parse("java.lang.string -1");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertNull(parser.getParameters());
		assertNull(parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string -1 Michael");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertNull(parser.getParameters());
		assertEquals("Michael", parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string foo Michael");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertNull(parser.getParameters());
		assertEquals("Michael", parser.getTargetUser());

		parser = JavadocCommandArguments.parse("java.lang.string 1.2");
		assertEquals("java.lang.string", parser.getClassName());
		assertNull(parser.getMethodName());
		assertEquals(1, parser.getParagraph());
		assertNull(parser.getParameters());
		assertEquals("1.2", parser.getTargetUser());
	}
}
