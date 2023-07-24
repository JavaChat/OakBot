package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.JoinRoom;

/**
 * Makes the bot join another room.
 * @author Michael Angstadt
 */
public class SummonCommand implements Command {
	private final int minSummonsRequired;
	private final Duration summonTime = Duration.ofMinutes(2);
	private final Map<Integer, Pending> pendingSummons = new HashMap<>();

	/**
	 * @param minSummonsRequired the minimum number of users that have to summon
	 * the bot to a room before the bot actually joins the room
	 */
	public SummonCommand(int minSummonsRequired) {
		this.minSummonsRequired = minSummonsRequired;
	}

	@Override
	public String name() {
		return "summon";
	}

	@Override
	public List<String> aliases() {
		return Arrays.asList("join");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Makes the bot join another room.")
			.detail("At least " + minSummonsRequired + " user(s) are needed to summon the bot to a room.")
			.example("139", "Makes the bot join the room with ID 139.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String content = chatCommand.getContent().trim();

		Integer maxRooms = bot.getMaxRooms();
		if (maxRooms != null && bot.getRooms().size() >= maxRooms) {
			return reply("I can't join anymore rooms, I've reached my limit (" + maxRooms + ").", chatCommand);
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

		boolean authorIsAdmin = bot.getAdminUsers().contains(chatCommand.getMessage().getUserId());
		if (!authorIsAdmin) {
			Pending pending = pendingSummons.get(roomToJoin);
			if (pending == null) {
				pending = new Pending(roomToJoin);
				pendingSummons.put(roomToJoin, pending);
			} else {
				Duration elapsed = Duration.between(pending.getStarted(), Instant.now());
				if (elapsed.compareTo(summonTime) > 0) {
					pending = new Pending(roomToJoin);
					pendingSummons.put(roomToJoin, pending);
				}
			}

			int userId = chatCommand.getMessage().getUserId();
			boolean alreadyVoted = !pending.getUserIds().add(userId);
			int votesNeeded = minSummonsRequired - pending.getUserIds().size();
			if (alreadyVoted) {
				if (votesNeeded == 1) {
					return reply("I need a vote from " + votesNeeded + " other person.", chatCommand);
				} else {
					return reply("I need votes from " + votesNeeded + " other people.", chatCommand);
				}
			}

			if (votesNeeded > 0) {
				if (votesNeeded == 1) {
					return reply(votesNeeded + " more vote needed.", chatCommand);
				} else {
					return reply(votesNeeded + " more votes needed.", chatCommand);
				}
			}
		}

		pendingSummons.remove(roomToJoin);

		//@formatter:off
		return ChatActions.create(
			new JoinRoom(roomToJoin)
			.onSuccess(() -> reply("Joined.", chatCommand))
			.ifRoomDoesNotExist(() -> reply("That room doesn't exist...", chatCommand))
			.ifLackingPermissionToPost(() -> reply("I don't seem to have permission to post there.", chatCommand))
			.onError((thrown) -> reply("Hmm, I can't seem to join that room: " + thrown.getMessage(), chatCommand))
		);
		//@formatter:on
	}

	private static class Pending {
		private final int roomId;
		private final Set<Integer> userIds = new HashSet<>();
		private final Instant started = Instant.now();

		public Pending(int roomId) {
			this.roomId = roomId;
		}

		@SuppressWarnings("unused")
		public int getRoomId() {
			return roomId;
		}

		public Set<Integer> getUserIds() {
			return userIds;
		}

		public Instant getStarted() {
			return started;
		}
	}
}
