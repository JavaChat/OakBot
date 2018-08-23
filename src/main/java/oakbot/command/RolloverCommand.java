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
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Turns the bot upside down.")
			.detail("Toggles a filter that makes all the letters in the messages Oak posts look like they are upside down.")
			.includeSummaryWithDetail(false)
		.build();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		int roomId = chatCommand.getMessage().getRoomId();
		filter.toggle(roomId);
		return null;
	}
}
