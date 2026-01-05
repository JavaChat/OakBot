package oakbot.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Angstadt
 */
class UpsidedownTextFilterTest {
	@Test
	void filter() {
		var filter = new UpsidedownTextFilter();
		assertEquals("Hǝllo' ʍoɹlp¡", filter.filter(MessageParts.parse("Hello, world!")));
		assertEquals("Hǝllo' [ʍoɹlp¡](http://google.com)", filter.filter(MessageParts.parse("Hello, [world!](http://google.com)")));
		assertEquals(":1 Hǝllo' ʍoɹlp¡", filter.filter(MessageParts.parse(":1 Hello, world!")));
	}
}
