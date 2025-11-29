package oakbot.bot;

import com.github.mangstadt.sochat4j.ChatMessage;

/**
 * Context information for executing chat actions.
 * Provides access to bot functionality without exposing entire Bot class.
 */
public class ActionContext {
	private final Bot bot;
	private final ChatMessage message;

	public ActionContext(Bot bot, ChatMessage message) {
		this.bot = bot;
		this.message = message;
	}

	public Bot getBot() {
		return bot;
	}

	public ChatMessage getMessage() {
		return message;
	}

	public int getRoomId() {
		return message.getRoomId();
	}
}
