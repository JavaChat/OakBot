package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.post;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.chat.ChatMessage;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Responds when a user mentions the bot's name.
 * @author Michael Angstadt
 */
public class MentionListener implements Listener {
	private final String botUsername;
	private final long cooldown = TimeUnit.MINUTES.toMillis(1);
	private Map<Integer, Long> prevResponses = new HashMap<>();
	private boolean ignore = false;

	public MentionListener(String botUsername) {
		this.botUsername = botUsername;
	}

	@Override
	public String name() {
		return "mention";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Sends a reply message when someone mentions the bot's name.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, BotContext context) {
		if (ignore) {
			ignore = false;
			return doNothing();
		}

		if (!message.getContent().isMentioned(botUsername)) {
			return doNothing();
		}

		Long prevResponse = prevResponses.get(message.getRoomId());
		if (prevResponse == null) {
			prevResponse = 0L;
		}

		long now = System.currentTimeMillis();
		long elapsed = now - prevResponse;
		if (elapsed < cooldown) {
			return doNothing();
		}

		prevResponses.put(message.getRoomId(), now);

		//@formatter:off
		return post(new ChatBuilder()
			.reply(message)
			.append("Type ").code().append(context.getTrigger()).append("help").code().append(" to see all my commands.")
		);
		//@formatter:on
	}

	/**
	 * Tells this listener to not respond to the next message it receives.
	 */
	public void ignoreNextMessage() {
		ignore = true;
	}
}
