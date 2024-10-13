package oakbot.command;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.List;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.JoinRoom;

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
		return List.of("join");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Makes the bot join another room.")
			.detail("Only room owners can make the bot join a room.")
			.example("139", "Makes the bot join the room with ID 139.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContent().trim();

		var maxRooms = bot.getMaxRooms();
		if (maxRooms != null && bot.getRooms().size() >= maxRooms) {
			return reply("I can't join any more rooms, I've reached my limit (" + maxRooms + ").", chatCommand);
		}

		int roomToJoin;
		try {
			roomToJoin = Integer.parseInt(content);
		} catch (NumberFormatException e) {
			return reply("Please specify the room ID.", chatCommand);
		}

		if (roomToJoin < 1) {
			return reply("Invalid room ID.", chatCommand);
		}

		if (roomToJoin == chatCommand.getMessage().getRoomId()) {
			return reply("That's the ID for this room... -_-", chatCommand);
		}

		if (bot.getRooms().contains(roomToJoin)) {
			return reply("I'm already there... -_-", chatCommand);
		}

		var userId = chatCommand.getMessage().getUserId();
		var authorIsAdmin = bot.isAdminUser(userId);
		if (!authorIsAdmin) {
			boolean authorIsOwnerOfRoomToJoin;
			try {
				authorIsOwnerOfRoomToJoin = bot.isRoomOwner(roomToJoin, userId);
			} catch (IOException e) {
				return error("Unable to join. Error determining whether you are a room owner: ", e, chatCommand);
			}

			if (!authorIsOwnerOfRoomToJoin) {
				return reply("Only room owners can invite me to rooms.", chatCommand);
			}
		}

		//@formatter:off
		return ChatActions.create(
			new JoinRoom(roomToJoin)
			.onSuccess(() -> reply("Joined.", chatCommand))
			.ifRoomDoesNotExist(() -> reply("That room doesn't exist...", chatCommand))
			.ifLackingPermissionToPost(() -> reply("I don't seem to have permission to post there.", chatCommand))
			.onError(thrown -> reply("I can't seem to join that room: " + thrown.getMessage(), chatCommand))
		);
		//@formatter:on
	}
}
