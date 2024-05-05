package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.post;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.util.Sleeper;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.command.HelpDoc;

/**
 * Displays a "wave" emoticon in response to another user's wave emoticon.
 * @author Michael Angstadt
 */
public class WaveListener implements Listener {
	private static final String WAVE_R = "o/";
	private static final String WAVE_L = "\\o";
	private static final Pattern waveRegex = Pattern.compile("(^|\\s)(o/|\\\\o)(\\s|$)");
	private static final Duration timeBetweenWaves = Duration.ofMinutes(5);

	private final Duration hesitation;
	private final CatchAllMentionListener catchAllListener;
	private final Map<Integer, Instant> lastWaveTimeByRoom = new HashMap<>();

	/**
	 * @param hesitation the amount of time to wait before waving back (duration
	 * string)
	 */
	public WaveListener(String hesitation) {
		this(hesitation, null);
	}

	/**
	 * @param hesitation the amount of time to wait before waving back (duration
	 * string)
	 * @param catchAllListener the catch-all listener
	 */
	public WaveListener(String hesitation, CatchAllMentionListener catchAllListener) {
		this.hesitation = Duration.parse(hesitation);
		this.catchAllListener = catchAllListener;
	}

	@Override
	public String name() {
		return "wave";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Waves back at you. " + WAVE_R)
			.detail("Responds with the opposite \"wave\" emoticon when a user waves: " + WAVE_R + " or " + WAVE_L + ". Will only wave once every " + timeBetweenWaves.toMinutes() + " minutes at most.")
			.includeSummaryWithDetail(false)
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		var content = message.getContent().getContent();
		var mentioned = message.getContent().isMentioned(bot.getUsername());

		String wave;
		if (mentioned) {
			/*
			 * If mentioned, look for the emoticon somewhere within the message
			 * text.
			 */
			var m = waveRegex.matcher(content);
			if (!m.find()) {
				return doNothing();
			}

			if (catchAllListener != null) {
				catchAllListener.ignoreNextMessage();
			}
			wave = m.group(2);
		} else {
			/*
			 * If not mentioned, only respond if the message consists of just
			 * the emoticon.
			 */
			if (!content.equals(WAVE_R) && !content.equals(WAVE_L)) {
				return doNothing();
			}

			var roomId = message.getRoomId();
			var lastWave = lastWaveTimeByRoom.get(roomId);

			/*
			 * Do not respond if the bot was not mentioned and it responded
			 * recently. Always wave back to admins.
			 */
			var now = Instant.now();
			var timeSinceLastWave = (lastWave == null) ? timeBetweenWaves : Duration.between(lastWave, now);
			var authorIsAdmin = bot.getAdminUsers().contains(message.getUserId());
			if (!authorIsAdmin && timeSinceLastWave.compareTo(timeBetweenWaves) < 0) {
				return doNothing();
			}

			lastWaveTimeByRoom.put(roomId, now);
			wave = content;
		}

		/*
		 * Wait for a moment to make it seem less robotic.
		 */
		hesitate();

		var reply = reverse(wave);
		return post(reply);
	}

	private void hesitate() {
		Sleeper.sleep(hesitation);
	}

	private String reverse(String wave) {
		return wave.equals(WAVE_R) ? WAVE_L : WAVE_R;
	}
}
