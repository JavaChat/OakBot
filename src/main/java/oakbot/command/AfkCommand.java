package oakbot.command;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.chat.ChatMessage;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;

/**
 * Marks users as being "afk".
 * @author Michael Angstadt
 */
public class AfkCommand implements Command, Listener {
	private final Map<Integer, AfkUser> afkUsersById = new HashMap<>();
	private final Duration timeBetweenWarnings = Duration.ofMinutes(15);

	@Override
	public String name() {
		return "afk";
	}

	@Override
	public List<String> aliases() {
		return Arrays.asList("brb");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((Command)this)
			.summary("Allows users to mark themselves as \"away from the keyboard\".")
			.detail("If the user is mentioned in chat, Oak will post a message saying that the user is away. As soon as the user posts a message, Oak will welcome them back and remove their \"away\" status. \"Away\" status spans all chat rooms the user has joined.")
			.example("", "Marks the user as \"away\".")
			.example("Feeding the dog.", "Marks the user as \"away\" and provides an away message that Oak will post if the user is mentioned in chat.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String username = chatCommand.getMessage().getUsername();
		int userId = chatCommand.getMessage().getUserId();
		String awayMessage = chatCommand.getContentMarkdown();

		setAway(userId, username, awayMessage);

		return reply("Cya later", chatCommand);
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		/*
		 * Ignore "/afk" command messages. These are handled by
		 * Command.onMessage().
		 */
		if (isInvokingMe(message, bot.getTrigger())) {
			return doNothing();
		}

		boolean returned = setBack(message.getUserId());

		Collection<String> mentions = new HashSet<>(message.getContent().getMentions()); //remove duplicates
		Collection<AfkUser> mentionedAfkUsers = getAfkUsers(mentions);
		List<AfkUser> usersNotWarnedAbout = filterUsersNotWarnedAbout(mentionedAfkUsers, message.getUserId());
		if (!usersNotWarnedAbout.isEmpty()) {
			ChatBuilder cb = new ChatBuilder();
			cb.reply(message);
			boolean first = true;
			usersNotWarnedAbout.sort(Comparator.comparing(AfkUser::getUsername));
			for (AfkUser afkUser : usersNotWarnedAbout) {
				afkUser.setTimeLastWarnedUser(message.getUserId());

				if (!first) {
					cb.nl();
				}

				cb.append(afkUser.getUsername()).append(" is away");
				String awayMessage = afkUser.getAwayMessage();
				if (awayMessage != null) {
					cb.append(": ").append(awayMessage);
				}

				first = false;
			}
			return post(cb);
		}

		if (returned) {
			return reply("Welcome back!", message);
		}

		return doNothing();
	}

	/**
	 * Gets the afk info for one or more users. Note that this method returns a
	 * list because more than one user can have the same username. Also,
	 * mentions only have to match the beginning of a username.
	 * @param mention the username as it appears in a mention
	 * @return the afk users that match the mention or empty list if no users
	 * match
	 */
	private List<AfkUser> getAfkUsers(String mention) {
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
		return afkUsersById.values().stream().filter(user -> {
			String sanitizedUserName = user.getUsername().replaceAll(" ", "").toLowerCase();
			return sanitizedUserName.startsWith(sanitizedMention);
		}).collect(Collectors.toList());
	}

	/**
	 * Sets a user as "away".
	 * @param userId the user ID
	 * @param username the username
	 * @param awayMessage the away message or null not to leave an away message
	 */
	void setAway(int userId, String username, String awayMessage) {
		if (awayMessage != null && awayMessage.isEmpty()) {
			awayMessage = null;
		}

		afkUsersById.put(userId, new AfkUser(userId, username, awayMessage));
	}

	/**
	 * Sets a user as "back".
	 * @param userId the user ID
	 * @return true if the user is returning from being afk, false if the user
	 * was never afk to begin with
	 */
	private boolean setBack(int userId) {
		return afkUsersById.remove(userId) != null;
	}

	/**
	 * Determines if the bot recently warned this user about the AFK user(s)
	 * being away.
	 * @param afkUsers all AFK users that were mentioned
	 * @param userId the user ID of the person that mentioned the AFK users
	 * @return the AFK users the user hasn't been warned about recently
	 */
	private List<AfkUser> filterUsersNotWarnedAbout(Collection<AfkUser> afkUsers, int userId) {
		List<AfkUser> usersNotWarnedAbout = new ArrayList<>(afkUsers.size());
		for (AfkUser afkUser : afkUsers) {
			Instant lastWarnedUser = afkUser.getTimeLastWarnedUser(userId);
			if (lastWarnedUser == null) {
				usersNotWarnedAbout.add(afkUser);
			} else {
				Duration sinceLastWarning = Duration.between(lastWarnedUser, Instant.now());
				if (sinceLastWarning.compareTo(timeBetweenWarnings) > 0) {
					usersNotWarnedAbout.add(afkUser);
				}
			}
		}
		return usersNotWarnedAbout;
	}

	/**
	 * Gets the users that are currently AFK.
	 * @param mentions the mentions that were in the chat message
	 * @return the mentioned users that are AFK
	 */
	private Collection<AfkUser> getAfkUsers(Collection<String> mentions) {
		Set<AfkUser> afkUsers = new HashSet<>();
		for (String mention : mentions) {
			afkUsers.addAll(getAfkUsers(mention));
		}
		return afkUsers;
	}

	/**
	 * Represents a user who is afk.
	 * @author Michael Angstadt
	 */
	public static class AfkUser {
		private final Map<Integer, Instant> lastWarnedUser = new HashMap<>();
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
		 * @return the time or null if the user never received a notification
		 */
		public Instant getTimeLastWarnedUser(int userId) {
			return lastWarnedUser.get(userId);
		}

		/**
		 * Resets the time that a user was last notified by the bot that the AFK
		 * user is away.
		 * @param userId the user ID
		 */
		public void setTimeLastWarnedUser(int userId) {
			lastWarnedUser.put(userId, Instant.now());
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
