package oakbot.task;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.BeforeClass;
import org.junit.Test;

import oakbot.bot.Bot;
import oakbot.bot.PostMessage;
import oakbot.util.Gobble;

/**
 * @author Michael Angstadt
 */
public class FOTDTest {
	private static String refdeskPage;

	/**
	 * Live test. Outputs current FOTD to stdout.
	 */
	public static void main(String args[]) throws Exception {
		FOTD fotd = new FOTD() {
			@Override
			void broadcast(PostMessage response, Bot bot) {
				System.out.println(response.message());
			}
		};
		fotd.run(null);
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		try (InputStream in = FOTDTest.class.getResourceAsStream("refdesk.html")) {
			refdeskPage = new Gobble(in).asString();
		}
	}

	@Test
	public void nextRun_morning() {
		FOTD task = new FOTD() {
			@Override
			LocalDateTime now() {
				return LocalDateTime.of(2018, 7, 19, 11, 0, 0);
			}
		};

		long nextRun = task.nextRun();
		assertEquals(Duration.ofHours(1).toMillis(), nextRun);
	}

	@Test
	public void nextRun_afternoon() {
		FOTD task = new FOTD() {
			@Override
			LocalDateTime now() {
				return LocalDateTime.of(2018, 7, 19, 13, 0, 0);
			}
		};

		long nextRun = task.nextRun();
		assertEquals(Duration.ofHours(23).toMillis(), nextRun);
	}

	@Test
	public void run() throws Exception {
		FOTD task = spy(new FOTD() {
			@Override
			String get(String url) {
				assertEquals("http://www.refdesk.com", url);
				return refdeskPage.replace("${fact}", "The <b>fact</b> - Provided by <a href=http://www.factretriever.com/>FactRetriever.com</a>");
			}

			@Override
			void broadcast(PostMessage response, Bot bot) throws IOException {
				assertEquals("The **fact** [(source)](http://www.refdesk.com/fotd-arch.html)", response.message());
			}
		});

		Bot bot = mock(Bot.class);
		task.run(bot);

		verify(task).broadcast(any(PostMessage.class), eq(bot));
	}

	@Test
	public void run_no_dash() throws Exception {
		FOTD task = spy(new FOTD() {
			@Override
			String get(String url) {
				assertEquals("http://www.refdesk.com", url);
				return refdeskPage.replace("${fact}", "The <b>fact</b><br>Provided by <a href=http://www.factretriever.com/>FactRetriever.com</a>");
			}

			@Override
			void broadcast(PostMessage response, Bot bot) throws IOException {
				assertEquals("The **fact** [(source)](http://www.refdesk.com/fotd-arch.html)", response.message());
			}
		});

		Bot bot = mock(Bot.class);
		task.run(bot);

		verify(task).broadcast(any(PostMessage.class), eq(bot));
	}

	@Test
	public void run_multiline() throws Exception {
		FOTD task = spy(new FOTD() {
			@Override
			String get(String url) {
				assertEquals("http://www.refdesk.com", url);
				return refdeskPage.replace("${fact}", "The <b>fact</b>\nline two - Provided by <a href=http://www.factretriever.com/>FactRetriever.com</a>");
			}

			@Override
			void broadcast(PostMessage response, Bot bot) throws IOException {
				assertEquals("The <b>fact</b>\nline two\nSource: http://www.refdesk.com/fotd-arch.html", response.message());
			}
		});

		Bot bot = mock(Bot.class);
		task.run(bot);

		verify(task).broadcast(any(PostMessage.class), eq(bot));
	}

	@Test
	public void run_fact_not_found() throws Exception {
		FOTD task = spy(new FOTD() {
			@Override
			String get(String url) {
				assertEquals("http://www.refdesk.com", url);
				return "<html></html>";
			}
		});

		Bot bot = mock(Bot.class);
		task.run(bot);

		verify(task, never()).broadcast(any(PostMessage.class), eq(bot));
	}
}
