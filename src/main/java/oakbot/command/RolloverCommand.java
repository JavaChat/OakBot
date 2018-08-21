package oakbot.command;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.filter.UpsidedownTextFilter;

/**
 * Turns the bot upside down.
 * @author Michael Angstadt
 */
public class RolloverCommand implements Command {
	private final UpsidedownTextFilter filter;

	public RolloverCommand(UpsidedownTextFilter filter) {
		this.filter = filter;
	}

	@Override
	public String name() {
		return "rollover";
	}

	@Override
	public String description() {
		return "Turns the bot upside down.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new HelpBuilder(trigger, this)
			.description("Toggles a filter that makes all the letters in the messages Oak posts look like they are upside down.")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		int roomId = chatCommand.getMessage().getRoomId();
		filter.toggle(roomId);
		return null;
	}
}
