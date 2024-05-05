package oakbot.command.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class JavadocCommandArgumentsTest {
	@Test
	public void parse() {
		var args = JavadocCommandArguments.parse("java.lang.string");
		assertEquals("java.lang.string", args.className());
		assertNull(args.methodName());
		assertEquals(1, args.paragraph());
		assertNull(args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string#indexOf");
		assertEquals("java.lang.string", args.className());
		assertEquals("indexOf", args.methodName());
		assertEquals(1, args.paragraph());
		assertNull(args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string#indexOf(*)");
		assertEquals("java.lang.string", args.className());
		assertEquals("indexOf", args.methodName());
		assertEquals(1, args.paragraph());
		assertNull(args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string#indexOf()");
		assertEquals("java.lang.string", args.className());
		assertEquals("indexOf", args.methodName());
		assertEquals(1, args.paragraph());
		assertEquals(List.of(), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string#indexOf(int)");
		assertEquals("java.lang.string", args.className());
		assertEquals("indexOf", args.methodName());
		assertEquals(1, args.paragraph());
		assertEquals(List.of("int"), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string#indexOf(int[])");
		assertEquals("java.lang.string", args.className());
		assertEquals("indexOf", args.methodName());
		assertEquals(1, args.paragraph());
		assertEquals(List.of("int[]"), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string#indexOf(int...)");
		assertEquals("java.lang.string", args.className());
		assertEquals("indexOf", args.methodName());
		assertEquals(1, args.paragraph());
		assertEquals(List.of("int..."), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string#indexOf(int, int)");
		assertEquals("java.lang.string", args.className());
		assertEquals("indexOf", args.methodName());
		assertEquals(1, args.paragraph());
		assertEquals(List.of("int", "int"), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string#indexOf(int, int) 2");
		assertEquals("java.lang.string", args.className());
		assertEquals("indexOf", args.methodName());
		assertEquals(2, args.paragraph());
		assertEquals(List.of("int", "int"), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string#indexOf(int,int)");
		assertEquals("java.lang.string", args.className());
		assertEquals("indexOf", args.methodName());
		assertEquals(1, args.paragraph());
		assertEquals(List.of("int", "int"), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("string()");
		assertEquals("string", args.className());
		assertEquals("string", args.methodName());
		assertEquals(1, args.paragraph());
		assertEquals(List.of(), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("string(string)");
		assertEquals("string", args.className());
		assertEquals("string", args.methodName());
		assertEquals(1, args.paragraph());
		assertEquals(List.of("string"), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string()");
		assertEquals("java.lang.string", args.className());
		assertEquals("string", args.methodName());
		assertEquals(1, args.paragraph());
		assertEquals(List.of(), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string(string)");
		assertEquals("java.lang.string", args.className());
		assertEquals("string", args.methodName());
		assertEquals(1, args.paragraph());
		assertEquals(List.of("string"), args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string 2");
		assertEquals("java.lang.string", args.className());
		assertNull(args.methodName());
		assertEquals(2, args.paragraph());
		assertNull(args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string @Michael");
		assertEquals("java.lang.string", args.className());
		assertNull(args.methodName());
		assertEquals(1, args.paragraph());
		assertNull(args.parameters());
		assertEquals("Michael", args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string Michael");
		assertEquals("java.lang.string", args.className());
		assertNull(args.methodName());
		assertEquals(1, args.paragraph());
		assertNull(args.parameters());
		assertEquals("Michael", args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string 2 @Michael");
		assertEquals("java.lang.string", args.className());
		assertNull(args.methodName());
		assertEquals(2, args.paragraph());
		assertNull(args.parameters());
		assertEquals("Michael", args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string 2 Michael");
		assertEquals("java.lang.string", args.className());
		assertNull(args.methodName());
		assertEquals(2, args.paragraph());
		assertNull(args.parameters());
		assertEquals("Michael", args.targetUser());
	}

	@Test
	public void parse_invalid_paragraph() {
		var args = JavadocCommandArguments.parse("java.lang.string -1");
		assertEquals("java.lang.string", args.className());
		assertNull(args.methodName());
		assertEquals(1, args.paragraph());
		assertNull(args.parameters());
		assertNull(args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string -1 Michael");
		assertEquals("java.lang.string", args.className());
		assertNull(args.methodName());
		assertEquals(1, args.paragraph());
		assertNull(args.parameters());
		assertEquals("Michael", args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string foo Michael");
		assertEquals("java.lang.string", args.className());
		assertNull(args.methodName());
		assertEquals(1, args.paragraph());
		assertNull(args.parameters());
		assertEquals("Michael", args.targetUser());

		args = JavadocCommandArguments.parse("java.lang.string 1.2");
		assertEquals("java.lang.string", args.className());
		assertNull(args.methodName());
		assertEquals(1, args.paragraph());
		assertNull(args.parameters());
		assertEquals("1.2", args.targetUser());
	}
}
