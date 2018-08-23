package oakbot.command.learn;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * A command that was taught to the bot at runtime using the "learn" command.
 * @author Michael Angstadt
 */
public class LearnedCommand implements Command {
	private final String name, output;

	/**
	 * @param name the command name
	 * @param output the text the command will output
	 */
	public LearnedCommand(String name, String output) {
		this.name = name;
		this.output = output;
	}

	@Override
	public String name() {
		return name;
	}

	public String output() {
		return output;
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("This command was created at runtime by chat room user. It does not have a help message.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.append(output)
		);
		//@formatter:on
	}
}
