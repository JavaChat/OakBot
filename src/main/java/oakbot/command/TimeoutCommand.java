package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.util.List;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;

/**
 * Stops the bot from responding to incoming messages.
 * @author Michael Angstadt
 */
public class TimeoutCommand implements Command {
	@Override
	public String name() {
		return "timeout";
	}

	@Override
	public List<String> aliases() {
		return List.of("shutup");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Stops the bot from responding to incoming messages (admins only).")
			.detail("Will still respond to messages from admin users.")
			.example("10", "Timeout for 10 minutes.")
			.example("cancel", "Ends the timeout early.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var userId = chatCommand.getMessage().userId();
		if (!bot.isAdminUser(userId)) {
			return reply("Only admins can run this command.", chatCommand);
		}

		var content = chatCommand.getContent();
		if ("cancel".equalsIgnoreCase(content)) {
			bot.cancelTimeout();
			return reply("Timeout canceled.", chatCommand);
		}

		int minutes;
		try {
			minutes = Integer.parseInt(content);
		} catch (NumberFormatException e) {
			return reply("Enter the number of minutes.", chatCommand);
		}

		if (minutes <= 0) {
			return reply("Enter a positive number.", chatCommand);
		}

		bot.timeout(Duration.ofMinutes(minutes));

		return reply("Incoming messages will be ignored for " + minutes + " minutes.", chatCommand);
	}
}
