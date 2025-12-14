package oakbot.command;

import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.reply;

import java.util.List;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.LeaveRoom;
import oakbot.bot.PostMessage;
import oakbot.util.ChatBuilder;
import oakbot.util.Rng;

/**
 * Makes the bot leave a room.
 * @author Michael Angstadt
 */
public class UnsummonCommand implements Command {
	@Override
	public String name() {
		return "unsummon";
	}

	@Override
	public List<String> aliases() {
		return List.of("leave");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Makes the bot leave a room.")
			.detail("Rooms which are designated as \"home\" rooms cannot be left.")
			.example("", "Makes the bot leave the current room.")
			.example("139", "Makes the bot leave the room with ID 139.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContent().trim();

		int roomToLeave;
		boolean inRoomToLeave;
		if (content.isEmpty()) {
			roomToLeave = chatCommand.getMessage().roomId();
			inRoomToLeave = true;
		} else {
			try {
				roomToLeave = Integer.parseInt(content);
			} catch (NumberFormatException e) {
				return reply("Please specify the room ID.", chatCommand);
			}
			inRoomToLeave = (roomToLeave == chatCommand.getMessage().roomId());
		}

		if (!bot.getRooms().contains(roomToLeave)) {
			return reply("I'm not in that room... -_-", chatCommand);
		}

		if (bot.getHomeRooms().contains(roomToLeave)) {
			if (inRoomToLeave) {
				return reply("This is one of my home rooms, I can't leave it.", chatCommand);
			}
			return reply("That's one of my home rooms, I can't leave it.", chatCommand);
		}

		String reply;
		if (inRoomToLeave) {
			reply = Rng.random("*poof*", "Hasta la vista, baby.", "Bye.");
		} else {
			reply = Rng.random("They smelled funny anyway.", "Less for me to worry about.");
		}

		//@formatter:off
		return create(
			new PostMessage(new ChatBuilder().append(reply)).parentId(chatCommand.getMessage().id()),
			new LeaveRoom(roomToLeave)
		);
		//@formatter:on
	}
}
