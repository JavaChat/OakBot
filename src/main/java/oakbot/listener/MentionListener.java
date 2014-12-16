package oakbot.listener;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * Responds when a user mentions the bots name.
 * @author Michael Angstadt
 */
public class MentionListener implements Listener {
	private final String mention, mentionWithoutSpaces;
	private final String trigger;

	public MentionListener(String botName, String trigger) {
		mention = "@" + botName.toLowerCase();
		mentionWithoutSpaces = mention.replace(" ", "");
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
		String contentToLower = message.getContent().toLowerCase();
		if (!contentToLower.contains(mention) && !contentToLower.contains(mentionWithoutSpaces)) {
			return null;
		}

		ChatBuilder cb = new ChatBuilder();
		cb.reply(message).append("Type ").code(trigger + "help").append(" to see all my commands.");
		return new ChatResponse(cb.toString());
	}

}
