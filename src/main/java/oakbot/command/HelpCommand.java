package oakbot.command;

import static com.google.common.base.Strings.repeat;
import static oakbot.bot.ChatActions.reply;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.PostMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommandsDao;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;

/**
 * Displays help information on each chat command.
 * @author Michael Angstadt
 */
public class HelpCommand implements Command {
	private final List<Command> commands;
	private final LearnedCommandsDao learnedCommands;
	private final List<Listener> listeners;

	public HelpCommand(List<Command> commands, LearnedCommandsDao learnedCommands, List<Listener> listeners) {
		this.commands = commands;
		this.learnedCommands = learnedCommands;
		this.listeners = listeners;
	}

	@Override
	public String name() {
		return "help";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays this help message.")
			.detail("Displays the list of available commands, as well as detailed information about specific commands.")
			.includeSummaryWithDetail(false)
			.example("", "Displays the list of available commands")
			.example("jaba", "Displays the help documentation for a command called \"jaba\".")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, BotContext context) {
		if (!chatCommand.getContent().isEmpty()) {
			return showHelpText(chatCommand, context.getTrigger());
		}

		Multimap<String, String> commandSummaries = getCommandSummaries();
		Multimap<String, String> listenerDescriptions = getListenerSummaries();

		int longestNameLength;
		{
			Collection<String> allNames = new ArrayList<>(commandSummaries.size() + listenerDescriptions.size());
			allNames.addAll(commandSummaries.keySet());
			allNames.addAll(listenerDescriptions.keySet());
			longestNameLength = longestStringLength(allNames);
		}

		//build message
		ChatBuilder cb = new ChatBuilder();
		if (!commandSummaries.isEmpty()) {
			cb.fixed().append("Commands=====================").nl();
			for (Map.Entry<String, String> entry : commandSummaries.entries()) {
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

		//@formatter:off
		return ChatActions.create(
			new PostMessage(cb)
			.splitStrategy(SplitStrategy.NEWLINE)
			.bypassFilters(true)
			.condensedMessage("My commands are also listed here: https://github.com/JavaChat/OakBot/wiki/Commands")
		);
		//@formatter:on
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

	private Multimap<String, String> getCommandSummaries() {
		Multimap<String, String> summaries = TreeMultimap.create();
		for (Command command : commands) {
			String name = command.name();
			if (name == null) {
				continue;
			}

			summaries.put(name, command.help().getSummary());
		}
		return summaries;
	}

	private List<String> getLearnedCommandNames() {
		List<String> names = new ArrayList<>();
		for (LearnedCommand command : learnedCommands) {
			names.add(command.name());
		}
		return names;
	}

	private Multimap<String, String> getListenerSummaries() {
		Multimap<String, String> summaries = TreeMultimap.create();
		for (Listener listener : listeners) {
			String name = listener.name();
			if (name == null) {
				continue;
			}

			summaries.put(name, listener.help().getSummary());
		}
		return summaries;
	}

	private ChatActions showHelpText(ChatCommand message, String trigger) {
		String commandName = message.getContent();
		List<String> helpTexts = new ArrayList<>();

		commands.stream() //@formatter:off
			.filter(c -> c.name() != null)
			.filter(c -> c.name().equalsIgnoreCase(commandName) || c.aliases().stream().anyMatch(a -> a.equalsIgnoreCase(commandName)))
			.map(c -> c.help().getHelpText(trigger))
		.forEach(helpTexts::add);

		StreamSupport.stream(learnedCommands.spliterator(), false)
			.filter(c -> c.name().equalsIgnoreCase(commandName))
			.map(c -> c.help().getHelpText(trigger))
		.forEach(helpTexts::add);

		listeners.stream()
			.filter(l -> l.name() != null)
			.filter(l -> l.name().equalsIgnoreCase(commandName))
			.map(l -> l.help().getHelpText(trigger))
		.forEach(helpTexts::add); //@formatter:on

		if (helpTexts.isEmpty()) {
			return reply("No command or listener exists with that name.", message);
		}

		ChatBuilder cb = new ChatBuilder();
		cb.reply(message);
		for (String helpText : helpTexts) {
			cb.append(helpText).nl();
		}

		//@formatter:off
		return ChatActions.create(
			new PostMessage(cb.toString().trim()).splitStrategy(SplitStrategy.NEWLINE)
		);
		//@formatter:on
	}
}
