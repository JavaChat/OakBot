package oakbot.command;

import static oakbot.bot.ChatActions.post;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;

/**
 * Displays a "shrug" emoticon.
 * @author Michael Angstadt
 */
public class ShrugCommand implements Command {
	@Override
	public String name() {
		return "shrug";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("lol idk")
			.detail("Displays a \"shrug\" emoticon.")
			.includeSummaryWithDetail(false)
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		return post("¯\\\\_(\u30C4)_/¯");
	}
}
