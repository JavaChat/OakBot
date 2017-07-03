package oakbot.command;

import static com.google.common.base.Strings.repeat;
import static oakbot.command.Command.reply;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.SplitStrategy;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommands;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;

/**
 * Displays help information on each chat command.
 * @author Michael Angstadt
 */
public class HelpCommand implements Command {
	private final List<Command> commands;
	private final LearnedCommands learnedCommands;
	private final List<Listener> listeners;

	public HelpCommand(List<Command> commands, LearnedCommands learnedCommands, List<Listener> listeners) {
		this.commands = commands;
		this.learnedCommands = learnedCommands;
		this.listeners = listeners;
	}

	@Override
	public String name() {
		return "help";
	}

	@Override
	public String description() {
		return "Displays this help message.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append("Displays the list of available commands, as well as detailed information about specific commands.").nl()
			.append("Usage: ").append(trigger).append(name()).append(" [COMMAND_NAME]").nl()
			.append("Examples:").nl()
			.append(trigger).append(name()).nl()
			.append(trigger).append(name()).append(" javadoc")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		if (!chatCommand.getContent().isEmpty()) {
			return showHelpText(chatCommand, context.getTrigger());
		}

		Multimap<String, String> commandDescriptions = getCommandDescriptions();
		Multimap<String, String> listenerDescriptions = getListenerDescriptions();

		int longestNameLength;
		{
			Collection<String> allNames = new ArrayList<>(commandDescriptions.size() + listenerDescriptions.size());
			allNames.addAll(commandDescriptions.keySet());
			allNames.addAll(listenerDescriptions.keySet());
			longestNameLength = longestStringLength(allNames);
		}

		//build message
		ChatBuilder cb = new ChatBuilder();
		if (!commandDescriptions.isEmpty()) {
			cb.fixed().append("Commands=====================").nl();
			for (Map.Entry<String, String> entry : commandDescriptions.entries()) {
				String name = entry.getKey();
				String description = entry.getValue();

				cb.fixed().append(context.getTrigger()).append(name);
				cb.append(repeat(" ", longestNameLength - name.length() + 2));
				cb.append(description).nl();
			}
			cb.fixed().nl();
		}

		List<String> learnedCommandNames = getLearnedCommandNames();
		if (!learnedCommandNames.isEmpty()) {
			cb.fixed().append("Learned Commands=============").nl();
			cb.fixed();
			boolean first = true;
			for (String name : learnedCommandNames) {
				if (!first) {
					cb.append(", ");
				}
				cb.append(context.getTrigger()).append(name);
				first = false;
			}
			cb.nl().fixed().nl();
		}

		if (!listenerDescriptions.isEmpty()) {
			cb.fixed().append("Listeners====================").nl();
			for (Map.Entry<String, String> entry : listenerDescriptions.entries()) {
				String name = entry.getKey();
				String description = entry.getValue();

				cb.fixed().append(name);
				cb.append(repeat(" ", longestNameLength - name.length() + 2));
				cb.append(description).nl();
			}
		}

		return new ChatResponse(cb, SplitStrategy.NEWLINE);
	}

	private static int longestStringLength(Collection<String> strings) {
		int longestLength = 0;
		for (String string : strings) {
			int length = string.length();
			if (length > longestLength) {
				longestLength = length;
			}
		}
		return longestLength;
	}

	private Multimap<String, String> getCommandDescriptions() {
		Multimap<String, String> descriptions = TreeMultimap.create();
		for (Command command : commands) {
			String name = command.name();
			if (name == null) {
				continue;
			}

			descriptions.put(name, command.description());
		}
		return descriptions;
	}

	private List<String> getLearnedCommandNames() {
		List<String> names = new ArrayList<>();
		for (LearnedCommand command : learnedCommands) {
			names.add(command.name());
		}
		return names;
	}

	private Multimap<String, String> getListenerDescriptions() {
		Multimap<String, String> descriptions = TreeMultimap.create();
		for (Listener listener : listeners) {
			String name = listener.name();
			if (name == null) {
				continue;
			}

			descriptions.put(name, listener.description());
		}
		return descriptions;
	}

	private ChatResponse showHelpText(ChatCommand message, String trigger) {
		String commandText = message.getContent().toLowerCase();
		List<String> helpTexts = new ArrayList<>();

		for (Command command : commands) {
			String name = command.name();
			if (name == null) {
				continue;
			}

			name = name.toLowerCase();
			if (name.equals(commandText) || command.aliases().contains(commandText)) {
				helpTexts.add(command.helpText(trigger));
			}
		}

		for (LearnedCommand command : learnedCommands) {
			String name = command.name().toLowerCase();
			if (name.equals(commandText)) {
				helpTexts.add(command.helpText(trigger));
			}
		}

		for (Listener listener : listeners) {
			String name = listener.name();
			if (name == null) {
				continue;
			}

			name = name.toLowerCase();
			if (name.equals(commandText)) {
				helpTexts.add(listener.helpText());
			}
		}

		if (helpTexts.isEmpty()) {
			return reply("No command or listener exists with that name.", message);
		}

		ChatBuilder cb = new ChatBuilder();
		cb.reply(message);
		for (String helpText : helpTexts) {
			cb.append(helpText).nl();
		}
		return new ChatResponse(cb.toString().trim(), SplitStrategy.NEWLINE);
	}
}
