package oakbot.listener;

import static oakbot.listener.Listener.reply;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.command.AfkCommand;
import oakbot.command.AfkCommand.AfkUser;
import oakbot.util.ChatBuilder;

/**
 * Listens for when users who were afk come back. Also, displays a message if
 * someone mentions an afk user.
 * @author Michael Angstadt
 * @see AfkCommand
 */
public class AfkListener implements Listener {
	private final long timeBetweenWarnings = TimeUnit.MINUTES.toMillis(15);
	private final AfkCommand command;

	public AfkListener(AfkCommand command) {
		this.command = command;
	}

	@Override
	public String name() {
		return null;
	}

	@Override
	public String description() {
		return null;
	}

	@Override
	public String helpText() {
		return null;
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, BotContext context) {
		if (isUserAwayAndTypedAfkCommandAgain(message, context.getTrigger())) {
			return null;
		}

		boolean returned = command.setBack(message.getUserId());

		Collection<String> mentions = new HashSet<>(message.getContent().getMentions()); //remove duplicates
		Collection<AfkUser> mentionedAfkUsers = getAfkUsers(mentions);
		List<AfkUser> usersNotWarnedAbout = filterUsersNotWarnedAbout(mentionedAfkUsers, message.getUserId());
		if (!usersNotWarnedAbout.isEmpty()) {
			ChatBuilder cb = new ChatBuilder();
			cb.reply(message);
			boolean first = true;
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
			return new ChatResponse(cb);
		}

		if (returned) {
			return reply("Welcome back!", message);
		}

		return null;
	}

	/**
	 * Determines if the bot recently warned this user about the AFK user(s)
	 * being away.
	 * @param afkUsers all AFK users that the person mentioned
	 * @param userId the user ID of the person that mentioned the AFK users
	 * @return the AFK users the user hasn't been warned about recently
	 */
	private List<AfkUser> filterUsersNotWarnedAbout(Collection<AfkUser> afkUsers, int userId) {
		List<AfkUser> usersNotWarnedAbout = new ArrayList<>(afkUsers.size());
		for (AfkUser afkUser : afkUsers) {
			long lastWarnedUser = afkUser.getTimeLastWarnedUser(userId);
			long sinceLastWarning = System.currentTimeMillis() - lastWarnedUser;
			if (sinceLastWarning > timeBetweenWarnings) {
				usersNotWarnedAbout.add(afkUser);
			}
		}
		return usersNotWarnedAbout;
	}

	/**
	 * Gets the users that are currently AFK.
	 * @param mentions the mentions that were in the chat message
	 * @return the users that are AFK
	 */
	private Collection<AfkUser> getAfkUsers(Collection<String> mentions) {
		Set<AfkUser> afkUsers = new HashSet<>();
		for (String mention : mentions) {
			afkUsers.addAll(command.getAfkUsers(mention));
		}
		return afkUsers;
	}

	/**
	 * Check to see if a user is already away and has typed /afk again. If so,
	 * we don't want to say "Welcome back" and then set him to "away" again.
	 * @param message the chat message
	 * @return true or false
	 */
	private boolean isUserAwayAndTypedAfkCommandAgain(ChatMessage message, String trigger) {
		String content = message.getContent().getContent();

		//@formatter:off
		return
		command.isAway(message.getUserId()) &&
		(
			content.equals(trigger + command.name()) ||
			content.startsWith(trigger + command.name() + " ")
		);
		//@formatter:on
	}
}
