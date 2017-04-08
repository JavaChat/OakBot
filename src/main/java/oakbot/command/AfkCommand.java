package oakbot.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatBuilder;

/**
 * Marks users as being "afk".
 * @author Michael Angstadt
 */
public class AfkCommand implements Command {
	private final Map<Integer, AfkUser> afkUsersById = new HashMap<>();

	@Override
	public String name() {
		return "afk";
	}

	@Override
	public List<String> aliases() {
		return Arrays.asList("brb");
	}

	@Override
	public String description() {
		return "Allows users to mark themselves as \"away\".";
	}

	@Override
	public String helpText(String trigger) {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String username = chatCommand.getMessage().getUsername();
		int userId = chatCommand.getMessage().getUserId();
		String awayMessage = chatCommand.getContent();

		setAway(userId, username, awayMessage);

		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(chatCommand)
			.append("Cya later.")
		);
		//@formatter:on
	}

	/**
	 * Gets the afk info for one or more users. Note that this method returns a
	 * list because more than one user can have the same username. Also,
	 * mentions only have to match the beginning of a username.
	 * @param mention the username as it appears in a mention
	 * @return the afk users that match the mention or empty list if no users
	 * match
	 */
	public List<AfkUser> getAfkUsers(String mention) {
		if (afkUsersById.isEmpty()) {
			return Collections.emptyList();
		}

		/*
		 * Mentions must be at least 3 characters.
		 */
		if (mention.length() < 3) {
			return Collections.emptyList();
		}

		String sanitizedMention = mention.toLowerCase();
		List<AfkUser> matchedUsers = new ArrayList<>();
		for (AfkUser user : afkUsersById.values()) {
			String sanitizedUserName = user.getUsername().replaceAll(" ", "").toLowerCase();
			if (sanitizedUserName.startsWith(sanitizedMention)) {
				matchedUsers.add(user);
			}
		}
		return matchedUsers;
	}

	/**
	 * Determines if a user is away.
	 * @param userId the user ID
	 * @return true if the user is away, false if not
	 */
	public boolean isAway(int userId) {
		return afkUsersById.containsKey(userId);
	}

	/**
	 * Sets a user as "away".
	 * @param userId the user ID
	 * @param username the username
	 * @param awayMessage the away message or null not to leave an away message
	 */
	public void setAway(int userId, String username, String awayMessage) {
		if (awayMessage != null && awayMessage.isEmpty()) {
			awayMessage = null;
		}

		afkUsersById.put(userId, new AfkUser(userId, username, awayMessage));
	}

	/**
	 * Sets a user as "back".
	 * @param userId the user ID
	 * @return true if the user was afk before, false if the user was never afk
	 * to begin with
	 */
	public boolean setBack(int userId) {
		return afkUsersById.remove(userId) != null;
	}

	/**
	 * Represents a user who is afk.
	 * @author Michael Angstadt
	 */
	public static class AfkUser {
		private final Map<Integer, Long> lastWarnedUser = new HashMap<>();
		private final String username, awayMessage;
		private final int userId;

		public AfkUser(int userId, String username, String awayMessage) {
			this.username = username;
			this.userId = userId;
			this.awayMessage = awayMessage;
		}

		public String getUsername() {
			return username;
		}

		public String getAwayMessage() {
			return awayMessage;
		}

		public int getUserId() {
			return userId;
		}

		/**
		 * Gets the time that a user was last notified by the bot that the AFK
		 * user is away.
		 * @param userId the user ID
		 * @return the time or zero if the user never received a notification
		 */
		public long getTimeLastWarnedUser(int userId) {
			Long time = lastWarnedUser.get(userId);
			return (time == null) ? 0 : time;
		}

		/**
		 * Resets the time that a user was last notified by the bot that the AFK
		 * user is away.
		 * @param userId
		 */
		public void setTimeLastWarnedUser(int userId) {
			long time = System.currentTimeMillis();
			lastWarnedUser.put(userId, time);
		}
	}
}
