package oakbot.command;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.filter.WaduFilter;

/**
 * Makes the bot talk like Wadu.
 * @author Michael Angstadt
 */
public class WaduCommand implements Command {
	private final WaduFilter filter;

	public WaduCommand(WaduFilter filter) {
		this.filter = filter;
	}

	@Override
	public String name() {
		return "wadu";
	}

	@Override
	public String description() {
		return "Wadu hek?";
	}

	@Override
	public String helpText(String trigger) {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		int roomId = chatCommand.getMessage().getRoomId();
		boolean enabled = filter.isEnabled(roomId);
		filter.setEnabled(roomId, !enabled);
		return null;
	}
}
