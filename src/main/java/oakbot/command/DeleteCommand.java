package oakbot.command;

import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.reply;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.DeleteMessage;
import oakbot.bot.IBot;
import oakbot.util.ChatBuilder;

/**
 * Deletes a message the bot has posted.
 * @author Michael Angstadt
 */
public class DeleteCommand implements Command {
	private static final Pattern permalinkRegex = Pattern.compile("/transcript/message/(\\d+)");

	@Override
	public String name() {
		return "delete";
	}

	@Override
	public List<String> aliases() {
		return Arrays.asList("del", "rm");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Deletes a message the has bot posted.")
			.detail("Only messages posted within the last 2 minutes can be deleted.")
			.example("123456", "Deletes the message with ID 123456.")
			.example("https://chat.stackexchange.com/transcript/message/123456#123456", "Message permalinks can also be passed into this command, making it easier to delete a message quickly.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String content = chatCommand.getContent();

		long messageId;
		try {
			messageId = Long.parseLong(content);
		} catch (NumberFormatException e) {
			Matcher m = permalinkRegex.matcher(content);
			if (!m.find()) {
				return reply("Message ID or permalink is required.", chatCommand);
			}

			messageId = Long.parseLong(m.group(1));
		}

		return create(new DeleteMessage(messageId).onError(e -> {
			//@formatter:off
			ChatBuilder cb = new ChatBuilder()
				.append("Unable to delete message: ")
				.code(e.getMessage());
			//@formatter:on

			return reply(cb, chatCommand);
		}));
	}
}
