package oakbot.command;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.filter.GrootFilter;

/**
 * Makes the bot talk in Groot.
 * @author Michael Angstadt
 */
public class GrootCommand implements Command {
	private final GrootFilter filter;

	public GrootCommand(GrootFilter filter) {
		this.filter = filter;
	}

	@Override
	public String name() {
		return "groot";
	}

	@Override
	public String description() {
		return "I am Groot.";
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
