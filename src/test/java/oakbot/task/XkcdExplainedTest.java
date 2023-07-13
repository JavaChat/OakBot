package oakbot.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.chat.ChatMessage;
import oakbot.chat.MockHttpClientBuilder;
import oakbot.task.XkcdExplained.Comic;
import oakbot.util.Gobble;

public class XkcdExplainedTest {
	/**
	 * Live test. Outputs the message to stdout.
	 */
	public static void main(String args[]) throws Exception {
		XkcdExplained task = new XkcdExplained("PT0S");

		IBot bot = mock(IBot.class);
		doAnswer(invocation -> {
			PostMessage message = (PostMessage) invocation.getArguments()[1];
			System.out.println(message);
			return null;
		}).when(bot).sendMessage(eq(1), any(PostMessage.class));

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("https://xkcd.com/2800")
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);
	}

	@Test
	public void everything_is_ok() throws Exception {
		String html;
		try (InputStream in = XkcdExplainedTest.class.getResourceAsStream("xkcd-explained-2796.html")) {
			html = new Gobble(in).asString();
		}

		XkcdExplained task = new XkcdExplained("PT0S") {
			@Override
			CloseableHttpClient httpClient() {
				//@formatter:off
				return new MockHttpClientBuilder()
					.request("GET", "https://www.explainxkcd.com/wiki/index.php/2796")
					.response(200, html)
				.build();
				//@formatter:on
			}
		};

		IBot bot = mock(IBot.class);

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2796\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		PostMessage expected = new PostMessage(":0 **[XKCD #2796 Explained](https://www.explainxkcd.com/wiki/index.php/2796):** This *comic* shows a chart ranking locations in our solar system \\(the eight currently recognised planets and Earth's own moon\\) along two scales: their walkability and their proximity to shops. As this is a \"real estate analysis\", this comic mocks real life \"real estate analyses\" for people who are looking for a new home. Walkability measures the ease of walking as a form of transportation in an area \\(often ...");
		verify(bot).sendMessage(1, expected);

		assertTrue(task.comicsByRoom.isEmpty());
	}

	@Test
	public void first_paragraph_not_wrapped_in_p_element() throws Exception {
		String html;
		try (InputStream in = XkcdExplainedTest.class.getResourceAsStream("xkcd-explained-2796-first-para-not-wrapped-in-p-element.html")) {
			html = new Gobble(in).asString();
		}

		XkcdExplained task = new XkcdExplained("PT0S") {
			@Override
			CloseableHttpClient httpClient() {
				//@formatter:off
				return new MockHttpClientBuilder()
					.request("GET", "https://www.explainxkcd.com/wiki/index.php/2796")
					.response(200, html)
				.build();
				//@formatter:on
			}
		};

		IBot bot = mock(IBot.class);

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2796\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		PostMessage expected = new PostMessage(":0 **[XKCD #2796 Explained](https://www.explainxkcd.com/wiki/index.php/2796):** This *comic* shows a chart ranking locations in our solar system \\(the eight currently recognised planets and Earth's own moon\\) along two scales: their walkability and their proximity to shops. As this is a \"real estate analysis\", this comic mocks real life \"real estate analyses\" for people who are looking for a new home. Walkability measures the ease of walking as a form of transportation in an area \\(often ...");
		verify(bot).sendMessage(1, expected);

		assertTrue(task.comicsByRoom.isEmpty());
	}

	@Test
	public void initial_wait_time() throws Exception {
		XkcdExplained task = new XkcdExplained("PT1H") {
			@Override
			CloseableHttpClient httpClient() {
				return new MockHttpClientBuilder().build();
			}
		};

		IBot bot = mock(IBot.class);

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2802\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		assertEquals(1, task.comicsByRoom.size());

		Comic comic = task.comicsByRoom.get(1);
		assertEquals(2802, comic.comicId);
		assertSame(message, comic.messageContainingComic);
		assertEquals(0, comic.timesCheckedWiki);
		assertNull(comic.lastCheckedWiki);
	}

	@Test
	public void no_wiki_page() throws Exception {
		String html;
		try (InputStream in = XkcdExplainedTest.class.getResourceAsStream("xkcd-explained-404-response.html")) {
			html = new Gobble(in).asString();
		}

		XkcdExplained task = new XkcdExplained("PT0S") {
			@Override
			CloseableHttpClient httpClient() {
				//@formatter:off
				return new MockHttpClientBuilder()
					.request("GET", "https://www.explainxkcd.com/wiki/index.php/2802")
					.response(404, html)
				.build();
				//@formatter:on
			}
		};

		IBot bot = mock(IBot.class);

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2802\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		assertEquals(1, task.comicsByRoom.size());

		Comic comic = task.comicsByRoom.get(1);
		assertEquals(2802, comic.comicId);
		assertSame(message, comic.messageContainingComic);
		assertEquals(1, comic.timesCheckedWiki);
		assertNotNull(comic.lastCheckedWiki);
	}

	@Test
	public void explanation_not_posted_yet() throws Exception {
		String html;
		try (InputStream in = XkcdExplainedTest.class.getResourceAsStream("xkcd-explained-no-explanation.html")) {
			html = new Gobble(in).asString();
		}

		XkcdExplained task = new XkcdExplained("PT0S") {
			@Override
			CloseableHttpClient httpClient() {
				//@formatter:off
				return new MockHttpClientBuilder()
					.request("GET", "https://www.explainxkcd.com/wiki/index.php/2796")
					.response(200, html)
				.build();
				//@formatter:on
			}
		};

		IBot bot = mock(IBot.class);

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2796\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		assertEquals(1, task.comicsByRoom.size());

		Comic comic = task.comicsByRoom.get(1);
		assertEquals(2796, comic.comicId);
		assertSame(message, comic.messageContainingComic);
		assertEquals(1, comic.timesCheckedWiki);
		assertNotNull(comic.lastCheckedWiki);
	}

	@Test
	public void ioexception() throws Exception {
		XkcdExplained task = new XkcdExplained("PT0S") {
			@Override
			CloseableHttpClient httpClient() {
				throw new UncheckedIOException(new IOException());
			}
		};

		IBot bot = mock(IBot.class);

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2796\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		assertEquals(1, task.comicsByRoom.size());

		Comic comic = task.comicsByRoom.get(1);
		assertEquals(2796, comic.comicId);
		assertSame(message, comic.messageContainingComic);
		assertEquals(1, comic.timesCheckedWiki);
		assertNotNull(comic.lastCheckedWiki);
	}

	@Test
	public void human_user_id() throws Exception {
		XkcdExplained task = new XkcdExplained("PT0S") {
			@Override
			CloseableHttpClient httpClient() {
				return new MockHttpClientBuilder().build();
			}
		};

		IBot bot = mock(IBot.class);

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(100)
			.roomId(1)
			.content("My favorite comic is this one: https://xkcd.com/2796")
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		assertTrue(task.comicsByRoom.isEmpty());
	}

	@Test
	public void system_bot_posted_something_else() throws Exception {
		XkcdExplained task = new XkcdExplained("PT0S") {
			@Override
			CloseableHttpClient httpClient() {
				return new MockHttpClientBuilder().build();
			}
		};

		IBot bot = mock(IBot.class);

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-blog\"><div class=\"ob-blog-title\"><a href=\"http://www.qwantz.com/index.php?comic=4072\">Dinosaur Comics!: honestly i would LOVE to get a letter like this, not a word of lie</a></div><div class=\"ob-blog-meta\">posted on July 12, 2023</div><div class=\"ob-blog-text\"><p>archive - contact - sexy exciting merchandise - search - about ← previousJuly 12th, 2023nextJuly 12th, 2023: My new book DANGER AND OTHER UNKNOWN RISKS is out now and it's getting really good reviews!  Which is great!  Hopefully that means you like it too??  Hey don't forget to sign up for DINOSAUR COMICS to TEXT YOU to your PERSONAL HANDHELD COMPUTER PHONE!!– Ryan</p></div></div>")
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		assertTrue(task.comicsByRoom.isEmpty());
	}

	@Test
	public void give_up() throws Exception {
		String html;
		try (InputStream in = XkcdExplainedTest.class.getResourceAsStream("xkcd-explained-no-explanation.html")) {
			html = new Gobble(in).asString();
		}

		XkcdExplained task = new XkcdExplained("PT0S") {
			@Override
			CloseableHttpClient httpClient() {
				MockHttpClientBuilder b = new MockHttpClientBuilder();
				for (int i = 0; i < 8; i++) {
					b.request("GET", "https://www.explainxkcd.com/wiki/index.php/2796");
					b.response(200, html);
				}
				return b.build();
			}
		};

		IBot bot = mock(IBot.class);

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2796\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build();
		//@formatter:on

		task.onMessage(message, bot);

		task.run(bot);
		assertEquals(1, task.comicsByRoom.size());
		Comic comic = task.comicsByRoom.get(1);
		assertEquals(2796, comic.comicId);
		assertSame(message, comic.messageContainingComic);
		assertEquals(1, comic.timesCheckedWiki);
		assertNotNull(comic.lastCheckedWiki);

		/*
		 * Wiki was already checked within the last 15 minutes, so it shouldn't
		 * check again.
		 */
		task.run(bot);
		assertEquals(1, comic.timesCheckedWiki);

		/**
		 * Simulate 15 minute passing.
		 */
		comic.lastCheckedWiki = Instant.now().minus(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(2, comic.timesCheckedWiki);

		/**
		 * Simulate 15 minute passing.
		 */
		comic.lastCheckedWiki = Instant.now().minus(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(3, comic.timesCheckedWiki);

		/**
		 * Simulate 15 minute passing.
		 */
		comic.lastCheckedWiki = Instant.now().minus(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(4, comic.timesCheckedWiki);

		/**
		 * Simulate 15 minute passing.
		 */
		comic.lastCheckedWiki = Instant.now().minus(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(5, comic.timesCheckedWiki);

		/**
		 * Simulate 15 minute passing.
		 */
		comic.lastCheckedWiki = Instant.now().minus(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(6, comic.timesCheckedWiki);

		/**
		 * Simulate 15 minute passing.
		 */
		comic.lastCheckedWiki = Instant.now().minus(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(7, comic.timesCheckedWiki);

		/**
		 * Simulate 15 minute passing.
		 */
		comic.lastCheckedWiki = Instant.now().minus(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(8, comic.timesCheckedWiki);

		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));
		assertTrue(task.comicsByRoom.isEmpty());
	}
}
