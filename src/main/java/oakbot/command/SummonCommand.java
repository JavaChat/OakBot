package oakbot.command;

import java.io.IOException;

import oakbot.bot.Bot;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatBuilder;

/**
 * Makes the bot join another room.
 * @author Michael Angstadt
 */
public class SummonCommand implements Command {
	@Override
	public String name() {
		return "summon";
	}

	@Override
	public String description() {
		return "Makes the bot join another room.";
	}

	@Override
	public String helpText(String trigger) {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, boolean isAdmin, Bot bot) {
		String content = chatCommand.getContent().trim();

		int roomToJoin;
		try {
			roomToJoin = Integer.parseInt(content);
		} catch (NumberFormatException e) {
			return reply("Please specify the room ID.", chatCommand);
		}

		if (bot.getRooms().contains(roomToJoin)) {
			return reply("I'm already there... -_-", chatCommand);
		}

		try {
			bot.join(roomToJoin);
		} catch (IOException e) {
			return reply("Sorry, couldn't join that room.", chatCommand);
		}

		return reply("Joined.", chatCommand);
	}

	private static ChatResponse reply(String content, ChatCommand message) {
		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(message)
			.append(content)
		);
		//@formatter:on
	}
}
