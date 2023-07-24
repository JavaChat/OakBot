package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.post;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.chat.ChatMessage;
import oakbot.command.HelpDoc;
import oakbot.util.Sleeper;

/**
 * Displays a "wave" emoticon in response to another user's wave emoticon.
 * @author Michael Angstadt
 */
public class WaveListener implements Listener {
	private final String WAVE_R = "o/", WAVE_L = "\\o";
	private final Pattern waveRegex = Pattern.compile("(^|\\s)(o/|\\\\o)(\\s|$)");
	private final Duration timeBetweenWaves = Duration.ofMinutes(5);
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
		String content = message.getContent().getContent();
		boolean mentioned = message.getContent().isMentioned(bot.getUsername());

		String wave;
		if (mentioned) {
			/*
			 * If mentioned, look for the emoticon somewhere within the message
			 * text.
			 */
			Matcher m = waveRegex.matcher(content);
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

			int roomId = message.getRoomId();
			Instant lastWave = lastWaveTimeByRoom.get(roomId);

			/*
			 * Do not respond if the bot was not mentioned and it responded
			 * recently. Always wave back to admins.
			 */
			Instant now = Instant.now();
			Duration timeSinceLastWave = (lastWave == null) ? timeBetweenWaves : Duration.between(lastWave, now);
			boolean authorIsAdmin = bot.getAdminUsers().contains(message.getUserId());
			if (!authorIsAdmin && timeSinceLastWave.compareTo(timeBetweenWaves) < 0) {
				return doNothing();
			}

			lastWaveTimeByRoom.put(roomId, now);
			wave = content;
		}

		/*
		 * Wait for a moment to make it seem less robotic.
		 */
		Sleeper.sleep(hesitation);

		String reply = reverse(wave);
		return post(reply);
	}

	private String reverse(String wave) {
		return wave.equals(WAVE_R) ? WAVE_L : WAVE_R;
	}
}
