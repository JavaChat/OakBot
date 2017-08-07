package oakbot.command;

import static oakbot.command.Command.reply;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import oakbot.bot.BotContext;
import oakbot.bot.BotContext.JoinRoomCallback;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;

/**
 * Makes the bot join another room.
 * @author Michael Angstadt
 */
public class SummonCommand implements Command {
	private final int minSummonsRequired;
	private final long summonTime = TimeUnit.MINUTES.toMillis(2);
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
	public String description() {
		return "Brings the bot to another room.";
	}

	@Override
	public String helpText(String trigger) {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String content = chatCommand.getContent().trim();

		Integer maxRooms = context.getMaxRooms();
		if (maxRooms != null && context.getCurrentRooms().size() >= maxRooms) {
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

		if (context.getCurrentRooms().contains(roomToJoin)) {
			return reply("I'm already there... -_-", chatCommand);
		}

		if (!context.isAuthorAdmin()) {
			Pending pending = pendingSummons.get(roomToJoin);
			long elapsed = System.currentTimeMillis() - ((pending == null) ? 0 : pending.getStarted());
			if (elapsed > summonTime) {
				pending = new Pending(roomToJoin);
				pendingSummons.put(roomToJoin, pending);
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

		context.joinRoom(roomToJoin, new JoinRoomCallback() {
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

	private static class Pending {
		private final int roomId;
		private final Set<Integer> userIds = new HashSet<Integer>();
		private final long started = System.currentTimeMillis();

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

		public long getStarted() {
			return started;
		}
	}
}
