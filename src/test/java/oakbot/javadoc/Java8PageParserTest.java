package oakbot.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import oakbot.javadoc.ClassInfo;
import oakbot.javadoc.Java8PageParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class Java8PageParserTest {
	@Test
	public void getAllClasses() throws Exception {
		Document document;
		try (InputStream in = getClass().getResourceAsStream("java8-allclasses-frame.html")) {
			document = Jsoup.parse(in, "UTF-8", "");
		}

		Java8PageParser parser = new Java8PageParser();
		List<String> actual = parser.parseClassNames(document);
		//@formatter:off
		List<String> expected = Arrays.asList(
			"java.awt.List",
			"java.lang.String",
			"java.util.List",
			"java.util.Map.Entry"
		);
		//@formatter:on
		assertEquals(expected, actual);
	}

	@Test
	public void getClassInfo() throws Exception {
		Document document;
		try (InputStream in = getClass().getResourceAsStream("String.html")) {
			document = Jsoup.parse(in, "UTF-8", "http://root.com/folder/");
		}

		Java8PageParser parser = new Java8PageParser();
		ClassInfo info = parser.parseClassPage(document, "java.lang.String");
		assertEquals("java.lang.String", info.getFullName());
		assertEquals("https://docs.oracle.com/javase/8/docs/api/?java/lang/String.html", info.getUrl());

		assertEquals(Arrays.asList("public", "final", "class"), info.getModifiers());
		assertFalse(info.isDeprecated());

		//@formatter:off
		assertEquals(
		"The `String` class represents character strings.\n" +
		"\n" +
		" `code` text\n" +
		"\n" +
		" **bold** text\n" +
		"\n" +
		" **bold** text\n" +
		"\n" +
		" *italic* text\n" +
		"\n" +
		" *italic* text\n" +
		"\n" +
		" \\*asterisks\\*\n" +
		"\n" +
		" \\_underscores\\_\n" +
		"\n" +
		" \\[brackets\\]\n" +
		"\n" +
		" [Google Search](http://www.google.com \"with title\")\n" +
		"\n" +
		" [relative link](http://root.com/dir/file.html)\n" +
		"\n" +
		" Because String objects are immutable they can be shared. For example:\n" +
		"\n" +
		"`String str = \"abc\";`\n" +
		"\n" +
		" is equivalent to:\n" +
		"\n" +
		"    char data[] = {'a', 'b', 'c'};\n" +
		"    String str = new String(data);\n" +
		"    if (foo){\n" +
        "      System.out.println(\"bar\");\n" +
        "    }\n" +
		"\n" +
		"ignore me", info.getDescription());
		//@formatter:on
	}

	@Test
	public void getClassInfo_annotation() throws Exception {
		Document document;
		try (InputStream in = getClass().getResourceAsStream("SuppressWarnings.html")) {
			document = Jsoup.parse(in, "UTF-8", "");
		}

		Java8PageParser parser = new Java8PageParser();
		ClassInfo info = parser.parseClassPage(document, "java.lang.SuppressWarnings");
		assertEquals("java.lang.SuppressWarnings", info.getFullName());
		assertEquals("https://docs.oracle.com/javase/8/docs/api/?java/lang/SuppressWarnings.html", info.getUrl());

		assertEquals(Arrays.asList("public", "@interface"), info.getModifiers());
		assertFalse(info.isDeprecated());
		assertNotNull(info.getDescription());
	}

	@Test
	public void getClassInfo_deprecated() throws Exception {
		Document document;
		try (InputStream in = getClass().getResourceAsStream("StringBufferInputStream.html")) {
			document = Jsoup.parse(in, "UTF-8", "");
		}

		Java8PageParser parser = new Java8PageParser();
		ClassInfo info = parser.parseClassPage(document, "java.io.StringBufferInputStream");
		assertEquals("java.io.StringBufferInputStream", info.getFullName());
		assertEquals("https://docs.oracle.com/javase/8/docs/api/?java/io/StringBufferInputStream.html", info.getUrl());

		assertEquals(Arrays.asList("public", "class"), info.getModifiers());
		assertTrue(info.isDeprecated());
		System.out.println((int)info.getDescription().charAt(12));
		System.out.println((int)info.getDescription().charAt(13));
		assertNotNull(info.getDescription());
	}
}
