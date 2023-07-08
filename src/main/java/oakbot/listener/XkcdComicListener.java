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

	private final XkcdExplainTask explainTask;

	/**
	 * @param explainTask the task that posts the explanation
	 */
	public XkcdComicListener(XkcdExplainTask explainTask) {
		this.explainTask = explainTask;
	}

	@Override
	public ChatActions onMessage(ChatMessage message, BotContext context) {
		if (postedByBot(message)) {
			Matcher m = regex.matcher(message.getContent().getContent());
			if (m.find()) {
				int comicId = Integer.parseInt(m.group(1));
				explainTask.comicPosted(comicId, message);
			}
		}

		return doNothing();
	}

	/**
	 * Determines if the given message was posted by a bot user.
	 * @param message the chat message
	 * @return true if it was posted by a bot user, false if not
	 */
	private boolean postedByBot(ChatMessage message) {
		return message.getUserId() < 1;
	}
}
