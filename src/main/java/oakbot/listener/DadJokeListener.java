package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.util.Sleeper;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Responds to "I am [blank]" messages with "Hi [blank], I'm Oak!".
 * @author Michael Angstadt
 */
public class DadJokeListener implements Listener {
	private final String botName;
	private final CatchAllMentionListener catchAllListener;
	private final Duration hesitation = Duration.ofSeconds(3);

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

		String messageAsMarkdown = ChatBuilder.toMarkdown(message.getContent().getContent(), message.getContent().isFixedWidthFont());
		String messageWithoutLinks = removeLinks(messageAsMarkdown);
		Optional<String> phrase = findPhrase(messageWithoutLinks);
		if (!phrase.isPresent()) {
			return doNothing();
		}

		String name = phrase.get();
		if (countWords(name) > 5) {
			return doNothing();
		}

		hesitate();

		//@formatter:off
		String response = name.equalsIgnoreCase(botName) ?
			"Hi " + name + ", I'm " + botName + " too!" :
			"Hi " + name + ", I'm " + botName + "!";
		//@formatter:on

		return reply(response, message);
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
			.replaceAll("\\[(.*?)]\\([^\\)]*+\\)", "$1") //markdown links
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

	private void hesitate() {
		Sleeper.sleep(hesitation);
	}
}
