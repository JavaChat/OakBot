package oakbot.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;
import oakbot.util.Now;

public class QOTDTest {
	/**
	 * Live test. Outputs current QOTD to stdout.
	 */
	public static void main(String args[]) throws Exception {
		var qotd = new QOTD();
		System.out.println(qotd.fromSlashdot());
	}

	@AfterEach
	public void after() {
		Now.restore();
		HttpFactory.restore();
	}

	@Test
	public void fromSlashdot() throws Exception {
		var slashdot = new Gobble(getClass(), "slashdot.html").asString(StandardCharsets.UTF_8);

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://slashdot.org")
			.responseOk(slashdot)
		.build());
		//@formatter:on

		var qotd = new QOTD();

		var expected = "\"For a male and female to live continuously together is... biologically speaking, an extremely unnatural condition.\" -- Robert Briffault ([source](https://slashdot.org))";
		var actual = qotd.fromSlashdot().toString();
		assertEquals(expected, actual);
	}

	@Test
	public void fromTheySaidSo() throws Exception {
		var theySaidSo = new Gobble(getClass(), "theysaidso.json").asString(StandardCharsets.UTF_8);

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://quotes.rest/qod.json")
			.responseOk(theySaidSo)
		.build());
		//@formatter:on

		var qotd = new QOTD();

		var expected = "*\"If you like what you do, and you’re lucky enough to be good at it, do it for that reason.\"* -Phil Grimshaw [(source)](https://theysaidso.com)";
		var actual = qotd.fromTheySaidSo().toString();
		assertEquals(expected, actual);
	}

	@Test
	public void fromTheySaidSo_newline() throws Exception {
		var theySaidSo = new Gobble(getClass(), "theysaidso_newline.json").asString(StandardCharsets.UTF_8);

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://quotes.rest/qod.json")
			.responseOk(theySaidSo)
		.build());
		//@formatter:on

		var qotd = new QOTD();

		var expected = "If you like what you do,\nand you’re lucky enough to be good at it, do it for that reason.\n-Phil Grimshaw (source: https://theysaidso.com)";
		var actual = qotd.fromTheySaidSo().toString();
		assertEquals(expected, actual);
	}

	@Test
	public void nextRun() {
		Now.setNow(LocalDateTime.of(2018, 7, 19, 11, 0, 0));

		var task = new QOTD();

		var expected = Duration.ofHours(13).toMillis();
		var actual = task.nextRun();
		assertApprox(expected, actual);
	}

	private static void assertApprox(long expected, long actual) {
		assertTrue(expected - actual < 1000, () -> "Expected " + expected + " but was " + actual + ".");
	}
}
