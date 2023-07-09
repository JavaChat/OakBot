package oakbot.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.chat.ChatMessage;
import oakbot.task.XkcdExplainTask;

/**
 * @author Michael Angstadt
 */
public class XkcdComicListenerTest {
	@Test
	public void onMessage() {
		ChatMessage chatMessage = new ChatMessage.Builder() //@formatter:off
			.messageId(1)
			.roomId(1)
			.userId(-313)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2797\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build(); //@formatter:on

		XkcdExplainTask task = new XkcdExplainTask("PT1S") {
			@Override
			public void comicPosted(int comicId, ChatMessage comicMessage) {
				assertEquals(2797, comicId);
				assertSame(chatMessage, comicMessage);
			}
		};
		
		IBot bot = mock(IBot.class);

		XkcdComicListener listener = new XkcdComicListener(task);
		ChatActions actions = listener.onMessage(chatMessage, bot);
		assertTrue(actions.isEmpty());
	}

	@Test
	public void onMessage_wrong_user() {
		ChatMessage chatMessage = new ChatMessage.Builder() //@formatter:off
			.messageId(1)
			.roomId(1)
			.userId(1234)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2797\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build(); //@formatter:on

		XkcdExplainTask task = new XkcdExplainTask("PT1S") {
			@Override
			public void comicPosted(int comicId, ChatMessage comicMessage) {
				fail();
			}
		};
		
		IBot bot = mock(IBot.class);

		XkcdComicListener listener = new XkcdComicListener(task);
		ChatActions actions = listener.onMessage(chatMessage, bot);
		assertTrue(actions.isEmpty());
	}

	@Test
	public void onMessage_cant_parse_comic_id() {
		ChatMessage chatMessage = new ChatMessage.Builder() //@formatter:off
			.messageId(1)
			.roomId(1)
			.userId(-313)
			.content("foobar")
		.build(); //@formatter:on

		XkcdExplainTask task = new XkcdExplainTask("PT1S") {
			@Override
			public void comicPosted(int comicId, ChatMessage comicMessage) {
				fail();
			}
		};
		
		IBot bot = mock(IBot.class);

		XkcdComicListener listener = new XkcdComicListener(task);
		ChatActions actions = listener.onMessage(chatMessage, bot);
		assertTrue(actions.isEmpty());
	}
}
