package oakbot.command;

import static oakbot.command.Command.reply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;

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
		String awayMessage = chatCommand.getContentMarkdown();

		setAway(userId, username, awayMessage);

		return reply("Cya later", chatCommand);
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
			this.userId = userId;
			this.username = username;
			this.awayMessage = awayMessage;
		}

		/**
		 * Gets the full username.
		 * @return the username
		 */
		public String getUsername() {
			return username;
		}

		/**
		 * Gets the user's away message.
		 * @return the away message or null if not set
		 */
		public String getAwayMessage() {
			return awayMessage;
		}

		/**
		 * Gets the user ID.
		 * @return the user ID
		 */
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

		@Override
		public int hashCode() {
			return userId;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			AfkUser other = (AfkUser) obj;
			if (userId != other.userId) return false;
			if (!username.equals(other.username)) return false;
			return true;
		}
	}
}
