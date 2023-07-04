package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.chat.ChatMessage;
import oakbot.task.XkcdExplainTask;

/**
 * Listens for when an XKCD comic is posted so that a description from
 * explainxkcd.com can be posted.
 * @author Michael Angstadt
 */
public class XkcdComicListener implements Listener {
	private static final Pattern regex = Pattern.compile("https://xkcd.com/(\\d+)");

	private final XkcdExplainTask task;
	private final int feedUserId;

	/**
	 * @param task the task that posts the explanation
	 * @param feedUserId the user ID of the user that posts the XKCD comics
	 */
	public XkcdComicListener(XkcdExplainTask task, int feedUserId) {
		this.task = task;
		this.feedUserId = feedUserId;
	}

	@Override
	public ChatActions onMessage(ChatMessage message, BotContext context) {
		if (message.getUserId() == feedUserId) {
			Matcher m = regex.matcher(message.getContent().getContent());
			if (m.find()) {
				int comicId = Integer.parseInt(m.group(1));
				task.comicPosted(comicId, message);
			}
		}

		return doNothing();
	}
}
