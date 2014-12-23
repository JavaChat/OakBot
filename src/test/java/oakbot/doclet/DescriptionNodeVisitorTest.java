package oakbot.doclet;

import static org.junit.Assert.assertEquals;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class DescriptionNodeVisitorTest {
	@Test
	public void bold() {
		String html = "Bold test: <b>bold1</b>, <strong>bold2</strong>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Bold test: **bold1**, **bold2**";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void italic() {
		String html = "Italic test: <i>italic1</i>, <em>italic2</em>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Italic test: *italic1*, *italic2*";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void strike() {
		String html = "Strike test: <strike>strike1</strike>, <s>strike2</s>, <del>strike3</del>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Strike test: ---strike1---, ---strike2---, ---strike3---";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void code() {
		String html = "Code test: <code>code1</code>, <tt>code2</tt>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Code test: `code1`, `code2`";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void link() {
		String html = "Link test: <a href=\"http://www.example.com\">link1</a>, <a href=\"http://www.example.com\" title=\"the title\">link2</a>, <a name=\"name\">link3</a>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Link test: [link1](http://www.example.com), [link2](http://www.example.com \"the title\"), link3";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void br() {
		String html = "Br test: line1<br />line2";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Br test: line1\n\nline2";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void p() {
		String html = "P test: <p>paragraph1</p><p>paragraph2<p>paragraph3";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "P test:\n\nparagraph1\n\nparagraph2\n\nparagraph3";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void nbsp() {
		String html = "Nbsp test: one&nbsp;two";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Nbsp test: one two";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void pre() {
		String html = "Pre test: <pre>line1\nline2\n  line3</pre>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Pre test:\n\n    line1\n    line2\n      line3";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void headers() {
		String html = "Header test: <h1>header1</h1>text<h2>header2</h2>text<h3>header3</h3>text<h4>header4</h4>text<h5>header5</h5>text<h6>header6</h6>text";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Header test:\n\n**header1:** text\n\n**header2:** text\n\n**header3:** text\n\n**header4:** text\n\n**header5:** text\n\n**header6:** text";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}
	
	@Test
	public void escape() {
		String html = "Escape test: one*two[three]`four_five";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Escape test: one\\*two\\[three\\]\\`four\\_five";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}
	
	@Test
	public void special_chars_in_code_tag() {
		String html = "<code>one*two</code>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "`one*two`";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void multiple_empty_paragraphs() {
		String html = "test1<p><p><p>test2";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "test1\n\ntest2";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void duplicate_formatting_tags() {
		String html = "<b><b>double bold</b></b>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "****double bold****"; //TODO duplicates should be ignored, should be "**double bold**"
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void pre_should_ignore_formatting_tags() {
		String html = "Pre test: <pre><b>line1</b>\n<i>line2</i>\n  <code>line3</code></pre>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "Pre test:\n\n    line1\n    line2\n      line3";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void code_is_the_outter_formatting_tag() {
		String html = "<code><b>test</b></code>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "**`test`**";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void code_formatting_with_links() {
		{
			String html = "<code><a href=\"http://www.example.com\">link</a></code>";
			Document document = Jsoup.parse(html);
			DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
			document.traverse(visitor);

			String expected = "[`link`](http://www.example.com)";
			String actual = visitor.getDescription();
			assertEquals(expected, actual);
		}

		{
			String html = "<a href=\"http://www.example.com\"><code>link</code></a>";
			Document document = Jsoup.parse(html);
			DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
			document.traverse(visitor);

			String expected = "[`link`](http://www.example.com)";
			String actual = visitor.getDescription();
			assertEquals(expected, actual);
		}
	}

	@Test
	public void link_with_lots_of_formatting() {
		String html = "<code><i><a href=\"http://www.example.com\"><b>link</b></a></i></code>";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "*[**`link`**](http://www.example.com)*";
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}

	@Test
	public void whitespace_at_start_and_end_of_tags() {
		String html = "text<b> bold </b>text";
		Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);

		String expected = "text** bold **text"; //TODO should be "text **bold** text"
		String actual = visitor.getDescription();
		assertEquals(expected, actual);
	}
}
