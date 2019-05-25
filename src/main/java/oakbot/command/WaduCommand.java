package oakbot.command;

import static oakbot.bot.ChatActions.doNothing;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
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
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Wadu hek?")
			.detail("Toggles a filter that makes Oak speak in Wadu Hek.")
			.includeSummaryWithDetail(false)
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, BotContext context) {
		int roomId = chatCommand.getMessage().getRoomId();
		filter.toggle(roomId);
		return doNothing();
	}
}
