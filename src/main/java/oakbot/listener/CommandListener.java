package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.learn.LearnedCommandsDao;

/**
 * Listens for commands (e.g. "/wiki Java").
 * @author Michael Angstadt
 */
public class CommandListener implements Listener {
	private final List<Command> commands;
	private final LearnedCommandsDao learnedCommands;
	private final BiFunction<ChatCommand, IBot, ChatActions> onUnrecognizedCommand;

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
	public CommandListener(List<Command> commands, LearnedCommandsDao learnedCommands, BiFunction<ChatCommand, IBot, ChatActions> onUnrecognizedCommand) {
		this.commands = commands;
		this.learnedCommands = learnedCommands;
		this.onUnrecognizedCommand = onUnrecognizedCommand;
	}

	/**
	 * Gets all commands that share a name or alias with at least one other
	 * command (case-insensitive).
	 * @return the commands with shared names (key = command name, value =
	 * command classes that share that name)
	 */
	public Multimap<String, Command> checkForDuplicateNames() {
		var allCommands = new ArrayList<Command>();
		allCommands.addAll(this.commands);
		allCommands.addAll(learnedCommands.getCommands());

		Multimap<String, Command> byName = ArrayListMultimap.create();
		for (var command : allCommands) {
			byName.put(command.name().toLowerCase(), command);

			//@formatter:off
			command.aliases().stream()
				.map(String::toLowerCase)
			.forEach(alias -> byName.put(alias, command));
			//@formatter:on
		}

		Multimap<String, Command> duplicates = ArrayListMultimap.create();

		//@formatter:off
		byName.asMap().entrySet().stream() 
			.filter(entry -> entry.getValue().size() > 1)
		.forEach(entry -> duplicates.putAll(entry.getKey(), entry.getValue()));
		//@formatter:on

		return duplicates;
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		var chatCommand = ChatCommand.fromMessage(message, bot.getTrigger());
		if (chatCommand == null) {
			return doNothing();
		}

		var matchingCommands = getCommands(chatCommand.getCommandName());
		if (matchingCommands.isEmpty()) {
			return (onUnrecognizedCommand == null) ? doNothing() : onUnrecognizedCommand.apply(chatCommand, bot);
		}

		if (matchingCommands.size() == 1) {
			return matchingCommands.get(0).onMessage(chatCommand, bot);
		}

		var actions = new ChatActions();

		//@formatter:off
		matchingCommands.stream()
			.map(command -> command.onMessage(chatCommand, bot))
		.forEach(actions::addAll);
		//@formatter:on

		return actions;
	}

	/**
	 * Gets all commands that have a given name.
	 * @param name the command name
	 * @return the matching commands
	 */
	private List<Command> getCommands(String name) {
		//@formatter:off
		return Stream.concat(commands.stream(), learnedCommands.getCommands().stream())
			.filter(command -> command.name().equals(name) || command.aliases().contains(name))
		.toList();
		//@formatter:on
	}
}
