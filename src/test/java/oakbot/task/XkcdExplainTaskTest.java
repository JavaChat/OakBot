package oakbot.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import oakbot.util.Gobble;

public class XkcdExplainTaskTest {
	/**
	 * Live test. Outputs the message to stdout.
	 */
	public static void main(String args[]) throws Exception {
		XkcdExplainTask task = new XkcdExplainTask("PT1S");
		String message = task.getExplainationMessage(2796, 1234);
		System.out.println(message);
	}

	@Test
	public void getExplainationMessage() throws Exception {
		XkcdExplainTask task = new XkcdExplainTask("PT1S") {
			@Override
			String get(String url) throws IOException {
				assertEquals("https://www.explainxkcd.com/wiki/index.php/2796", url);
				try (InputStream in = XkcdExplainTaskTest.class.getResourceAsStream("xkcd-explain-2796.html")) {
					return new Gobble(in).asString();
				}
			}
		};

		String expected = ":1234 **[XKCD #2796 Explained](https://www.explainxkcd.com/wiki/index.php/2796):** This *comic* shows a chart ranking locations in our solar system \\(the eight currently recognised planets and Earth's own moon\\) along two scales: their walkability and their proximity to shops. As this is a \"real estate analysis\", this comic mocks real life \"real estate analyses\" for people who are looking for a new home. Walkability measures the ease of walking as a form of transportation in an area \\(often ...";
		String actual = task.getExplainationMessage(2796, 1234);
		assertEquals(expected, actual);
		assertTrue(actual.length() < 500);
	}
}
