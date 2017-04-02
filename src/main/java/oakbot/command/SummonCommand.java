package oakbot.command;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import oakbot.bot.BotContext;
import oakbot.bot.BotContext.JoinRoomEvent;
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
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String content = chatCommand.getContent().trim();

		int roomToJoin;
		try {
			roomToJoin = Integer.parseInt(content);
		} catch (NumberFormatException e) {
			return reply("Please specify the room ID.", chatCommand);
		}

		if (roomToJoin == chatCommand.getMessage().getRoomId()) {
			return reply("That's the ID for this room... -_-", chatCommand);
		}

		if (context.getCurrentRooms().contains(roomToJoin)) {
			return reply("I'm already there... -_-", chatCommand);
		}

		context.joinRoom(new JoinRoomEvent(roomToJoin) {
			@Override
			public ChatResponse success() {
				return reply("Joined.", chatCommand);
			}

			@Override
			public ChatResponse ifRoomDoesNotExist() {
				return reply("That room doesn't exist...", chatCommand);
			}

			@Override
			public ChatResponse ifBotDoesNotHavePermission() {
				return reply("I don't seem to have permission to post there.", chatCommand);
			}

			@Override
			public ChatResponse ifOther(IOException thrown) {
				return reply("Hmm, I can't seem to join that room: " + thrown.getMessage(), chatCommand);
			}
		});

		return null;
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
