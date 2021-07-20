package oakbot.listener;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.chat.ChatMessage;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.Sleeper;

import java.util.Optional;
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

	private final Pattern regex = Pattern.compile( //@formatter:off
		"(?i)" +
		"(?:^|@[^ ]+|:\\d+|[.?!])\\s*" +
		"(I\\s+am|I'm)\\b" +
		"(.*?)" +
		"([.,;!?\\n]|\\band\\b|$)"
	); //@formatter:on

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
		if (message.getContent().isOnebox()) {
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

		hesitate(3000);
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
		return messageAsMarkdown
			.replaceAll("\\[(.*?)]\\(.*?\\)", "$1") //markdown links
			.replaceAll("\\bhttps?://[^\\s]+", ""); //plaintext links
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

	private void hesitate(long ms) {
		try {
			Sleeper.sleep(ms);
		} catch (InterruptedException ignored) {
		}
	}
}
