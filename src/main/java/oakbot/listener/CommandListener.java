package oakbot.listener;

import java.util.ArrayList;
import java.util.List;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.bot.UnknownCommandHandler;
import oakbot.chat.ChatMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommandsDao;

/**
 * Listens for commands (e.g. "/wiki Java").
 * @author Michael Angstadt
 */
public class CommandListener implements Listener {
	private final List<Command> commands;
	private final LearnedCommandsDao learnedCommands;
	private final UnknownCommandHandler unknownCommandHandler;

	/**
	 * @param commands the commands
	 * @param learnedCommands the learned commands
	 */
	public CommandListener(List<Command> commands, LearnedCommandsDao learnedCommands) {
		this(commands, learnedCommands, null);
	}

	/**
	 * @param commands the commands
	 * @param learnedCommands the learned commands
	 * @param unknownCommandHandler how to respond to unrecognized commands or
	 * null to ignore unrecognized commands
	 */
	public CommandListener(List<Command> commands, LearnedCommandsDao learnedCommands, UnknownCommandHandler unknownCommandHandler) {
		this.commands = commands;
		this.learnedCommands = learnedCommands;
		this.unknownCommandHandler = unknownCommandHandler;
	}

	@Override
	public String name() {
		return null;
	}

	@Override
	public HelpDoc help() {
		return null;
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, BotContext context) {
		ChatCommand chatCommand = ChatCommand.fromMessage(message, context.getTrigger());
		if (chatCommand == null) {
			return null;
		}

		List<Command> matchingCommands = getCommands(chatCommand.getCommandName());
		if (matchingCommands.isEmpty()) {
			return (unknownCommandHandler == null) ? null : unknownCommandHandler.onMessage(chatCommand, context);
		}

		Command command = matchingCommands.get(0); //TODO support multiple ChatResponse objects being returned?
		return command.onMessage(chatCommand, context);
	}

	/**
	 * Gets all commands that have a given name.
	 * @param name the command name
	 * @return the matching commands
	 */
	private List<Command> getCommands(String name) {
		List<Command> result = new ArrayList<>();
		for (Command command : commands) {
			if (command.name().equals(name) || command.aliases().contains(name)) {
				result.add(command);
			}
		}
		for (LearnedCommand command : learnedCommands) {
			if (command.name().equals(name) || command.aliases().contains(name)) {
				result.add(command);
			}
		}
		return result;
	}
}
