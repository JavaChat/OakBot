package oakbot.listener;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * Responds when a user mentions the bots name.
 * @author Michael Angstadt
 */
public class MentionListener implements Listener {
	private final String botUsername, trigger;
	private boolean ignore = false;

	public MentionListener(String botUsername, String trigger) {
		this.botUsername = botUsername;
		this.trigger = trigger;
	}

	@Override
	public String name() {
		return "mention";
	}

	@Override
	public String description() {
		return "Sends a reply message when someone mentions the bot's name.";
	}

	@Override
	public String helpText() {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, BotContext context) {
		if (ignore) {
			ignore = false;
			return null;
		}

		if (message.isMentioned(botUsername)) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("Type ").code().append(trigger).append("help").code().append(" to see all my commands.")
			);
			//@formatter:on
		}

		return null;
	}

	/**
	 * Tells this listener to not respond to the next message it receives.
	 */
	public void ignoreNextMessage() {
		ignore = true;
	}
}
