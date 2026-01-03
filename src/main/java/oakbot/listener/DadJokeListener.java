package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.util.Sleeper;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.StringUtils;

/**
 * Responds to "I am [blank]" messages with "Hi [blank], I'm Oak!".
 * @author Michael Angstadt
 */
public class DadJokeListener implements Listener {
	private static final Duration HESITATION = Duration.ofSeconds(3);
	private static final Duration TIME_BETWEEN_JOKES = Duration.ofMinutes(30);

	private final String botName;
	private final CatchAllMentionListener catchAllListener;
	private final Map<Integer, Instant> lastJokeByRoom = new HashMap<>();

	//@formatter:off
	private final Pattern regex = Pattern.compile(
		"(?i)" +
		"(?:^|@[^ ]+|:\\d+|[.?!])\\s*" +
		"(I\\s+am|I'm)\\b" +
		"(.*?)" +
		"([.,;!?\\n]|\\band\\b|$)"
	);
	//@formatter:on

	/**
	 * @param botName name used in replies
	 * @param catchAllListener the catch-all listener (can be null)
	 */
	public DadJokeListener(String botName, CatchAllMentionListener catchAllListener) {
		this.botName = botName;
		this.catchAllListener = catchAllListener;
	}

	@Override
	public String name() {
		return "dadjoke";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Responds to sentences that start with \"I am [blank]\".")
			.detail("Responds once every " + TIME_BETWEEN_JOKES.toMinutes() + " minutes per room at most.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		if (message.content().isOnebox()) {
			return doNothing();
		}

		/*
		 * If the bot is mentioned in the message, let the catch-all mention
		 * listener handle it.
		 */
		if (message.isUserMentioned(bot.getUserId(), bot.getUsername()) && catchAllListener != null) {
			return doNothing();
		}

		if (respondedRecently(message)) {
			return doNothing();
		}

		var messageAsMarkdown = ChatBuilder.toMarkdown(message.content().getContent(), message.content().isFixedWidthFont());
		var messageWithoutLinks = removeLinks(messageAsMarkdown);
		var phrase = findPhrase(messageWithoutLinks);
		if (phrase.isEmpty()) {
			return doNothing();
		}

		var name = phrase.get();
		if (StringUtils.countWords(name) > 5) {
			return doNothing();
		}

		hesitate();

		//@formatter:off
		var response = name.equalsIgnoreCase(botName) ?
			"Hi " + name + ", I'm " + botName + " too!" :
			"Hi " + name + ", I'm " + botName + "!";
		//@formatter:on

		lastJokeByRoom.put(message.roomId(), Instant.now());

		return reply(response, message);
	}

	private boolean respondedRecently(ChatMessage message) {
		var lastJoke = lastJokeByRoom.get(message.roomId());
		if (lastJoke == null) {
			return false;
		}

		var timeSinceLastJoke = Duration.between(lastJoke, Instant.now());
		return (timeSinceLastJoke.compareTo(TIME_BETWEEN_JOKES) < 0);
	}

	private Optional<String> findPhrase(String content) {
		var m = regex.matcher(content);
		if (!m.find()) {
			return Optional.empty();
		}

		var phrase = m.group(2).trim();
		return phrase.isEmpty() ? Optional.empty() : Optional.of(phrase);
	}

	private String removeLinks(String messageAsMarkdown) {
		//@formatter:off
		return messageAsMarkdown
			.replaceAll("\\[(.*?)]\\([^\\)]*+\\)", "$1") //markdown links
			.replaceAll("\\bhttps?://[^\\s]+", ""); //plaintext links
		//@formatter:on
	}

	private void hesitate() {
		Sleeper.sleep(HESITATION);
	}
}
