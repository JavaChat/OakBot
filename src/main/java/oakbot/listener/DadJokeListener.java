package oakbot.listener;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.chat.ChatMessage;
import oakbot.command.HelpDoc;
import oakbot.util.Sleeper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;

/**
 * Responds to "I am [blank]" messages with "Hi [blank], I'm Oak!".
 * @author Michael Angstadt
 */
public class DadJokeListener implements Listener {
	private final String botName;
	private final Pattern regex = Pattern.compile("(?i)(I\\s+am|I'm)\\s+(.*?)([.,;!?]|$)");

	/**
	 * @param botName name used in replies
	 */
	public DadJokeListener(String botName) {
		this.botName = botName;
	}

	@Override
	public String name() {
		return "dadjoke";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Responds to messages that start with \"I am [blank]\" with \"Hi [blank], I'm Oak!\".")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, BotContext context) {
		String content = message.getContent().getContent();
		Matcher m = regex.matcher(content);
		if (!m.find()) {
			return doNothing();
		}

		String name = m.group(2);
		hesitate(3000);
		return reply("Hi " + name + ", I'm " + botName + "!", message);
	}

	private void hesitate(long ms) {
		try {
			Sleeper.sleep(ms);
		} catch (InterruptedException ignored) {
		}
	}
}
