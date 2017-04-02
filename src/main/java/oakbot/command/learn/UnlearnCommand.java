package oakbot.command.learn;

import java.util.Arrays;
import java.util.List;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;

/**
 * Removes a learned command.
 * @author Michael Angstadt
 */
public class UnlearnCommand implements Command {
	private final List<Command> hardcodedCommands;
	private final LearnedCommands learnedCommands;

	public UnlearnCommand(List<Command> hardcodedCommands, LearnedCommands learnedCommands) {
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
	public String description() {
		return "Makes the bot forget a learned command.";
	}

	@Override
	public String helpText(String trigger) {
		return description() + " Syntax: `" + trigger + name() + " commandName`";
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String commandName = chatCommand.getContent().trim();
		if (commandName.isEmpty()) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("Syntax: ").code(context.getTrigger() + name() + " commandName")
			);
			//@formatter:on
		}

		if (hardcodedCommandExists(commandName)) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("You can only unlearn commands that were added with the \"learn\" command. :P")
			);
			//@formatter:on
		}

		boolean removed = learnedCommands.remove(commandName);
		if (!removed) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("That command does not exist.")
			);
			//@formatter:on
		}

		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(chatCommand)
			.append("Command forgotten.")
		);
		//@formatter:on
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
