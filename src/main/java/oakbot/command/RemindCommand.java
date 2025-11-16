package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatBuilder;

/**
 * Reminds the user about something.
 * @author Michael Angstadt
 */
public class RemindCommand implements Command {
	@Override
	public String name() {
		return "remind";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Reminds you about something.")
			.detail("Syntax: <REMINDER> in <NUM> [hour|minute]")
			.example("do the laundry in 2 hours", "Sends the user a reminder in 2 hours.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContent();
		if (content.isEmpty()) {
			return reply("Enter what you want to be reminded about, followed by a time period (e.g. " + bot.getTrigger() + name() + " do the laundry in 2 hours)", chatCommand);
		}

		var p = Pattern.compile("^(.*?) in (\\d+) (hour|minute)s?", Pattern.CASE_INSENSITIVE);
		var m = p.matcher(content);
		if (!m.find()) {
			return reply("Invalid format. Example: " + bot.getTrigger() + name() + " do the laundry in 2 hours", chatCommand);
		}

		var reminder = m.group(1);
		var duration = parseDuration(m);
		var mention = chatCommand.getMessage().getUsername().replace(" ", "");

		//@formatter:off
		return ChatActions.create(
			new PostMessage(new ChatBuilder().append("Created.")).parentId(chatCommand.getMessage().getMessageId()),
			new PostMessage(new ChatBuilder().mention(mention).append(" ").append(reminder)).delay(duration)
		);
		//@formatter:on
	}

	private Duration parseDuration(Matcher m) {
		var num = Integer.parseInt(m.group(2));
		var period = m.group(3).toLowerCase();

		return "hour".equals(period) ? Duration.ofHours(num) : Duration.ofMinutes(num);
	}
}
