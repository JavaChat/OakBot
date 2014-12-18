package oakbot.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Displays help information on each chat command.
 * @author Michael Angstadt
 */
public class HelpCommand implements Command {
	private final List<Command> commands;
	private final List<Listener> listeners;
	private final String trigger;

	public HelpCommand(List<Command> commands, List<Listener> listeners, String trigger) {
		this.commands = commands;
		this.listeners = listeners;
		this.trigger = trigger;
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
	public String helpText() {
		return "Displays the list of available commands, as well as detailed information about specific commands.";
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		String commandText = message.getContent();
		if (!commandText.isEmpty()) {
			ChatBuilder cb = new ChatBuilder();
			cb.reply(message);

			List<String> names = new ArrayList<>(), helpTexts = new ArrayList<>();
			for (Command command : commands) {
				String name = command.name();
				if (name != null && name.equalsIgnoreCase(commandText)) {
					names.add(command.name());
					helpTexts.add(command.helpText());
				}
			}
			for (Listener listener : listeners) {
				String name = listener.name();
				if (name != null && name.equalsIgnoreCase(commandText)) {
					names.add(listener.name());
					helpTexts.add(listener.helpText());
				}
			}

			if (names.isEmpty()) {
				cb.append("No command or listener exists with that name.");
			} else {
				boolean first = true;
				for (int i = 0; i < names.size(); i++) {
					if (first) {
						first = false;
					} else {
						cb.nl();
					}
					cb.code(names.get(i)).append(": ").append(helpTexts.get(i));
				}
			}

			return new ChatResponse(cb.toString(), SplitStrategy.WORD);
		}

		//build each line of the reply and keep them sorted alphabetically
		Multimap<String, String> commandLines = TreeMultimap.create();
		for (Command command : commands) {
			String name = command.name();
			if (name != null) {
				commandLines.put(name, command.description());
			}
		}
		Multimap<String, String> listenerLines = TreeMultimap.create();
		for (Listener listener : listeners) {
			String name = listener.name();
			if (name != null) {
				listenerLines.put(name, listener.description());
			}
		}

		//get the length of the longest command name
		int longestName = 0;
		for (String key : commandLines.keySet()) {
			int length = key.length();
			if (length > longestName) {
				longestName = length;
			}
		}

		//build message
		ChatBuilder cb = new ChatBuilder();
		cb.fixed().append("Commands=====================").nl();
		for (Map.Entry<String, String> entry : commandLines.entries()) {
			String name = entry.getKey();
			String description = entry.getValue();

			cb.fixed().append(trigger).append(name);
			for (int i = name.length(); i < longestName + 2; i++) {
				cb.append(' ');
			}
			cb.append(description).nl();
		}

		cb.fixed().nl();
		cb.fixed().append("Listeners====================").nl();
		for (Map.Entry<String, String> entry : listenerLines.entries()) {
			String name = entry.getKey();
			String description = entry.getValue();

			cb.fixed().append(name);
			for (int i = name.length(); i < longestName + 2; i++) {
				cb.append(' ');
			}
			cb.append(description).nl();
		}

		return new ChatResponse(cb.toString(), SplitStrategy.NEWLINE);
	}
}
