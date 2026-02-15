package oakbot.command.learn;

import static oakbot.bot.ChatActions.reply;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
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
		return List.of("forget");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Deletes a learned command.")
			.example("happy", "Deletes the command called \"happy\".")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var commandName = chatCommand.getContent().trim();
		if (commandName.isEmpty()) {
			return reply("You haven't specified the command name.", chatCommand);
		}

		if (hardcodedCommandExists(commandName)) {
			return reply("That command is not a learned command. You can only unlearn commands that were added with the \"learn\" command.", chatCommand);
		}

		var removed = learnedCommands.remove(commandName);
		if (!removed) {
			return reply("That command does not exist.", chatCommand);
		}

		return reply("Command forgotten.", chatCommand);
	}

	private boolean hardcodedCommandExists(String commandName) {
		//@formatter:off
		return Stream.concat(
			hardcodedCommands.stream().map(Command::name),
			hardcodedCommands.stream().map(Command::aliases).flatMap(Collection::stream)
		).anyMatch(commandName::equalsIgnoreCase);
		//@formatter:on
	}
}
