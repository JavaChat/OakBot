package oakbot.command.learn;

import java.util.List;

import oakbot.bot.Bot;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatCommand;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;

/**
 * Teaches the bot a new command.
 * @author Michael Angstadt
 */
public class LearnCommand implements Command {
	private final List<Command> hardcodedCommands;
	private final LearnedCommands learnedCommands;

	public LearnCommand(List<Command> hardcodedCommands, LearnedCommands learnedCommands) {
		this.hardcodedCommands = hardcodedCommands;
		this.learnedCommands = learnedCommands;
	}

	@Override
	public String name() {
		return "learn";
	}

	@Override
	public String description() {
		return "Teaches the bot a new command.";
	}

	@Override
	public String helpText(String trigger) {
		return description() + " Syntax: `" + trigger + name() + " commandName output`";
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, boolean isAdmin, Bot bot) {
		String split[] = chatCommand.getContent().split("\\s+", 2);
		if (split.length < 2) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("Syntax: ").code(bot.getTrigger() + name() + " commandName output")
			);
			//@formatter:on
		}

		String commandName = split[0];
		if (commandExists(commandName)) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("A command with that name already exists.")
			);
			//@formatter:on
		}

		String commandOutput = split[1];
		learnedCommands.add(commandName, commandOutput);

		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(chatCommand)
			.append("Saved.")
		);
		//@formatter:on
	}

	/**
	 * Determines if a command already exists.
	 * @param commandName the command name
	 * @return true if a command with the given name already exists, false if
	 * not
	 */
	private boolean commandExists(String commandName) {
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

		return learnedCommands.contains(commandName);
	}
}
