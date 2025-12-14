package oakbot.command;

import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;

import java.util.List;
import java.util.regex.Pattern;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.DeleteMessage;
import oakbot.bot.IBot;
import oakbot.listener.CatchAllMentionListener;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;

/**
 * Deletes a message the bot has posted.
 * @author Michael Angstadt
 */
public class DeleteCommand implements Command, Listener {
	private static final Pattern permalinkRegex = Pattern.compile("/transcript/message/(\\d+)");

	private final CatchAllMentionListener catchAllListener;

	public DeleteCommand(CatchAllMentionListener catchAllListener) {
		this.catchAllListener = catchAllListener;
	}

	@Override
	public String name() {
		return "delete";
	}

	@Override
	public List<String> aliases() {
		return List.of("del", "rm");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((Command)this)
			.summary("Deletes a message the has bot posted.")
			.detail("Only messages posted within the last 2 minutes can be deleted. Messages can also be deleted by replying to the message you want to delete with this command's name or one of its aliases as the message content.")
			.example("123456", "Deletes the message with ID 123456.")
			.example("https://chat.stackexchange.com/transcript/message/123456#123456", "Message permalinks can also be passed into this command, making it easier to delete a message quickly.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContent();
		var messageToDelete = parseMessageId(content);
		if (messageToDelete == 0) {
			return reply("Message ID or permalink is required.", chatCommand);
		}

		var messageToReplyToOnError = chatCommand.getMessage().id();

		return deleteAction(messageToDelete, messageToReplyToOnError);
	}

	private long parseMessageId(String content) {
		try {
			return Long.parseLong(content);
		} catch (NumberFormatException e) {
			var m = permalinkRegex.matcher(content);
			return m.find() ? Long.parseLong(m.group(1)) : 0;
		}
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		//is the message a reply?
		var messageToDelete = message.parentMessageId();
		if (messageToDelete == 0) {
			return doNothing();
		}

		//is it replying to a bot message?
		var content = message.content().getContent();
		var mention = "@" + bot.getUsername().replace(" ", "");
		if (!content.startsWith(mention)) {
			return doNothing();
		}

		//does the reply consist of the command name or alias?
		var pos = content.indexOf(' ');
		if (pos < 0) {
			return doNothing();
		}
		var afterMention = content.substring(pos + 1).trim();
		if (!name().equals(afterMention) && !aliases().contains(afterMention)) {
			return doNothing();
		}

		if (catchAllListener != null) {
			catchAllListener.ignoreNextMessage();
		}

		var messageToReplyToOnError = message.id();
		return deleteAction(messageToDelete, messageToReplyToOnError);
	}

	private ChatActions deleteAction(long messageToDelete, long messageToReplyToOnError) {
		//@formatter:off
		return create(new DeleteMessage(messageToDelete).onError(e ->
			reply(new ChatBuilder()
				.append("Unable to delete message: ")
				.code(e.getMessage()), messageToReplyToOnError
			)
		));
		//@formatter:on
	}
}
