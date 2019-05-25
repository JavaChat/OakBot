package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.chat.ChatMessage;
import oakbot.command.Command;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommandsDao;

/**
 * Listens for commands (e.g. "/wiki Java").
 * @author Michael Angstadt
 */
public class CommandListener implements Listener {
	private final List<Command> commands;
	private final LearnedCommandsDao learnedCommands;
	private final BiFunction<ChatCommand, BotContext, ChatActions> onUnrecognizedCommand;

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
	public CommandListener(List<Command> commands, LearnedCommandsDao learnedCommands, BiFunction<ChatCommand, BotContext, ChatActions> onUnrecognizedCommand) {
		this.commands = commands;
		this.learnedCommands = learnedCommands;
		this.onUnrecognizedCommand = onUnrecognizedCommand;
	}

	/**
	 * Gets all commands that share a name or alias with at least one other
	 * command (case insensitive).
	 * @return the commands with shared names (key = command name, value =
	 * command classes that share that name)
	 */
	public Multimap<String, Command> checkForDuplicateNames() {
		List<Command> allCommands = new ArrayList<>();
		allCommands.addAll(this.commands);
		allCommands.addAll(learnedCommands.getCommands());

		Multimap<String, Command> byName = ArrayListMultimap.create();
		for (Command command : allCommands) {
			byName.put(command.name().toLowerCase(), command);
			for (String alias : command.aliases()) {
				byName.put(alias.toLowerCase(), command);
			}
		}

		Multimap<String, Command> duplicates = ArrayListMultimap.create();

		byName.asMap().entrySet().stream() //@formatter:off
			.filter(entry -> entry.getValue().size() > 1)
		.forEach(entry -> duplicates.putAll(entry.getKey(), entry.getValue())); //@formatter:on

		return duplicates;
	}

	@Override
	public ChatActions onMessage(ChatMessage message, BotContext context) {
		ChatCommand chatCommand = ChatCommand.fromMessage(message, context.getTrigger());
		if (chatCommand == null) {
			return doNothing();
		}

		List<Command> matchingCommands = getCommands(chatCommand.getCommandName());
		if (matchingCommands.isEmpty()) {
			return (onUnrecognizedCommand == null) ? doNothing() : onUnrecognizedCommand.apply(chatCommand, context);
		}

		ChatActions actions = new ChatActions();
		for (Command command : matchingCommands) {
			actions.addAll(command.onMessage(chatCommand, context));
		}

		return actions;
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
