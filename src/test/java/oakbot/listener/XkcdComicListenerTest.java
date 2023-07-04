package oakbot.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.chat.ChatMessage;
import oakbot.task.XkcdExplainTask;

/**
 * @author Michael Angstadt
 */
public class XkcdComicListenerTest {
	private final static BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), 0);

	@Test
	public void onMessage() {
		ChatMessage chatMessage = new ChatMessage.Builder() //@formatter:off
			.messageId(1)
			.roomId(1)
			.userId(-313)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2797\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build(); //@formatter:on

		XkcdExplainTask task = new XkcdExplainTask(1) {
			@Override
			public void comicPosted(int comicId, ChatMessage comicMessage) {
				assertEquals(2797, comicId);
				assertSame(chatMessage, comicMessage);
			}
		};

		XkcdComicListener listener = new XkcdComicListener(task, -313);
		ChatActions actions = listener.onMessage(chatMessage, context);
		assertTrue(actions.isEmpty());
	}

	//ChatMessage [timestamp=2023-06-30T11:35:01, messageId=56471052, parentMessageId=0, userId=-2, username=Feeds, mentionedUserId=0, roomId=139, roomName=null, content=[fixedFont=false] <div class="onebox ob-xkcd"><a rel="nofollow noopener noreferrer" href="https://xkcd.com/2796"><img src="https://imgs.xkcd.com/comics/real_estate_analysis.png" title="Mars does get a good score on &#39;noise levels&#39; and &#39;scenic views,&#39; but the school district ranking isn&#39;t great; the only teacher--the Perseverance rover--is too busy with rock samples to teach more than the occasional weekend class." alt="Mars does get a good score on &#39;noise levels&#39; and &#39;scenic views,&#39; but the school district ranking isn&#39;t great; the only teacher--the Perseverance rover--is too busy with rock samples to teach more than the occasional weekend class." /></a></div>, edits=0, stars=0]

	@Test
	public void onMessage_wrong_user() {
		ChatMessage chatMessage = new ChatMessage.Builder() //@formatter:off
			.messageId(1)
			.roomId(1)
			.userId(1234)
			.content("<div class=\"onebox ob-xkcd\"><a rel=\"nofollow noopener noreferrer\" href=\"https://xkcd.com/2797\"><img src=\"https://imgs.xkcd.com/comics/actual_progress.png\" title=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" alt=\"Slowly progressing from &#39;how do protons behave in relativistic collisions?&#39; to &#39;what the heck are protons even doing when they&#39;re just sitting there?&#39;\" /></a></div>")
		.build(); //@formatter:on

		XkcdExplainTask task = new XkcdExplainTask(1) {
			@Override
			public void comicPosted(int comicId, ChatMessage comicMessage) {
				fail();
			}
		};

		XkcdComicListener listener = new XkcdComicListener(task, -313);
		ChatActions actions = listener.onMessage(chatMessage, context);
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

		XkcdExplainTask task = new XkcdExplainTask(1) {
			@Override
			public void comicPosted(int comicId, ChatMessage comicMessage) {
				fail();
			}
		};

		XkcdComicListener listener = new XkcdComicListener(task, -313);
		ChatActions actions = listener.onMessage(chatMessage, context);
		assertTrue(actions.isEmpty());
	}
}
