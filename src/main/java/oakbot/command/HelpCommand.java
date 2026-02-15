package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.github.mangstadt.sochat4j.SplitStrategy;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommandsDao;
import oakbot.listener.Listener;
import oakbot.task.ScheduledTask;
import oakbot.util.ChatBuilder;

/**
 * Displays help information on each chat command.
 * @author Michael Angstadt
 */
public class HelpCommand implements Command {
	private final List<Command> commands;
	private final LearnedCommandsDao learnedCommands;
	private final List<Listener> listeners;
	private final List<ScheduledTask> tasks;
	private final String helpWebpage;

	public HelpCommand(List<Command> commands, LearnedCommandsDao learnedCommands, List<Listener> listeners, List<ScheduledTask> tasks, String helpWebpage) {
		this.commands = commands;
		this.learnedCommands = learnedCommands;
		this.listeners = listeners;
		this.tasks = tasks;
		this.helpWebpage = helpWebpage;
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
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		if (!chatCommand.getContent().isEmpty()) {
			return showHelpText(chatCommand, bot.getTrigger());
		}

		var commandSummaries = getCommandSummaries();
		var listenerDescriptions = getListenerSummaries();
		var taskDescriptions = getTaskSummaries();

		var longestNameLength = longestNameLength(bot.getTrigger(), commandSummaries, listenerDescriptions, taskDescriptions);
		final var bufferSpace = 2;

		//build message
		var cb = new ChatBuilder();
		cb.fixedWidth();
		if (!commandSummaries.isEmpty()) {
			cb.append("Commands=====================").nl();
			commandSummaries.forEach((name, description) -> {
				cb.append(bot.getTrigger()).append(name);
				cb.repeat(' ', longestNameLength - (bot.getTrigger().length() + name.length()) + bufferSpace);
				cb.append(description).nl();
			});
			cb.nl();
		}

		var learnedCommandNames = getLearnedCommandNames();
		if (!learnedCommandNames.isEmpty()) {
			cb.append("Learned Commands=============").nl();

			//@formatter:off
			cb.append(learnedCommandNames.stream()
				.map(name -> bot.getTrigger() + name)
			.collect(Collectors.joining(", ")));
			//@formatter:on

			cb.nl().nl();
		}

		if (!listenerDescriptions.isEmpty()) {
			cb.append("Listeners====================").nl();
			listenerDescriptions.forEach((name, description) -> {
				cb.append(name);
				cb.repeat(' ', longestNameLength - name.length() + bufferSpace);
				cb.append(description).nl();
			});
			cb.nl();
		}

		if (!taskDescriptions.isEmpty()) {
			cb.append("Tasks========================").nl();
			taskDescriptions.forEach((name, description) -> {
				cb.append(name);
				cb.repeat(' ', longestNameLength - name.length() + bufferSpace);
				cb.append(description).nl();
			});
		}

		String condensedMessage;
		if (helpWebpage == null || helpWebpage.isEmpty()) {
			condensedMessage = "Type " + bot.getTrigger() + name() + " to see my commands.";
		} else {
			condensedMessage = "My commands are also listed here: " + helpWebpage;
		}

		//@formatter:off
		return ChatActions.create(
			new PostMessage(cb)
			.splitStrategy(SplitStrategy.NEWLINE)
			.bypassFilters(true)
			.condensedMessage(condensedMessage)
		);
		//@formatter:on
	}

	private static int longestNameLength(String trigger, Multimap<String, String> commandSummaries, Multimap<String, String> listenerDescriptions, Multimap<String, String> taskDescriptions) {
		var longestCommandNameLength = longestString(commandSummaries.keySet()) + trigger.length();
		var longestListenerNameLength = longestString(listenerDescriptions.keySet());
		var longestTaskNameLength = longestString(taskDescriptions.keySet());

		var m = Math.max(longestCommandNameLength, longestListenerNameLength);
		return Math.max(m, longestTaskNameLength);
	}

	private static int longestString(Collection<String> c) {
		//@formatter:off
		return c.stream()
			.mapToInt(String::length)
		.max().orElse(0);
		//@formatter:on
	}

	private Multimap<String, String> getCommandSummaries() {
		Multimap<String, String> summaries = TreeMultimap.create();

		//@formatter:off
		commands.stream()
			.filter(command -> command.name() != null)
		.forEach(command -> summaries.put(command.name(), command.help().getSummary()));
		//@formatter:on

		return summaries;
	}

	private List<String> getLearnedCommandNames() {
		//@formatter:off
		return learnedCommands.getCommands().stream()
			.map(LearnedCommand::name)
			.sorted(String.CASE_INSENSITIVE_ORDER)
		.toList();
		//@formatter:on
	}

	private Multimap<String, String> getListenerSummaries() {
		Multimap<String, String> summaries = TreeMultimap.create();

		//@formatter:off
		listeners.stream()
			.filter(listener -> listener.name() != null)
			/*
			 * If a command exists with the same name, do not include this
			 * listener in the help output.
			 */
			.filter(listener -> commands.stream().map(Command::name).noneMatch(name -> name.equals(listener.name())))
		.forEach(listener -> summaries.put(listener.name(), listener.help().getSummary()));
		//@formatter:on

		return summaries;
	}

	private Multimap<String, String> getTaskSummaries() {
		Multimap<String, String> summaries = TreeMultimap.create();

		//@formatter:off
		tasks.stream()
			.filter(task -> task.name() != null)
			/*
			 * If a command or listener exists with the same name, do not
			 * include this task in the help output.
			 */
			.filter(task -> commands.stream().map(Command::name).noneMatch(name -> name.equals(task.name())))
			.filter(task -> listeners.stream().map(Listener::name).noneMatch(name -> name.equals(task.name())))
		.forEach(task -> summaries.put(task.name(), task.help().getSummary()));
		//@formatter:on

		return summaries;
	}

	private ChatActions showHelpText(ChatCommand message, String trigger) {
		var commandName = message.getContent();

		/*
		 * Remove duplicate help messages, as classes implement multiple
		 * interfaces.
		 */
		var helpTexts = new HashSet<String>();

		//@formatter:off
		commands.stream()
			.filter(c -> c.name() != null)
			.filter(c -> c.name().equalsIgnoreCase(commandName) || c.aliases().stream().anyMatch(a -> a.equalsIgnoreCase(commandName)))
			.map(c -> c.help().getHelpText(trigger))
		.forEach(helpTexts::add);

		learnedCommands.getCommands().stream()
			.filter(c -> c.name().equalsIgnoreCase(commandName))
			.map(c -> c.help().getHelpText(trigger))
		.forEach(helpTexts::add);

		listeners.stream()
			.filter(l -> l.name() != null)
			.filter(l -> l.name().equalsIgnoreCase(commandName))
			.map(l -> l.help().getHelpText(trigger))
		.forEach(helpTexts::add);

		tasks.stream()
			.filter(t -> t.name() != null)
			.filter(t -> t.name().equalsIgnoreCase(commandName))
			.map(t -> t.help().getHelpText(trigger))
		.forEach(helpTexts::add);
		//@formatter:on

		if (helpTexts.isEmpty()) {
			return reply("No command or listener exists with that name.", message);
		}

		var cb = new ChatBuilder();
		for (var helpText : helpTexts) {
			cb.append(helpText).nl();
		}

		return reply(cb.toString().trim(), message, SplitStrategy.NEWLINE);
	}
}
