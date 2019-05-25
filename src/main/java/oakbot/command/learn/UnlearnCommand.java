package oakbot.command.learn;

import static oakbot.bot.ChatActions.reply;

import java.util.Arrays;
import java.util.List;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.command.Command;
import oakbot.command.HelpDoc;

/**
 * Removes a learned command.
 * @author Michael Angstadt
 */
public class UnlearnCommand implements Command {
	private final List<Command> hardcodedCommands;
	private final LearnedCommandsDao learnedCommands;

	public UnlearnCommand(List<Command> hardcodedCommands, LearnedCommandsDao learnedCommands) {
		this.hardcodedCommands = hardcodedCommands;
		this.learnedCommands = learnedCommands;
	}

	@Override
	public String name() {
		return "unlearn";
	}

	@Override
	public List<String> aliases() {
		return Arrays.asList("forget");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Makes the bot forget a learned command.")
			.example("happy", "Deletes the command called \"happy\".")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, BotContext context) {
		String commandName = chatCommand.getContent().trim();
		if (commandName.isEmpty()) {
			return reply("You haven't specified the command name.", chatCommand);
		}

		if (hardcodedCommandExists(commandName)) {
			return reply("That command is not a learned command. You can only unlearn commands that were added with the \"learn\" command.", chatCommand);
		}

		boolean removed = learnedCommands.remove(commandName);
		if (!removed) {
			return reply("That command does not exist.", chatCommand);
		}

		return reply("Command forgotten.", chatCommand);
	}

	private boolean hardcodedCommandExists(String commandName) {
		for (Command command : hardcodedCommands) {
			if (commandName.equalsIgnoreCase(command.name())) {
				return true;
			}
			for (String alias : command.aliases()) {
				if (commandName.equalsIgnoreCase(alias)) {
					return true;
				}
			}
		}
		return false;
	}
}
