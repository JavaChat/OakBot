package oakbot.bot;

import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * Shuts down the bot.
 * @author Michael Angstadt
 */
public class ShutdownCommand implements Command {

	@Override
	public String name() {
		return "shutdown";
	}

	@Override
	public String description() {
		return "Terminates the bot (admins only).";
	}

	@Override
	public String helpText() {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		if (isAdmin) {
			throw new ShutdownException("Shutting down.  See you later.");
		}
		String reply = new ChatBuilder().reply(message).append("Only admins can shut me down.").toString();
		return new ChatResponse(reply);
	}
}
