package oakbot.command.learn;

import static oakbot.command.Command.reply;

import java.util.List;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;

/**
 * Teaches the bot a new command.
 * @author Michael Angstadt
 */
public class LearnCommand implements Command {
	private final List<Command> hardcodedCommands;
	private final LearnedCommands learnedCommands;
	private final String invalidCommandNameChars = "*_-`[]()\\";

	public LearnCommand(List<Command> hardcodedCommands, LearnedCommands learnedCommands) {
		this.hardcodedCommands = hardcodedCommands;
		this.learnedCommands = learnedCommands;
	}

	@Override
	public String name() {
		return "learn";
	}

	@Override
	public String description() {
		return "Teaches the bot a new command.";
	}

	@Override
	public String helpText(String trigger) {
		return description() + " Syntax: `" + trigger + name() + " commandName output`";
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		ChatMessage message = chatCommand.getMessage();
		String content = chatCommand.getContent();

		ChatMessage subMessage = new ChatMessage.Builder(message).content(content, message.isFixedFont()).build();
		ChatCommand subCommand = ChatCommand.fromMessage(subMessage, null);
		if (subCommand == null) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("Syntax: ").code().append(context.getTrigger()).append(name()).append(" NAME OUTPUT").code()
			);
			//@formatter:on
		}

		String commandName = subCommand.getCommandName();
		if (!commandNameValid(commandName)) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < invalidCommandNameChars.length(); i++) {
				char c = invalidCommandNameChars.charAt(i);
				if (c != '\\' && c != '-') {
					sb.append('\\');
				}
				sb.append(c).append(' ');
			}
			return reply("Tricksy hobbitses. Command names can't contain these characters: " + sb, chatCommand);
		}

		if (commandExists(commandName)) {
			return reply("A command with that name already exists.", chatCommand);
		}

		String commandOutput = ChatBuilder.toMarkdown(subCommand.getContent(), subMessage.isFixedFont());
		learnedCommands.add(commandName, commandOutput);

		return reply("Saved.", chatCommand);
	}

	/**
	 * Determines if a command already exists.
	 * @param commandName the command name
	 * @return true if a command with the given name already exists, false if
	 * not
	 */
	private boolean commandExists(String commandName) {
		for (Command command : hardcodedCommands) {
			if (commandName.equalsIgnoreCase(command.name())) {
				return true;
			}
			for (String alias : command.aliases()) {
				if (commandName.equalsIgnoreCase(alias)) {
					return true;
				}
			}
		}

		return learnedCommands.contains(commandName);
	}

	/**
	 * Determines if a command name is valid.
	 * @param commandName the command name
	 * @return true if it's valid, false if not
	 */
	private boolean commandNameValid(String commandName) {
		for (int i = 0; i < invalidCommandNameChars.length(); i++) {
			char c = invalidCommandNameChars.charAt(i);
			if (commandName.indexOf(c) >= 0) {
				return false;
			}
		}
		return true;
	}
}
