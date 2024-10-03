package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.JoinRoom;
import oakbot.util.ChatBuilder;

/**
 * Makes the bot join another room.
 * @author Michael Angstadt
 */
public class SummonCommand implements Command {
	private final int minSummonsRequired;
	private final boolean allowedToJoinRooms;
	private final Duration summonTime = Duration.ofMinutes(2);
	private final Map<Integer, Pending> pendingSummons = new HashMap<>();

	/**
	 * @param minSummonsRequired the minimum number of users that have to summon
	 * the bot to a room before the bot actually joins the room
	 * @param allowedToJoinRooms true if users can use this command to make the
	 * bot join rooms, false if not
	 */
	public SummonCommand(int minSummonsRequired, boolean allowedToJoinRooms) {
		this.minSummonsRequired = minSummonsRequired;
		this.allowedToJoinRooms = allowedToJoinRooms;
	}

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
			.detail("At least " + minSummonsRequired + " user(s) are needed to summon the bot to a room.")
			.example("139", "Makes the bot join the room with ID 139.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var userId = chatCommand.getMessage().getUserId();
		var authorIsAdmin = bot.isAdminUser(userId);

		if (!allowedToJoinRooms && !authorIsAdmin) {
			//@formatter:off
			return reply(new ChatBuilder()
				.append("Command is disabled. ")
				.append("Please ping ").link("Michael", "https://stackoverflow.com/users/13379").append(" in the ").link("Sandbox", "/rooms/1").append(" if you would like Oak to join your chat room.")
				.toString()
			, chatCommand);
			//@formatter:on
		}

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

		if (!authorIsAdmin) {
			var response = checkForEnoughSummonVotes(roomToJoin, userId);
			if (response != null) {
				return reply(response, chatCommand);
			}
		}

		pendingSummons.remove(roomToJoin);

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

	private String checkForEnoughSummonVotes(int roomToJoin, int userId) {
		var pending = getPendingSummons(roomToJoin);

		var alreadyVoted = !pending.getUserIds().add(userId);
		var votesNeeded = minSummonsRequired - pending.getUserIds().size();

		if (alreadyVoted) {
			//@formatter:off
			return (votesNeeded == 1) ?
				"I need a vote from " + votesNeeded + " other person." :
				"I need votes from " + votesNeeded + " other people.";
			//@formatter:on
		}

		if (votesNeeded > 0) {
			//@formatter:off
			return (votesNeeded == 1) ?
				votesNeeded + " more vote needed." :
				votesNeeded + " more votes needed.";
			//@formatter:on
		}

		return null;
	}

	private Pending getPendingSummons(int roomToJoin) {
		var firstPersonToVote = !pendingSummons.containsKey(roomToJoin);
		var pending = pendingSummons.computeIfAbsent(roomToJoin, Pending::new);
		if (firstPersonToVote) {
			return pending;
		}

		var elapsed = Duration.between(pending.getStarted(), Instant.now());
		var prevVoteWasTooLongAgo = (elapsed.compareTo(summonTime) > 0);
		if (prevVoteWasTooLongAgo) {
			/*
			 * Reset everything if the previous vote was too long ago.
			 */
			pending = new Pending(roomToJoin);
			pendingSummons.put(roomToJoin, pending);
		}

		return pending;
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
