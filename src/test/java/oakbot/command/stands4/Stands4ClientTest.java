package oakbot.command.stands4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;

/**
 * @author Michael Angstadt
 */
public class Stands4ClientTest {
	@Test
	public void getAbbreviations() throws Exception {
		var client = new Stands4Client("USERID", "TOKEN");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.stands4.com/services/v2/abbr.php?uid=USERID&tokenid=TOKEN&format=json&term=ASAP")
			.responseOk(ResponseSamples.abbr())
		.build());
		//@formatter:on

		var expected = List.of("As Soon As Possible", "Alliance of Security Analysis Professionals");
		var actual = client.getAbbreviations("ASAP", 2);
		assertEquals(expected, actual);
	}

	@Test
	public void getAbbreviations_no_results() throws Exception {
		var client = new Stands4Client("USERID", "TOKEN");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.stands4.com/services/v2/abbr.php?uid=USERID&tokenid=TOKEN&format=json&term=ASAP")
			.responseOk(ResponseSamples.abbrNoResults())
		.build());
		//@formatter:on

		var expected = List.of();
		var actual = client.getAbbreviations("ASAP", 2);
		assertEquals(expected, actual);
	}

	@Test
	public void getAbbreviationsAttributionUrl() {
		var client = new Stands4Client("USERID", "TOKEN");

		var expected = "https://www.abbreviations.com/ASAP";
		var actual = client.getAbbreviationsAttributionUrl("ASAP");
		assertEquals(expected, actual);
	}

	@Test
	public void checkGrammar() throws Exception {
		var client = new Stands4Client("USERID", "TOKEN");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.stands4.com/services/v2/grammar.php?uid=USERID&tokenid=TOKEN&format=json&text=i+travels+to+new+york+city")
			.responseOk(ResponseSamples.grammar())
		.build());
		//@formatter:on

		var expected = List.of("The personal pronoun “I” should be uppercase.", "If the term is a proper noun, use initial capitals.");
		var actual = client.checkGrammar("i travels to new york city");
		assertEquals(expected, actual);
	}

	@Test
	public void checkGrammar_no_results() throws Exception {
		var client = new Stands4Client("USERID", "TOKEN");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.stands4.com/services/v2/grammar.php?uid=USERID&tokenid=TOKEN&format=json&text=I+love+dogs.")
			.responseOk(ResponseSamples.grammarNoWarnings())
		.build());
		//@formatter:on

		var expected = List.of();
		var actual = client.checkGrammar("I love dogs.");
		assertEquals(expected, actual);
	}

	@Test
	public void getGrammarAttributionUrl() {
		var client = new Stands4Client("USERID", "TOKEN");

		var expected = "https://www.grammar.com";
		var actual = client.getGrammarAttributionUrl();
		assertEquals(expected, actual);
	}

	@Test
	public void convert() throws Exception {
		var client = new Stands4Client("USERID", "TOKEN");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.stands4.com/services/v2/conv.php?uid=USERID&tokenid=TOKEN&format=json&expression=2+miles+in+centimeters")
			.responseOk(ResponseSamples.conv())
		.build());
		//@formatter:on

		var expected = "2 miles = 321,869 centimeters";
		var actual = client.convert("2 miles in centimeters");
		assertEquals(expected, actual);
	}

	@Test
	public void convert_error() throws Exception {
		var client = new Stands4Client("USERID", "TOKEN");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.stands4.com/services/v2/conv.php?uid=USERID&tokenid=TOKEN&format=json&expression=i+love+dogs")
			.responseOk(ResponseSamples.convError())
		.build());
		//@formatter:on

		try {
			client.convert("i love dogs");
			fail();
		} catch (ConvertException e) {
			assertEquals(4, e.getCode());
			assertEquals("4: Invalid expression", e.getMessage());
		}
	}

	@Test
	public void getConvertAttributionUrl() {
		var client = new Stands4Client("USERID", "TOKEN");

		var expected = "https://www.convert.net";
		var actual = client.getConvertAttributionUrl();
		assertEquals(expected, actual);
	}

	@Test
	public void explain() throws Exception {
		var client = new Stands4Client("USERID", "TOKEN");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.stands4.com/services/v2/phrases.php?uid=USERID&tokenid=TOKEN&format=json&phrase=in+the+nick+of+time")
			.responseOk(ResponseSamples.phrases())
		.build());
		//@formatter:on

		var actual = client.explain("in the nick of time");
		assertEquals("At the last possible moment; at the last minute.", actual.getExplanation());
		assertEquals("He finished writing his paper and slid it under the door just in the nick of time.", actual.getExample());
	}

	@Test
	public void explain_no_results() throws Exception {
		var client = new Stands4Client("USERID", "TOKEN");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.stands4.com/services/v2/phrases.php?uid=USERID&tokenid=TOKEN&format=json&phrase=asdf")
			.responseOk(ResponseSamples.phrasesNoResults())
		.build());
		//@formatter:on

		var actual = client.explain("asdf");
		assertNull(actual);
	}

	@Test
	public void getExplainAttributionUrl() {
		var client = new Stands4Client("USERID", "TOKEN");

		var expected = "https://www.phrases.com/psearch/in%20the%20nick%20of%20time";
		var actual = client.getExplainAttributionUrl("in the nick of time");
		assertEquals(expected, actual);
	}

	@Test
	public void getRhymes() throws Exception {
		var client = new Stands4Client("USERID", "TOKEN");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.stands4.com/services/v2/rhymes.php?uid=USERID&tokenid=TOKEN&format=json&term=java")
			.responseOk(ResponseSamples.rhymes())
		.build());
		//@formatter:on

		var expected = List.of("actava", "bava", "brattaslava", "cava", "chava", "fava", "guava", "gustava", "java", "lacava", "lava", "nava", "penkava", "rubalcava", "sava", "scozzafava", "srivastava", "votava");
		var actual = client.getRhymes("java");
		assertEquals(expected, actual);
	}

	@Test
	public void getRhymes_no_results() throws Exception {
		var client = new Stands4Client("USERID", "TOKEN");

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.stands4.com/services/v2/rhymes.php?uid=USERID&tokenid=TOKEN&format=json&term=asdf")
			.responseOk(ResponseSamples.rhymesNoResults())
		.build());
		//@formatter:on

		var expected = List.of();
		var actual = client.getRhymes("asdf");
		assertEquals(expected, actual);
	}

	@Test
	public void getRhymesAttributionUrl() {
		var client = new Stands4Client("USERID", "TOKEN");

		var expected = "https://www.rhymes.com/rhyme/java";
		var actual = client.getRhymesAttributionUrl("java");
		assertEquals(expected, actual);
	}
}
