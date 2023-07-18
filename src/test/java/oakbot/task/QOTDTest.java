package oakbot.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.After;
import org.junit.Test;

import oakbot.chat.MockHttpClientBuilder;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.Now;

public class QOTDTest {
	/**
	 * Live test. Outputs current QOTD to stdout.
	 */
	public static void main(String args[]) throws Exception {
		QOTD qotd = new QOTD();
		System.out.println(qotd.fromSlashdot());
	}

	@After
	public void after() {
		Now.restore();
		HttpFactory.restore();
	}

	@Test
	public void fromSlashdot() throws Exception {
		String slashdot = new Gobble(getClass(), "slashdot.html").asString(StandardCharsets.UTF_8);

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://slashdot.org")
			.responseOk(slashdot)
		.build());
		//@formatter:on

		QOTD qotd = new QOTD();

		String expected = "\"For a male and female to live continuously together is... biologically speaking, an extremely unnatural condition.\" -- Robert Briffault ([source](https://slashdot.org))";
		String actual = qotd.fromSlashdot().toString();
		assertEquals(expected, actual);
	}

	@Test
	public void fromTheySaidSo() throws Exception {
		String theySaidSo = new Gobble(getClass(), "theysaidso.json").asString(StandardCharsets.UTF_8);

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://quotes.rest/qod.json")
			.responseOk(theySaidSo)
		.build());
		//@formatter:on

		QOTD qotd = new QOTD();

		String expected = "*\"If you like what you do, and you’re lucky enough to be good at it, do it for that reason.\"* -Phil Grimshaw [(source)](https://theysaidso.com)";
		String actual = qotd.fromTheySaidSo().toString();
		assertEquals(expected, actual);
	}

	@Test
	public void fromTheySaidSo_newline() throws Exception {
		String theySaidSo = new Gobble(getClass(), "theysaidso_newline.json").asString(StandardCharsets.UTF_8);

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://quotes.rest/qod.json")
			.responseOk(theySaidSo)
		.build());
		//@formatter:on

		QOTD qotd = new QOTD();

		String expected = "If you like what you do,\nand you’re lucky enough to be good at it, do it for that reason.\n-Phil Grimshaw (source: https://theysaidso.com)";
		String actual = qotd.fromTheySaidSo().toString();
		assertEquals(expected, actual);
	}

	@Test
	public void nextRun() {
		Now.setNow(LocalDateTime.of(2018, 7, 19, 11, 0, 0));

		QOTD task = new QOTD();

		long expected = Duration.ofHours(13).toMillis();
		long actual = task.nextRun();
		assertApprox(expected, actual);
	}

	private static void assertApprox(long expected, long actual) {
		assertTrue("Expected " + expected + " but was " + actual + ".", expected - actual < 1000);
	}
}
