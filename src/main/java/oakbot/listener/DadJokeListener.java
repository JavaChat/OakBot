package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.chat.ChatMessage;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.Sleeper;

/**
 * Responds to "I am [blank]" messages with "Hi [blank], I'm Oak!".
 * @author Michael Angstadt
 */
public class DadJokeListener implements Listener {
	private final String botName;
	private final CatchAllMentionListener catchAllListener;

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
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		if (message.getContent().isOnebox()) {
			return doNothing();
		}

		/*
		 * If the bot is mentioned in the message, let the catch-all mention
		 * listener handle it.
		 */
		if (message.getContent().isMentioned(bot.getUsername()) && catchAllListener != null) {
			return doNothing();
		}

		String messageAsMarkdown = ChatBuilder.toMarkdown(message.getContent().getContent(), message.getContent().isFixedFont());
		String messageWithoutLinks = removeLinks(messageAsMarkdown);
		Optional<String> phrase = findPhrase(messageWithoutLinks);
		if (!phrase.isPresent()) {
			return doNothing();
		}

		if (countWords(phrase.get()) > 5) {
			return doNothing();
		}

		hesitate(Duration.ofSeconds(3));
		return reply("Hi " + phrase.get() + ", I'm " + botName + "!", message);
	}

	private Optional<String> findPhrase(String content) {
		Matcher m = regex.matcher(content);
		if (!m.find()) {
			return Optional.empty();
		}

		String phrase = m.group(2).trim();
		return phrase.isEmpty() ? Optional.empty() : Optional.of(phrase);
	}

	private String removeLinks(String messageAsMarkdown) {
		//@formatter:off
		return messageAsMarkdown
			.replaceAll("\\[(.*?)]\\(.*?\\)", "$1") //markdown links
			.replaceAll("\\bhttps?://[^\\s]+", ""); //plaintext links
		//@formatter:on
	}

	private int countWords(String phrase) {
		Pattern p = Pattern.compile("\\s+");
		Matcher m = p.matcher(phrase);
		int words = 1;
		while (m.find()) {
			words++;
		}
		return words;
	}

	private void hesitate(Duration duration) {
		Sleeper.sleep(duration.toMillis());
	}
}
