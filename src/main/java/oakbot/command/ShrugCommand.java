package oakbot.command;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatBuilder;

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
	public String description() {
		return "lol idk";
	}

	@Override
	public String helpText(String trigger) {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.append("¯\\\\_(\u30C4)_/¯")
		);
		//@formatter:on
	}
}
