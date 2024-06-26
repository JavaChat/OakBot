package oakbot.task;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;
import oakbot.util.Now;

/**
 * @author Michael Angstadt
 */
class FOTDTest {
	private static String refdeskPage;

	/**
	 * Live test. Outputs current FOTD to stdout.
	 */
	public static void main(String args[]) throws Exception {
		var bot = mock(IBot.class);
		doAnswer(invocation -> {
			PostMessage response = (PostMessage) invocation.getArguments()[0];
			System.out.println(response.message());
			return null;
		}).when(bot).broadcastMessage(any(PostMessage.class));

		var fotd = new FOTD();
		fotd.run(bot);
	}

	@BeforeAll
	static void beforeClass() throws Exception {
		refdeskPage = new Gobble(FOTDTest.class, "refdesk.html").asString();
	}

	@AfterEach
	void after() {
		Now.restore();
		HttpFactory.restore();
	}

	@Test
	void nextRun_morning() {
		Now.setNow(LocalDateTime.of(2018, 7, 19, 11, 0, 0));

		var task = new FOTD();

		var expected = Duration.ofHours(1).toMillis();
		var actual = task.nextRun();
		assertApprox(expected, actual);
	}

	@Test
	void nextRun_afternoon() {
		Now.setNow(LocalDateTime.of(2018, 7, 19, 13, 0, 0));

		var task = new FOTD();

		var expected = Duration.ofHours(23).toMillis();
		var actual = task.nextRun();
		assertApprox(expected, actual);
	}

	@Test
	void run() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://www.refdesk.com")
			.responseOk(refdeskPage.replace("${fact}", "The <b>fact</b> - Provided by <a href=http://www.factretriever.com/>FactRetriever.com</a>"))
		.build());
		//@formatter:on

		var task = new FOTD();

		var bot = mock(IBot.class);
		task.run(bot);

		var expected = new PostMessage("The **fact** [(source)](http://www.refdesk.com/fotd-arch.html)").splitStrategy(SplitStrategy.WORD);
		verify(bot).broadcastMessage(expected);
	}

	@Test
	void run_no_dash() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://www.refdesk.com")
			.responseOk(refdeskPage.replace("${fact}", "The <b>fact</b><br>Provided by <a href=http://www.factretriever.com/>FactRetriever.com</a>"))
		.build());
		//@formatter:on

		var task = new FOTD();

		var bot = mock(IBot.class);
		task.run(bot);

		var expected = new PostMessage("The **fact** [(source)](http://www.refdesk.com/fotd-arch.html)").splitStrategy(SplitStrategy.WORD);
		verify(bot).broadcastMessage(expected);
	}

	@Test
	void run_multiline() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://www.refdesk.com")
			.responseOk(refdeskPage.replace("${fact}", "The <b>fact</b>\nline two - Provided by <a href=http://www.factretriever.com/>FactRetriever.com</a>"))
		.build());
		//@formatter:on

		var task = new FOTD();

		var bot = mock(IBot.class);
		task.run(bot);

		var expected = new PostMessage("The <b>fact</b>\nline two\nSource: http://www.refdesk.com/fotd-arch.html").splitStrategy(SplitStrategy.WORD);
		verify(bot).broadcastMessage(expected);
	}

	@Test
	void run_fact_not_found() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://www.refdesk.com")
			.responseOk("<html></html>")
		.build());
		//@formatter:on

		var task = new FOTD();

		var bot = mock(IBot.class);
		task.run(bot);

		verify(bot, never()).broadcastMessage(any(PostMessage.class));
	}

	private static void assertApprox(long expected, long actual) {
		assertTrue(expected - actual < 1000, () -> "Expected " + expected + " but was " + actual + ".");
	}
}
