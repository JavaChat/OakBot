package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.util.Sleeper;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.command.HelpDoc;

/**
 * Responds to "good morning" messages.
 * @author Michael Angstadt
 */
public class MornListener implements Listener {
	private final Duration timeBetweenReplies = Duration.ofMinutes(10);
	private final Duration hesitation;
	private final CatchAllMentionListener catchAllListener;
	private final Map<Integer, Instant> lastReplyByRoom = new HashMap<>();

	/**
	 * The messages that the bot will respond to, along with its responses. The
	 * longest messages must be specified first.
	 */
	//@formatter:off
	private final List<String[]> responses = List.of(
		new String[] {"good morning", "Good morning."},
		new String[] {"morning", "Morning."},
		new String[] {"morn", "morn"}
	);
	//@formatter:on

	/**
	 * @param hesitation the amount of time to wait before responding (duration
	 * string)
	 */
	public MornListener(String hesitation) {
		this(hesitation, null);
	}

	/**
	 * @param hesitation the amount of time to wait before responding (duration
	 * string)
	 * @param mentionListener the mention listener
	 */
	public MornListener(String hesitation, CatchAllMentionListener catchAllListener) {
		this.hesitation = Duration.parse(hesitation);
		this.catchAllListener = catchAllListener;
	}

	@Override
	public String name() {
		return "morn";
	}

	@Override
	public HelpDoc help() {
		List<String> greetings = responses.stream().map(s -> s[0]).collect(Collectors.toList());

		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Replies to \"good morning\" messages.")
			.detail("Responds to the following greetings: " + String.join(", ", greetings))
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		List<String> mentions = message.getContent().getMentions();
		boolean mentioned = message.getContent().isMentioned(bot.getUsername());
		if (!mentions.isEmpty() && !mentioned) {
			/*
			 * Message isn't directed toward the bot.
			 */
			return doNothing();
		}

		String content = removeMentionsAndPunctuation(message.getContent().getContent());

		Optional<String> reply = responses
				.stream() //@formatter:off
			.filter(s -> content.equalsIgnoreCase(s[0]))
			.map(s -> s[1])
		.findFirst(); //@formatter:on

		if (!reply.isPresent()) {
			return doNothing();
		}

		/*
		 * Always reply if the bot is mentioned.
		 */
		if (mentioned) {
			if (catchAllListener != null) {
				catchAllListener.ignoreNextMessage();
			}

			/*
			 * Wait for a moment to make it seem less robotic.
			 */
			hesitate();

			return reply(reply.get(), message);
		}

		int roomId = message.getRoomId();
		Instant lastReply = lastReplyByRoom.get(roomId);

		/*
		 * Do not respond if the bot was not mentioned and it responded
		 * recently.
		 */
		Instant now = Instant.now();
		Duration timeSinceLastReply = (lastReply == null) ? timeBetweenReplies : Duration.between(lastReply, now);
		if (timeSinceLastReply.compareTo(timeBetweenReplies) < 0) {
			return doNothing();
		}

		lastReplyByRoom.put(roomId, now);

		/*
		 * Wait for a moment to make it seem less robotic.
		 */
		hesitate();

		return post(reply.get());
	}

	private void hesitate() {
		Sleeper.sleep(hesitation);
	}

	private static String removeMentionsAndPunctuation(String message) {
		return message //@formatter:off
			.replaceAll("@[a-zA-Z0-9]+", "") //remove mentions
			.replaceAll("[!,\\.]", "") //remove punctuation
		.trim(); //trim for good measure //@formatter:on
	}
}
