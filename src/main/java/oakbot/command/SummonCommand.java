package oakbot.command;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
	public List<String> aliases() {
		return Arrays.asList("join");
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
			return reply("Hmm, I couldn't join that room.", chatCommand);
		}

		String reply = random("I love making new friends! <3", "Joining!", "I hope they like me!");
		return reply(reply, chatCommand);
	}

	private static ChatResponse reply(String content, ChatCommand message) {
		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(message)
			.append(content)
		);
		//@formatter:on
	}

	private static String random(String... choices) {
		Random random = new Random();
		int index = random.nextInt(choices.length);
		return choices[index];
	}
}
