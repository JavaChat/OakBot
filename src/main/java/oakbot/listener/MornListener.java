package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.util.Sleeper;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

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
	 * The messages that the bot will respond to, along with its responses.
	 */
	//@formatter:off
	private final List<Match> responses = List.of(
		new StringMatch("good morning", "Good morning."),
		new StringMatch("good mourning", "Good mourning."),
		new StringMatch("good moaning", "Good moaning."),
		new StringMatch("goat morning", "Goat morning."),
		new StringMatch("goat mourning", "Goat mourning."),
		new StringMatch("goat moaning", "Goat moaning."),
		new StringMatch("morning", "Morning."),
		new StringMatch("morn", "morn"),
		new RegexMatch("(?i)yee+", new ChatBuilder().link("yee", "https://youtu.be/q6EoRBvdVPQ").toString())
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
		//@formatter:off
		var greetings = responses.stream()
			.map(Match::getGreeting)
		.collect(Collectors.joining(", "));

		return new HelpDoc.Builder(this)
			.summary("Replies to \"good morning\" messages.")
			.detail("Responds to the following greetings: " + greetings)
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		var mentions = message.getContent().getMentions();
		var mentioned = message.getContent().isMentioned(bot.getUsername());
		if (!mentions.isEmpty() && !mentioned) {
			/*
			 * Message isn't directed toward the bot.
			 */
			return doNothing();
		}

		var content = removeMentionsAndPunctuation(message.getContent().getContent());

		//@formatter:off
		var reply = responses.stream()
			.filter(m -> m.matches(content))
			.map(Match::getResponse)
		.findFirst();
		//@formatter:on

		if (reply.isEmpty()) {
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

		var roomId = message.getRoomId();
		var lastReply = lastReplyByRoom.get(roomId);

		/*
		 * Do not respond if the bot was not mentioned and it responded
		 * recently.
		 */
		var now = Instant.now();
		var timeSinceLastReply = (lastReply == null) ? timeBetweenReplies : Duration.between(lastReply, now);
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
		//@formatter:off
		return message
			.replaceAll("@[a-zA-Z0-9]+", "") //remove mentions
			.replaceAll("[!,\\.]", "") //remove punctuation
		.trim(); //trim for good measure
		//@formatter:on
	}

	private static interface Match {
		String getGreeting();

		String getResponse();

		boolean matches(String content);
	}

	private static class StringMatch implements Match {
		protected final String greeting, response;

		public StringMatch(String greeting, String response) {
			this.greeting = greeting;
			this.response = response;
		}

		@Override
		public String getGreeting() {
			return greeting;
		}

		@Override
		public String getResponse() {
			return response;
		}

		@Override
		public boolean matches(String content) {
			return content.equalsIgnoreCase(greeting);
		}
	}

	private static class RegexMatch extends StringMatch {
		private final Pattern p;

		public RegexMatch(String regex, String response) {
			super(regex, response);
			p = Pattern.compile(regex);
		}

		@Override
		public boolean matches(String content) {
			return p.matcher(content).matches();
		}
	}
}
