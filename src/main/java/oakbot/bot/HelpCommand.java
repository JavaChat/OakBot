package oakbot.bot;

import java.util.List;
import java.util.Map;

import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Displays help information on each chat command.
 * @author Michael Angstadt
 */
public class HelpCommand implements Command {
	private final List<Command> commands;

	public HelpCommand(List<Command> commands) {
		this.commands = commands;
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
	public String onMessage(ChatMessage message, boolean isAdmin) {
		String commandText = message.getContent();
		if (commandText != null) {
			ChatBuilder cb = new ChatBuilder();
			cb.reply(message);
			for (Command command : commands) {
				if (!command.name().equals(commandText)) {
					continue;
				}

				if (cb.length() > 0) {
					cb.nl().nl();
				}
				cb.code(command.name()).append(": ").append(command.helpText());
			}
			if (cb.length() == 0) {
				cb.append("No command exists with that name.");
			}
			return cb.toString();
		}

		//TODO split help message up into multiple messages if necessary
		//build each line of the reply and keep them sorted alphabetically
		Multimap<String, String> lines = TreeMultimap.create();
		for (Command command : commands) {
			lines.put(command.name(), command.description());
		}

		//get the length of the longest command name
		int longestName = 0;
		for (String key : lines.keySet()) {
			int length = key.length();
			if (length > longestName) {
				longestName = length;
			}
		}

		//build message
		ChatBuilder cb = new ChatBuilder();
		cb.fixed().append("OakBot Command List").nl();
		for (Map.Entry<String, String> entry : lines.entries()) {
			String name = entry.getKey();
			String description = entry.getValue();

			cb.fixed().append(name);
			for (int i = name.length(); i < longestName + 2; i++) {
				cb.append(' ');
			}
			cb.append(description).nl();
		}

		return cb.toString();
	}
}
