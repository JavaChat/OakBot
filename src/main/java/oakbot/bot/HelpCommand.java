package oakbot.bot;

import static oakbot.util.ChatUtils.reply;

import java.util.List;
import java.util.Map;

import oakbot.chat.ChatMessage;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
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
			StringBuilder sb = new StringBuilder();
			for (Command command : commands) {
				if (!command.name().equals(commandText)) {
					continue;
				}

				String text = "`" + command.name() + ":` " + command.helpText();
				if (sb.length() == 0) {
					sb.append(reply(message, text));
				} else {
					sb.append("\n\n").append(text);
				}
			}
			if (sb.length() == 0) {
				return reply(message, "No command exists with that name.");
			}
			return sb.toString();
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
		StringBuilder sb = new StringBuilder();
		sb.append("    OakBot Command List\n");
		for (Map.Entry<String, String> entry : lines.entries()) {
			String name = entry.getKey();
			String description = entry.getValue();

			sb.append("    ").append(name);
			for (int i = name.length(); i < longestName + 2; i++) {
				sb.append(' ');
			}
			sb.append(description).append("\n");
		}

		return sb.toString();
	}
}
