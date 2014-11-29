package oakbot.bot;

import static oakbot.util.ChatUtils.reply;
import oakbot.chat.ChatMessage;

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
	public String onMessage(ChatMessage message, boolean isAdmin) {
		if (isAdmin) {
			throw new ShutdownException("Shutting down.  See you later.");
		}
		return reply(message, "Only admins can shut me down.");
	}
}
