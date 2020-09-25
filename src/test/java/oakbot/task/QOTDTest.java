package oakbot.task;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import oakbot.util.Gobble;

public class QOTDTest {
	/**
	 * Live test. Outputs current QOTD to stdout.
	 */
	public static void main(String args[]) throws Exception {
		QOTD qotd = new QOTD();
		System.out.println(qotd.fromSlashdot());
	}

	@Test
	public void fromSlashdot() throws Exception {
		QOTD qotd = new QOTD() {
			@Override
			String httpGet(String url) throws IOException {
				try (InputStream in = FOTDTest.class.getResourceAsStream("slashdot.html")) {
					return new Gobble(in).asString();
				}
			}
		};

		String expected = "\"For a male and female to live continuously together is... biologically speaking, an extremely unnatural condition.\" -- Robert Briffault ([source](https://slashdot.org))";
		String actual = qotd.fromSlashdot().toString();
		assertEquals(expected, actual);
	}

	@Test
	public void fromTheySaidSo() throws Exception {
		QOTD qotd = new QOTD() {
			@Override
			String httpGet(String url) throws IOException {
				try (InputStream in = FOTDTest.class.getResourceAsStream("theysaidso.json")) {
					return new Gobble(in).asString();
				}
			}
		};

		String expected = "*\"If you like what you do, and you’re lucky enough to be good at it, do it for that reason.\"* -Phil Grimshaw [(source)](https://theysaidso.com)";
		String actual = qotd.fromTheySaidSo().toString();
		assertEquals(expected, actual);
	}

	@Test
	public void fromTheySaidSo_newline() throws Exception {
		QOTD qotd = new QOTD() {
			@Override
			String httpGet(String url) throws IOException {
				try (InputStream in = FOTDTest.class.getResourceAsStream("theysaidso_newline.json")) {
					return new Gobble(in).asString();
				}
			}
		};

		String expected = "If you like what you do,\nand you’re lucky enough to be good at it, do it for that reason.\n-Phil Grimshaw (source: https://theysaidso.com)";
		String actual = qotd.fromTheySaidSo().toString();
		assertEquals(expected, actual);
	}
}
