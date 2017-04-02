package oakbot.listener;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;

/**
 * Displays a "wave" emoticon in response to another user's wave emoticon.
 * @author Michael Angstadt
 */
public class WaveListener implements Listener {
	private final Pattern waveRegex = Pattern.compile("(^|\\s)(o/|\\\\o)(\\s|$)");
	private final long timeBetweenWaves = TimeUnit.MINUTES.toMillis(5);
	private final String botUsername;
	private final MentionListener mentionListener;
	private long lastWave = 0;

	public WaveListener(String botUsername, MentionListener mentionListener) {
		this.botUsername = botUsername;
		this.mentionListener = mentionListener;
	}

	@Override
	public String name() {
		return "wave";
	}

	@Override
	public String description() {
		return "Waves back.";
	}

	@Override
	public String helpText() {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, BotContext context) {
		boolean mentioned = message.isMentioned(botUsername);

		/*
		 * Do not respond if the bot was not mentioned and it responded
		 * recently.
		 */
		long now = System.currentTimeMillis();
		if (!mentioned && now - lastWave < timeBetweenWaves) {
			return null;
		}

		String content = message.getContent();
		String wave;
		if (mentioned) {
			/*
			 * If mentioned, look for the emoticon somewhere within the message
			 * text.
			 */
			Matcher m = waveRegex.matcher(content);
			if (!m.find()) {
				return null;
			}

			wave = m.group(2);
			mentionListener.ignoreNextMessage();
		} else {
			/*
			 * If not mentioned, only respond if the message consists of just
			 * the emoticon.
			 */
			if (!content.equals("o/") && !content.equals("\\o")) {
				return null;
			}

			wave = content;
			lastWave = now;
		}

		String reply = wave.equals("o/") ? "\\o" : "o/";
		return new ChatResponse(reply);
	}
}
