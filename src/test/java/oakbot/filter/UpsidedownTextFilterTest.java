package oakbot.filter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class UpsidedownTextFilterTest {
	@Test
	public void filter() {
		UpsidedownTextFilter filter = new UpsidedownTextFilter();
		assertEquals("Hǝllo' ʍoɹlp¡", filter.filter("Hello, world!"));
		assertEquals("Hǝllo' [ʍoɹlp¡](http://google.com)", filter.filter("Hello, [world!](http://google.com)"));
		assertEquals(":1 Hǝllo' ʍoɹlp¡", filter.filter(":1 Hello, world!"));
	}
}
