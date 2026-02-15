package oakbot.discord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Displays help information on each chat command.
 * @author Michael Angstadt
 */
public class HelpCommand implements DiscordCommand {
	private final List<DiscordCommand> commands;
	private final List<DiscordListener> listeners;
	private final List<DiscordSlashCommand> slashCommands;

	public HelpCommand(List<DiscordSlashCommand> slashCommands, List<DiscordCommand> commands, List<DiscordListener> listeners) {
		this.slashCommands = slashCommands;
		this.commands = commands;
		this.listeners = listeners;
	}

	@Override
	public String name() {
		return "help";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new DiscordHelpDoc.Builder(this)
			.summary("Displays this help message.")
			.detail("Displays the list of available commands, as well as detailed information about specific commands.")
			.includeSummaryWithDetail(false)
			.example("", "Displays the list of available commands")
			.example("cat", "Displays the help documentation for a command called \"cat\".")
		.build();
		//@formatter:on
	}

	@Override
	public void onMessage(String content, MessageReceivedEvent event, BotContext context) {
		if (!content.isEmpty()) {
			showHelpText(content, event, context.trigger());
			return;
		}

		var slashCommandSummaries = getSlashCommandSummaries();
		var commandSummaries = getCommandSummaries();
		var listenerDescriptions = getListenerSummaries();

		var longestNameLength = longestNameLength(context.trigger(), slashCommandSummaries, commandSummaries, listenerDescriptions);
		final var bufferSpace = 2;

		//build message
		var cb = new ChatBuilder();

		if (!slashCommandSummaries.isEmpty()) {
			var slash = "/";
			cb.bold("Slash commands").nl();
			cb.codeBlock();
			slashCommandSummaries.forEach((name, description) -> {
				cb.append(slash).append(name);
				cb.repeat(' ', longestNameLength - (slash.length() + name.length()) + bufferSpace);
				cb.append(description).nl();
			});
			cb.codeBlock();
		}

		if (!commandSummaries.isEmpty()) {
			cb.bold("Commands").nl();
			cb.codeBlock();
			commandSummaries.forEach((name, description) -> {
				cb.append(context.trigger()).append(name);
				cb.repeat(' ', longestNameLength - (context.trigger().length() + name.length()) + bufferSpace);
				cb.append(description).nl();
			});
			cb.codeBlock();
		}

		if (!listenerDescriptions.isEmpty()) {
			cb.bold("Listeners").nl();
			cb.codeBlock();
			listenerDescriptions.forEach((name, description) -> {
				cb.append(name);
				cb.repeat(' ', longestNameLength - name.length() + bufferSpace);
				cb.append(description).nl();
			});
			cb.codeBlock();
		}

		event.getMessage().reply(cb).queue();
	}

	private static int longestNameLength(String trigger, Multimap<String, String> slashCommandSummaries, Multimap<String, String> commandSummaries, Multimap<String, String> listenerDescriptions) {
		var longestSlashCommandNameLength = longestString(slashCommandSummaries.keySet()) + "/".length();
		var longestCommandNameLength = longestString(commandSummaries.keySet()) + trigger.length();
		var longestListenerNameLength = longestString(listenerDescriptions.keySet());

		var m = Math.max(longestSlashCommandNameLength, longestCommandNameLength);
		return Math.max(m, longestListenerNameLength);
	}

	private static int longestString(Collection<String> c) {
		//@formatter:off
		return c.stream()
			.mapToInt(String::length)
		.max().orElse(0);
		//@formatter:on
	}

	private Multimap<String, String> getSlashCommandSummaries() {
		Multimap<String, String> summaries = TreeMultimap.create();

		//@formatter:off
		slashCommands.stream()
			.map(DiscordSlashCommand::data)
		.forEach(data -> {
			var name = data.getName();
			var description = data.getDescription();
			summaries.put(name, description);
		});
		//@formatter:on

		return summaries;
	}

	private Multimap<String, String> getCommandSummaries() {
		Multimap<String, String> summaries = TreeMultimap.create();

		//@formatter:off
		commands.stream()
			.filter(c -> c.name() != null)
			.filter(c -> c.help() != null)
		.forEach(c -> summaries.put(c.name(), c.help().getSummary()));
		//@formatter:on

		return summaries;
	}

	private Multimap<String, String> getListenerSummaries() {
		Multimap<String, String> summaries = TreeMultimap.create();

		//@formatter:off
		listeners.stream()
			.filter(l -> l.name() != null)
			
			/*
			 * If a command exists with the same name, do not include this
			 * listener in the help output.
			 */
			.filter(l -> commands.stream().noneMatch(c -> c.name().equalsIgnoreCase(l.name())))
			
			.filter(l -> l.help() != null)
		.forEach(l -> summaries.put(l.name(), l.help().getSummary()));
		//@formatter:on

		return summaries;
	}

	private void showHelpText(String name, MessageReceivedEvent event, String trigger) {
		/*
		 * Remove duplicate help messages, as classes implement multiple
		 * interfaces.
		 */
		var helpTexts = new ArrayList<String>();

		//@formatter:off
		slashCommands.stream()
			.map(DiscordSlashCommand::data)
			.filter(data -> data.getName().equalsIgnoreCase(name))
			.map(SlashCommandData::getDescription)
		.forEach(helpTexts::add);

		commands.stream()
			.filter(c -> c.name() != null)
			.filter(c -> c.name().equalsIgnoreCase(name))
			.filter(c -> c.help() != null)
			.map(c -> c.help().getHelpText(trigger))
		.forEach(helpTexts::add);

		listeners.stream()
			.filter(l -> l.name() != null)
			.filter(l -> l.name().equalsIgnoreCase(name))
			.filter(l -> l.help() != null)
			.map(l -> l.help().getHelpText(trigger))
		.forEach(helpTexts::add);
		//@formatter:on

		if (helpTexts.isEmpty()) {
			event.getMessage().reply("No command or listener exists with that name.").queue();
			return;
		}

		var message = helpTexts.stream().collect(Collectors.joining("\n"));
		event.getMessage().reply(message).queue();
	}
}
