package oakbot.bot;

import oakbot.chat.ChatMessage;

/**
 * @author Michael Angstadt
 */
public class AboutCommand implements Command {
	private final String text;

	public AboutCommand(String text) {
		this.text = text;
	}

	@Override
	public String name() {
		return "about";
	}

	@Override
	public String description() {
		return "Displays information about this bot.";
	}

	@Override
	public String helpText() {
		return description();
	}

	@Override
	public String onMessage(ChatMessage message, boolean isAdmin) {
		return text;
	}
}
