package oakbot.listener;

import java.util.ArrayList;
import java.util.List;

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

	private final AfkCommand command;
	private final String trigger;

	public AfkListener(AfkCommand command, String trigger) {
		this.command = command;
		this.trigger = trigger;
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
		if (isUserAwayAndTypedAfkCommandAgain(message)) {
			return null;
		}

		boolean returned = command.setBack(message.getUserId());

		List<String> mentions = message.getMentions();
		List<AfkUser> mentionedAfkUsers = getAfkUsers(mentions);
		if (!mentionedAfkUsers.isEmpty()) {
			ChatBuilder cb = new ChatBuilder();
			cb.reply(message);
			boolean first = true;
			for (AfkUser afkUser : mentionedAfkUsers) {
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
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("Welcome back!")
			);
			//@formatter:on
		}

		return null;
	}

	private List<AfkUser> getAfkUsers(List<String> mentions) {
		List<AfkUser> awayInfo = new ArrayList<>();
		for (String mention : mentions) {
			awayInfo.addAll(command.getAfkUsers(mention));
		}
		return awayInfo;
	}

	/**
	 * Check to see if a user is already away and has typed /afk again. If so,
	 * we don't want to say "Welcome back" and then set him to "away" again.
	 * @param message the chat message
	 * @return true or false
	 */
	private boolean isUserAwayAndTypedAfkCommandAgain(ChatMessage message) {
		//@formatter:off
		return
		command.isAway(message.getUserId()) &&
		(
			message.getContent().equals(trigger + command.name()) ||
			message.getContent().startsWith(trigger + command.name() + " ")
		);
		//@formatter:on
	}
}
