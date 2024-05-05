package oakbot.command.learn;

import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;

/**
 * Teaches the bot a new command.
 * @author Michael Angstadt
 */
public class LearnCommand implements Command {
	private static final Logger logger = Logger.getLogger(LearnCommand.class.getName());

	private final List<Command> hardcodedCommands;
	private final LearnedCommandsDao learnedCommands;
	private final Predicate<String> validCommandName = Pattern.compile("^[A-Za-z0-9]+$").asPredicate();

	public LearnCommand(List<Command> hardcodedCommands, LearnedCommandsDao learnedCommands) {
		this.hardcodedCommands = hardcodedCommands;
		this.learnedCommands = learnedCommands;
	}

	@Override
	public String name() {
		return "learn";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Creates a new command.")
			.example("happy :)", "Creates a command called \"happy\" which outputs \":)\" when invoked.")
			.example("complement {0} is awesome!", "Creates a command called \"complement\" which has a parameter.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		//example: "/learn test this is a test"
		var message = chatCommand.getMessage();

		//example: "test this is a test"
		var subMessage = new ChatMessage.Builder(message).content(chatCommand.getContent(), chatCommand.isFixedWidthFont()).build();
		var subCommand = ChatCommand.fromMessage(subMessage, null);
		if (subCommand == null) {
			return reply("You haven't specified the command name or its output.", chatCommand);
		}
		if (subCommand.getContent().trim().isEmpty()) {
			return reply("You haven't specified the command output.", chatCommand);
		}

		var commandName = subCommand.getCommandName();
		if (!commandNameValid(commandName)) {
			return reply("Tricksy hobbitses. Command names can only contain letters (a-z) and numbers.", chatCommand);
		}

		if (commandExists(commandName)) {
			return reply("A command with that name already exists.", chatCommand);
		}

		String commandOutput = null;
		try {
			var plainText = bot.getOriginalMessageContent(message.getMessageId());

			/*
			 * Capture the text that comes before the command name, in case the
			 * user wants to use fixed-width formatting.
			 */
			var p = Pattern.compile("^(.*?)" + Pattern.quote(bot.getTrigger()) + name() + "\\s+" + Pattern.quote(commandName) + "\\s+(.*?)$", Pattern.DOTALL);

			var m = p.matcher(plainText);
			if (m.find()) {
				commandOutput = m.group(1) + m.group(2);
			} else {
				logger.severe(() -> "Could not parse command output from plaintext chat message. Falling back to manually converting the HTML-encoded message to Markdown: " + plainText);
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem querying chat service for original message content. Falling back to manually converting the HTML-encoded message to Markdown.");
		}

		if (commandOutput == null) {
			commandOutput = subCommand.getContentMarkdown();
		}

		//@formatter:off
		learnedCommands.add(new LearnedCommand.Builder()
			.authorUserId(message.getUserId())
			.authorUsername(message.getUsername())
			.roomId(message.getRoomId())
			.messageId(message.getMessageId())
			.created(message.getTimestamp())
			.name(commandName)
			.output(commandOutput)
		.build());
		//@formatter:on

		return reply("Saved.", chatCommand);
	}

	/**
	 * Determines if a command already exists.
	 * @param commandName the command name
	 * @return true if a command with the given name already exists, false if
	 * not
	 */
	private boolean commandExists(String commandName) {
		for (var command : hardcodedCommands) {
			if (commandName.equalsIgnoreCase(command.name())) {
				return true;
			}
			for (var alias : command.aliases()) {
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
		return validCommandName.test(commandName);
	}
}
