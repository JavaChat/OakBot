package oakbot.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import oakbot.javadoc.ClassInfo;
import oakbot.javadoc.JsoupPageParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class JsoupPageParserTest {
	@Test
	public void getAllClasses() throws Exception {
		Document document;
		try (InputStream in = getClass().getResourceAsStream("jsoup-allclasses-frame.html")) {
			document = Jsoup.parse(in, "UTF-8", "");
		}

		JsoupPageParser parser = new JsoupPageParser();
		List<String> actual = parser.parseClassNames(document);
		//@formatter:off
		List<String> expected = Arrays.asList(
			"org.jsoup.nodes.Attribute",
			"org.jsoup.nodes.Attributes",
			"org.jsoup.nodes.Comment",
			"org.jsoup.Connection",
			"org.jsoup.Connection.Base"
		);
		//@formatter:on
		assertEquals(expected, actual);
	}

	@Test
	public void getClassInfo() throws Exception {
		Document document;
		try (InputStream in = getClass().getResourceAsStream("Attribute.html")) {
			document = Jsoup.parse(in, "UTF-8", "");
		}

		JsoupPageParser parser = new JsoupPageParser();
		ClassInfo info = parser.parseClassPage(document, "org.jsoup.nodes.Attribute");
		assertEquals("org.jsoup.nodes.Attribute", info.getFullName());
		assertEquals(Arrays.asList("public", "class"), info.getModifiers());
		assertFalse(info.isDeprecated());
		assertEquals("http://jsoup.org/apidocs/?org/jsoup/nodes/Attribute.html", info.getUrl());
		assertEquals("A single key + value attribute. Keys are trimmed and normalised to lower-case.", info.getDescription());
	}
}
