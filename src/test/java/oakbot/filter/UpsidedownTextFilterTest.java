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
	}
}
