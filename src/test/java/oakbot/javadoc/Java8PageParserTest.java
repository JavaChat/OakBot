package oakbot.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
			document = Jsoup.parse(in, "UTF-8", "https://docs.oracle.com/javase/8/docs/api/java/lang/");
		}

		Java8PageParser parser = new Java8PageParser();
		ClassInfo actual = parser.parseClassPage(document, "java.lang.String");

		assertEquals("java.lang.String", actual.getFullName());
		assertEquals("https://docs.oracle.com/javase/8/docs/api/?java/lang/String.html", actual.getUrl());
		assertEquals(Arrays.asList("public", "final", "class"), actual.getModifiers());
		assertFalse(actual.isDeprecated());

		//@formatter:off
		assertEquals(
		"The `String` class represents character strings.\n" +
		"\n" +
		" `code` text\n" +
		"\n" +
		" `Don't escape[]`\n" +
		"\n" +
		"**bold** text\n" +
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
		" [relative link](https://docs.oracle.com/javase/8/docs/api/java/dir/file.html)\n" +
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
		"ignore me", actual.getDescription());
		//@formatter:on

		Iterator<MethodInfo> methods = actual.getMethods().iterator();
		
		//CONSTRUCTORS=================================================

		//@formatter:off
		MethodInfo methodInfo = methods.next();
		assertEquals("String", methodInfo.getName());
		assertEquals(Arrays.asList("public"), methodInfo.getModifiers());
		assertEquals(Arrays.asList(), methodInfo.getParameters());
		assertEquals("String()", methodInfo.getSignatureString());
		assertEquals(
			"Initializes a newly created `String` object so that it represents an empty character sequence. Note that use of this constructor is unnecessary since Strings are immutable.",
			methodInfo.getDescription()
		);
		assertFalse(methodInfo.isDeprecated());
		
		methodInfo = methods.next();
		assertEquals("String", methodInfo.getName());
		assertEquals(Arrays.asList("public"), methodInfo.getModifiers());
		assertEquals(
			Arrays.asList(
				new MethodParameter("String", "original")
			),
			methodInfo.getParameters());
		assertEquals("String(String original)", methodInfo.getSignatureString());
		assertEquals(
			"Initializes a newly created `String` object so that it represents the same sequence of characters as the argument; in other words, the newly created string is a copy of the argument string. Unless an explicit copy of `original` is needed, use of this constructor is unnecessary since Strings are immutable.",
			methodInfo.getDescription()
		);
		assertFalse(methodInfo.isDeprecated());
		
		methodInfo = methods.next();
		assertEquals("String", methodInfo.getName());
		assertEquals(Arrays.asList("public"), methodInfo.getModifiers());
		assertEquals(
			Arrays.asList(
				new MethodParameter("char[]", "value")
			),
			methodInfo.getParameters()
		);
		assertEquals("String(char[] value)", methodInfo.getSignatureString());
		assertEquals(
			"Allocates a new `String` so that it represents the sequence of characters currently contained in the character array argument. The contents of the character array are copied; subsequent modification of the character array does not affect the newly created string.",
			methodInfo.getDescription()
		);
		assertFalse(methodInfo.isDeprecated());
		
		methodInfo = methods.next();
		assertEquals("String", methodInfo.getName());
		assertEquals(Arrays.asList("public"), methodInfo.getModifiers());
		assertEquals(
			Arrays.asList(
				new MethodParameter("byte[]", "ascii"),
				new MethodParameter("int", "hibyte"),
				new MethodParameter("int", "offset"),
				new MethodParameter("int", "count")
			),
			methodInfo.getParameters()
		);
		assertEquals("String(byte[] ascii, int hibyte, int offset, int count)", methodInfo.getSignatureString());
		assertEquals(
			"Deprecated. This method does not properly convert bytes into characters. As of JDK 1.1, the preferred way to do this is via the `String` constructors that take a [`Charset`](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html \"class in java.nio.charset\"), charset name, or that use the platform's default charset.\n" +
			"\n" +
			"Allocates a new `String` constructed from a subarray of an array of 8-bit integer values.\n" +
			"\n" +
			" The `offset` argument is the index of the first byte of the subarray, and the `count` argument specifies the length of the subarray.\n" +
			"\n" +
			" Each `byte` in the subarray is converted to a `char` as specified in the method above.",
			methodInfo.getDescription()
		);
		assertTrue(methodInfo.isDeprecated());
		
		methodInfo = methods.next();
		assertEquals("String", methodInfo.getName());
		assertEquals(Arrays.asList("public"), methodInfo.getModifiers());
		assertEquals(
			Arrays.asList(
				new MethodParameter("byte[]", "bytes"),
				new MethodParameter("int", "offset"),
				new MethodParameter("int", "length"),
				new MethodParameter("String", "charsetName")
			),
			methodInfo.getParameters()
		);
		assertEquals("String(byte[] bytes, int offset, int length, String charsetName)", methodInfo.getSignatureString());
		assertEquals(
			"Constructs a new `String` by decoding the specified subarray of bytes using the specified charset. The length of the new `String` is a function of the charset, and hence may not be equal to the length of the subarray.\n" +
			"\n" +
			" The behavior of this constructor when the given bytes are not valid in the given charset is unspecified. The [`CharsetDecoder`](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/CharsetDecoder.html \"class in java.nio.charset\") class should be used when more control over the decoding process is required.",
			methodInfo.getDescription()
		);
		assertFalse(methodInfo.isDeprecated());
	
		//METHODS=================================================
		
		methodInfo = methods.next();
		assertEquals("length", methodInfo.getName());
		assertEquals(Arrays.asList("public"), methodInfo.getModifiers());
		assertEquals(Arrays.asList(), methodInfo.getParameters());
		assertEquals("int length()", methodInfo.getSignatureString());
		assertEquals(
			"Returns the length of this string. The length is equal to the number of [Unicode code units](https://docs.oracle.com/javase/8/docs/api/java/lang/Character.html#unicode) in the string.",
			methodInfo.getDescription()
		);
		assertFalse(methodInfo.isDeprecated());
		
		methodInfo = methods.next();
		assertEquals("getBytes", methodInfo.getName());
		assertEquals(Arrays.asList("public"), methodInfo.getModifiers());
		assertEquals(
			Arrays.asList(
				new MethodParameter("int", "srcBegin"),
				new MethodParameter("int", "srcEnd"),
				new MethodParameter("byte[]", "dst"),
				new MethodParameter("int", "dstBegin")
			),
			methodInfo.getParameters()
		);
		assertEquals("void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin)", methodInfo.getSignatureString());
		assertEquals(
			"Deprecated. This method does not properly convert characters into bytes. As of JDK 1.1, the preferred way to do this is via the [`getBytes()`](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#getBytes--) method, which uses the platform's default charset.\n" +
			"\n" +
			"Copies characters from this string into the destination byte array. Each byte receives the 8 low-order bits of the corresponding character. The eight high-order bits of each character are not copied and do not participate in the transfer in any way.\n" +
			"\n" +
			" The first character to be copied is at index `srcBegin`; the last character to be copied is at index `srcEnd-1`. The total number of characters to be copied is `srcEnd-srcBegin`. The characters, converted to bytes, are copied into the subarray of `dst` starting at index `dstBegin` and ending at index:\n" +
			"\n" +
			"`dstbegin + (srcEnd-srcBegin) - 1`",
			methodInfo.getDescription()
		);
		assertTrue(methodInfo.isDeprecated());
		
		methodInfo = methods.next();
		assertEquals("getBytes", methodInfo.getName());
		assertEquals(Arrays.asList("public"), methodInfo.getModifiers());
		assertEquals(
			Arrays.asList(
				new MethodParameter("String", "charsetName")
			),
			methodInfo.getParameters()
		);
		assertEquals("byte[] getBytes(String charsetName)", methodInfo.getSignatureString());
		assertEquals(
			"Encodes this `String` into a sequence of bytes using the named charset, storing the result into a new byte array.\n" +
			"\n" +
			" The behavior of this method when this string cannot be encoded in the given charset is unspecified. The [`CharsetEncoder`](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/CharsetEncoder.html \"class in java.nio.charset\") class should be used when more control over the encoding process is required.",
			methodInfo.getDescription()
		);
		assertFalse(methodInfo.isDeprecated());
		
		methodInfo = methods.next();
		assertEquals("join", methodInfo.getName());
		assertEquals(Arrays.asList("public", "static"), methodInfo.getModifiers());
		assertEquals(
			Arrays.asList(
				new MethodParameter("CharSequence", "delimiter"),
				new MethodParameter("CharSequence...", "elements")
			),
			methodInfo.getParameters()
		);
		assertEquals("String join(CharSequence delimiter, CharSequence... elements)", methodInfo.getSignatureString());
		assertEquals(
			"Returns a new String composed of copies of the `CharSequence elements` joined together with a copy of the specified `delimiter`. For example,\n" +
			"\n" +
			"    String message = String.join(\"-\", \"Java\", \"is\", \"cool\");\n" +
			"    // message returned is: \"Java-is-cool\"\n" +
			"\n" +
			" Note that if an element is null, then `\"null\"` is added.",
			methodInfo.getDescription()
		);
		assertFalse(methodInfo.isDeprecated());

		assertFalse(methods.hasNext());
		
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
		assertEquals("Deprecated.  This class does not properly convert characters into bytes. As of JDK 1.1, the preferred way to create a stream from a string is via the `StringReader` class.", info.getDescription());
	}
}
