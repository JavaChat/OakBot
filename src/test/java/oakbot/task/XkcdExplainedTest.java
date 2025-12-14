package oakbot.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;
import oakbot.util.Now;

class XkcdExplainedTest {
	/**
	 * Live test. Outputs the message to stdout.
	 */
	public static void main(String args[]) throws Exception {
		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);
		doAnswer(invocation -> {
			PostMessage message = (PostMessage) invocation.getArguments()[1];
			System.out.println(message);
			return null;
		}).when(bot).sendMessage(eq(1), any(PostMessage.class));

		//@formatter:off
		var message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("https://xkcd.com/2800")
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);
	}

	@AfterEach
	 void after() {
		Now.restore();
		HttpFactory.restore();
	}

	@Test
	void everything_is_ok() throws Exception {
		var html = new Gobble(getClass(), "xkcd-explained-2796.html").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.explainxkcd.com/wiki/index.php/2796")
			.responseOk(html)
		.build());
		//@formatter:on

		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);

		//@formatter:off
		var message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2796\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
			.id(123)
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		var expected = new PostMessage("**[XKCD #2796 Explained](https://www.explainxkcd.com/wiki/index.php/2796):** This *comic* shows a chart ranking locations in our solar system \\(the eight currently recognised planets and Earth's own moon\\) along two scales: their walkability and their proximity to shops. As this is a \"real estate analysis\", this comic mocks real life \"real estate analyses\" for people who are looking for a new home. Walkability measures the ease of walking as a form of transportation in an area \\(often ...").parentId(123);
		verify(bot).sendMessage(1, expected);

		assertTrue(task.comicsByRoom.isEmpty());
	}
	
	@Test
	void explanation_incomplete_wrapped_in_div() throws Exception {
		var html = new Gobble(getClass(), "xkcd-explained-3072-explanation-incomplete-wrapped-in-div.html").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.explainxkcd.com/wiki/index.php/3072")
			.responseOk(html)
		.build());
		//@formatter:on

		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);

		//@formatter:off
		var message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/3072\"><img src=\"https://imgs.xkcd.com/comics/stargazing_4.png\" title=\"We haven't actually seen a star fall in since we invented telescopes, but I have a list of ones I'm really hoping are next.\" alt=\"We haven't actually seen a star fall in since we invented telescopes, but I have a list of ones I'm really hoping are next.\"></a></div>")
			.id(123)
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		var expected = new PostMessage("**[XKCD #3072 Explained](https://www.explainxkcd.com/wiki/index.php/3072):** This is the fourth comic in the [Stargazing](https://www.explainxkcd.com/wiki/index.php/Category:Stargazing) series, and it followed [2274: Stargazing 3](https://www.explainxkcd.com/wiki/index.php/2274:_Stargazing_3) that came out five years before. That was the longest stretch between two comics in the series so far.").parentId(123);
		verify(bot).sendMessage(1, expected);

		assertTrue(task.comicsByRoom.isEmpty());
	}

	@Test
	void first_paragraph_not_wrapped_in_p_element() throws Exception {
		var html = new Gobble(getClass(), "xkcd-explained-2796-first-para-not-wrapped-in-p-element.html").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.explainxkcd.com/wiki/index.php/2796")
			.responseOk(html)
		.build());
		//@formatter:on

		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);

		//@formatter:off
		var message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2796\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
			.id(123)
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		var expected = new PostMessage("**[XKCD #2796 Explained](https://www.explainxkcd.com/wiki/index.php/2796):** This *comic* shows a chart ranking locations in our solar system \\(the eight currently recognised planets and Earth's own moon\\) along two scales: their walkability and their proximity to shops. As this is a \"real estate analysis\", this comic mocks real life \"real estate analyses\" for people who are looking for a new home. Walkability measures the ease of walking as a form of transportation in an area \\(often ...").parentId(123);
		verify(bot).sendMessage(1, expected);

		assertTrue(task.comicsByRoom.isEmpty());
	}

	@Test
	void first_paragraph_has_newlines() throws Exception {
		var html = new Gobble(getClass(), "xkcd-explained-2796-has-newlines.html").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.explainxkcd.com/wiki/index.php/2796")
			.responseOk(html)
		.build());
		//@formatter:on

		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);

		//@formatter:off
		var message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2796\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
			.id(123)
		.build();
		//@formatter:on

		task.onMessage(message, bot);
		task.run(bot);

		var expected = new PostMessage("**[XKCD #2796 Explained](https://www.explainxkcd.com/wiki/index.php/2796):** This *comic* shows a chart ranking locations in our solar system \\(the eight currently recognised planets and Earth's own moon\\) along two scales: their walkability and their proximity to shops. As this is a \"real estate analysis\", this comic mocks real life \"real estate analyses\" for people who are looking for a new home. Walkability measures the ease of walking as a form of transportation in an area \\(often ...").parentId(123);
		verify(bot).sendMessage(1, expected);

		assertTrue(task.comicsByRoom.isEmpty());
	}

	@Test
	void initial_wait_time() throws Exception {
		var task = new XkcdExplained("PT1H");

		var bot = mock(IBot.class);

		//@formatter:off
		var message = new ChatMessage.Builder()
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

		var comic = task.comicsByRoom.get(1);
		assertEquals(2802, comic.comicId);
		assertSame(message, comic.messageContainingComic);
		assertEquals(0, comic.timesCheckedWiki);
		assertNull(comic.lastCheckedWiki);
	}

	@Test
	void no_wiki_page() throws Exception {
		var html = new Gobble(getClass(), "xkcd-explained-404-response.html").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.explainxkcd.com/wiki/index.php/2802")
			.response(404, html)
		.build());
		//@formatter:on

		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);

		//@formatter:off
		var message = new ChatMessage.Builder()
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

		var comic = task.comicsByRoom.get(1);
		assertEquals(2802, comic.comicId);
		assertSame(message, comic.messageContainingComic);
		assertEquals(1, comic.timesCheckedWiki);
		assertNotNull(comic.lastCheckedWiki);
	}

	@Test
	void explanation_not_posted_yet() throws Exception {
		var html = new Gobble(getClass(), "xkcd-explained-no-explanation.html").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.explainxkcd.com/wiki/index.php/2796")
			.responseOk(html)
		.build());
		//@formatter:on

		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);

		//@formatter:off
		var message = new ChatMessage.Builder()
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

		var comic = task.comicsByRoom.get(1);
		assertEquals(2796, comic.comicId);
		assertSame(message, comic.messageContainingComic);
		assertEquals(1, comic.timesCheckedWiki);
		assertNotNull(comic.lastCheckedWiki);
	}

	@Test
	void ioexception() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.explainxkcd.com/wiki/index.php/2796")
			.response(new IOException())
		.build());
		//@formatter:on

		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);

		//@formatter:off
		var message = new ChatMessage.Builder()
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

		var comic = task.comicsByRoom.get(1);
		assertEquals(2796, comic.comicId);
		assertSame(message, comic.messageContainingComic);
		assertEquals(1, comic.timesCheckedWiki);
		assertNotNull(comic.lastCheckedWiki);
	}

	@Test
	void human_user_id() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
		.build());
		//@formatter:on

		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);

		//@formatter:off
		var message = new ChatMessage.Builder()
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
	void system_bot_posted_something_else() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
		.build());
		//@formatter:on

		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);

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
	void give_up() throws Exception {
		var html = new Gobble(getClass(), "xkcd-explained-no-explanation.html").asString();

		var mockHttp = new MockHttpClientBuilder();
		for (int i = 0; i < 8; i++) {
			mockHttp.requestGet("https://www.explainxkcd.com/wiki/index.php/2796");
			mockHttp.responseOk(html);
		}
		HttpFactory.inject(mockHttp.build());

		var task = new XkcdExplained("PT0S");

		var bot = mock(IBot.class);

		//@formatter:off
		var message = new ChatMessage.Builder()
			.timestamp(LocalDateTime.now())
			.userId(-2)
			.roomId(1)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2796\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build();
		//@formatter:on

		task.onMessage(message, bot);

		task.run(bot);
		assertEquals(1, task.comicsByRoom.size());
		var comic = task.comicsByRoom.get(1);
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

		Now.fastForward(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(2, comic.timesCheckedWiki);

		Now.fastForward(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(3, comic.timesCheckedWiki);

		Now.fastForward(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(4, comic.timesCheckedWiki);

		Now.fastForward(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(5, comic.timesCheckedWiki);

		Now.fastForward(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(6, comic.timesCheckedWiki);

		Now.fastForward(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(7, comic.timesCheckedWiki);

		Now.fastForward(Duration.ofMinutes(15));
		task.run(bot);
		assertEquals(8, comic.timesCheckedWiki);

		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));
		assertTrue(task.comicsByRoom.isEmpty());
	}
}
