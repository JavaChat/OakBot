package oakbot.task;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.chat.MockHttpClientBuilder;
import oakbot.chat.SplitStrategy;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.Now;

/**
 * @author Michael Angstadt
 */
public class FOTDTest {
	private static String refdeskPage;

	/**
	 * Live test. Outputs current FOTD to stdout.
	 */
	public static void main(String args[]) throws Exception {
		IBot bot = mock(IBot.class);
		doAnswer(invocation -> {
			PostMessage response = (PostMessage) invocation.getArguments()[0];
			System.out.println(response.message());
			return null;
		}).when(bot).broadcastMessage(any(PostMessage.class));

		FOTD fotd = new FOTD();
		fotd.run(bot);
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		refdeskPage = new Gobble(FOTDTest.class, "refdesk.html").asString();
	}

	@After
	public void after() {
		Now.restore();
		HttpFactory.restore();
	}

	@Test
	public void nextRun_morning() {
		Now.setNow(LocalDateTime.of(2018, 7, 19, 11, 0, 0));

		FOTD task = new FOTD();

		long expected = Duration.ofHours(1).toMillis();
		long actual = task.nextRun();
		assertApprox(expected, actual);
	}

	@Test
	public void nextRun_afternoon() {
		Now.setNow(LocalDateTime.of(2018, 7, 19, 13, 0, 0));

		FOTD task = new FOTD();

		long expected = Duration.ofHours(23).toMillis();
		long actual = task.nextRun();
		assertApprox(expected, actual);
	}

	@Test
	public void run() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://www.refdesk.com")
			.responseOk(refdeskPage.replace("${fact}", "The <b>fact</b> - Provided by <a href=http://www.factretriever.com/>FactRetriever.com</a>"))
		.build());
		//@formatter:on

		FOTD task = new FOTD();

		IBot bot = mock(IBot.class);
		task.run(bot);

		PostMessage expected = new PostMessage("The **fact** [(source)](http://www.refdesk.com/fotd-arch.html)").splitStrategy(SplitStrategy.WORD);
		verify(bot).broadcastMessage(expected);
	}

	@Test
	public void run_no_dash() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://www.refdesk.com")
			.responseOk(refdeskPage.replace("${fact}", "The <b>fact</b><br>Provided by <a href=http://www.factretriever.com/>FactRetriever.com</a>"))
		.build());
		//@formatter:on

		FOTD task = new FOTD();

		IBot bot = mock(IBot.class);
		task.run(bot);

		PostMessage expected = new PostMessage("The **fact** [(source)](http://www.refdesk.com/fotd-arch.html)").splitStrategy(SplitStrategy.WORD);
		verify(bot).broadcastMessage(expected);
	}

	@Test
	public void run_multiline() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://www.refdesk.com")
			.responseOk(refdeskPage.replace("${fact}", "The <b>fact</b>\nline two - Provided by <a href=http://www.factretriever.com/>FactRetriever.com</a>"))
		.build());
		//@formatter:on

		FOTD task = new FOTD();

		IBot bot = mock(IBot.class);
		task.run(bot);

		PostMessage expected = new PostMessage("The <b>fact</b>\nline two\nSource: http://www.refdesk.com/fotd-arch.html").splitStrategy(SplitStrategy.WORD);
		verify(bot).broadcastMessage(expected);
	}

	@Test
	public void run_fact_not_found() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://www.refdesk.com")
			.responseOk("<html></html>")
		.build());
		//@formatter:on

		FOTD task = new FOTD();

		IBot bot = mock(IBot.class);
		task.run(bot);

		verify(bot, never()).broadcastMessage(any(PostMessage.class));
	}

	private static void assertApprox(long expected, long actual) {
		assertTrue("Expected " + expected + " but was " + actual + ".", expected - actual < 1000);
	}
}
