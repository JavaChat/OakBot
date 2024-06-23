package oakbot.discord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Displays help information on each chat command.
 * @author Michael Angstadt
 */
public class HelpCommand implements DiscordCommand {
	private final List<DiscordCommand> commands;
	private final List<DiscordListener> listeners;

	public HelpCommand(List<DiscordCommand> commands, List<DiscordListener> listeners) {
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

		var commandSummaries = getCommandSummaries();
		var listenerDescriptions = getListenerSummaries();

		var longestNameLength = longestNameLength(context.trigger(), commandSummaries, listenerDescriptions);
		final var bufferSpace = 2;

		//build message
		var cb = new ChatBuilder();
		cb.append("```");
		if (!commandSummaries.isEmpty()) {
			cb.append("Commands=====================").nl();
			for (var entry : commandSummaries.entries()) {
				var name = entry.getKey();
				var description = entry.getValue();

				cb.append(context.trigger()).append(name);
				cb.repeat(' ', longestNameLength - (context.trigger().length() + name.length()) + bufferSpace);
				cb.append(description).nl();
			}
			cb.nl();
		}

		if (!listenerDescriptions.isEmpty()) {
			cb.append("Listeners====================").nl();
			for (var entry : listenerDescriptions.entries()) {
				var name = entry.getKey();
				var description = entry.getValue();

				cb.append(name);
				cb.repeat(' ', longestNameLength - name.length() + bufferSpace);
				cb.append(description).nl();
			}
			cb.nl();
		}
		cb.append("```");
		
		event.getMessage().reply(cb).queue();
	}

	private static int longestNameLength(String trigger, Multimap<String, String> commandSummaries, Multimap<String, String> listenerDescriptions) {
		var longestCommandNameLength = longestString(commandSummaries.keySet()) + trigger.length();
		var longestListenerNameLength = longestString(listenerDescriptions.keySet());

		return Math.max(longestCommandNameLength, longestListenerNameLength);
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
