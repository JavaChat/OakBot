package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.reply;
import static oakbot.util.StringUtils.plural;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Displays the user's usage quota for rate-limited functions.
 * @author Michael Angstadt
 */
public class QuotaCommand implements Command {
	private final ChatGPT chatGpt;
	private final ImagineCore imagine;

	public QuotaCommand(ChatGPT chatGpt, ImagineCore imagine) {
		this.chatGpt = chatGpt;
		this.imagine = imagine;
	}

	@Override
	public String name() {
		return "quota";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays the invoking user's usage quota for rate-limited functions.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var userId = chatCommand.getMessage().userId();

		var cb = new ChatBuilder();
		printQuota("Conversation (ChatGPT)", chatGpt.getUsageQuota(), userId, cb);
		printQuota("Image generation", imagine.getUsageQuota(), userId, cb);

		return reply(cb, chatCommand);
	}

	private void printQuota(String label, UsageQuota quota, int userId, ChatBuilder cb) {
		cb.append(label).append(": ");

		var max = quota.getRequestsPerPeriod();
		if (max == 0) {
			cb.append("no limit").nl();
			return;
		}

		var used = quota.getCurrent(userId);
		cb.append(used).append(" / ").append(max);

		if (used >= max) {
			var timeUntilNextRequest = quota.getTimeUntilUserCanMakeRequest(userId);
			var hours = timeUntilNextRequest.toHours() + 1;
			cb.append("; available in ").append(hours).append(plural(" hour", hours));
		}

		var period = quota.getPeriod();
		cb.append(" (period = ").append(period.toString()).append(")").nl();
	}
}
