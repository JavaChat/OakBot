package oakbot.listener;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * Responds when a user mentions the bots name.
 * @author Michael Angstadt
 */
public class MentionListener implements Listener {
	private final String botUsernameMention, trigger;

	public MentionListener(String botUsername, String trigger) {
		botUsernameMention = botUsername.replace(" ", "").toLowerCase();
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
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		if (mentioned(message)) {
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
	 * Determines if the bot was mentioned in a chat message.
	 * @param message the chat message
	 * @return true if the bot was mentioned, false if not
	 */
	private boolean mentioned(ChatMessage message) {
		for (String mention : message.getMentions()) {
			mention = mention.toLowerCase();
			if (botUsernameMention.startsWith(mention)) {
				return true;
			}
		}
		return false;
	}
}
