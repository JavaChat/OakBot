package oakbot.command;

import java.io.IOException;

import oakbot.bot.Bot;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
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
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin, Bot bot) {
		String content = message.getContent().trim();

		int roomToJoin;
		try {
			roomToJoin = Integer.parseInt(content);
		} catch (NumberFormatException e) {
			return reply("Please specify the room ID.", message);
		}

		if (bot.getRooms().contains(roomToJoin)) {
			return reply("I'm already there... -_-", message);
		}

		try {
			bot.join(roomToJoin);
		} catch (IOException e) {
			return reply("Sorry, couldn't join that room.", message);
		}

		return reply("Joined.", message);
	}

	private static ChatResponse reply(String content, ChatMessage message) {
		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(message)
			.append(content)
		);
		//@formatter:on
	}
}
