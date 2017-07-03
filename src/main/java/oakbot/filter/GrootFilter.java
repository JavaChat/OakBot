package oakbot.filter;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.util.ChatBuilder;

/**
 * Translates a message from English to Groot.
 * @author Michael Angstadt
 */
public class GrootFilter extends ChatResponseFilter {
	private final Pattern whitespaceRegex = Pattern.compile("\\s+");
	private final Pattern replyRegex = Pattern.compile("^:\\d+\\s");
	private final String grootWords[] = { "I", "am", "Groot" };

	@Override
	public String filter(String message) {
		boolean fixed = message.startsWith("    ");

		ChatBuilder cb = new ChatBuilder();

		/*
		 * Preserve the reply message ID, if present.
		 */
		Matcher m = replyRegex.matcher(message);
		if (m.find()) {
			message = message.substring(m.end());
			cb.append(m.group());
		}

		Random random = new Random(message.hashCode());
		String[] lines = message.split("\r\n|\r|\n");
		boolean firstLine = true;
		for (String line : lines) {
			if (!firstLine) {
				cb.nl();
			}

			if (fixed) {
				cb.fixed();
			}

			int words = countWords(line);
			int grootSentences = words / (grootWords.length * 2) + 1;

			for (int i = 0; i < grootSentences; i++) {
				boolean first = true;
				for (String grootWord : grootWords) {
					if (!first) {
						cb.append(' ');
					}

					boolean bold = random.nextInt(10) < 2;
					boolean italic = random.nextInt(10) < 2;
					if (!fixed) {
						if (bold) {
							cb.bold();
						}

						if (italic) {
							cb.italic();
						}
					}

					if (grootWord.equals("Groot")) {
						switch (random.nextInt(10)) {
						case 0:
							grootWord = "Grooot";
						case 1:
							grootWord = "Groooot";
						}
					}

					boolean caps = random.nextInt(10) < 2;
					if (caps) {
						grootWord = grootWord.toUpperCase();
					}

					cb.append(grootWord);

					if (!fixed) {
						if (italic) {
							cb.italic();
						}

						if (bold) {
							cb.bold();
						}
					}

					first = false;
				}

				switch (random.nextInt(10)) {
				case 0:
				case 1:
					cb.append('!');
					break;
				case 2:
					cb.append("!!");
					break;
				case 3:
					cb.append("...");
					break;
				default:
					cb.append('.');
					break;
				}

				cb.append(' ');
			}

			firstLine = false;
		}

		return cb.toString();
	}

	private int countWords(String message) {
		int count = 1;
		Matcher m = whitespaceRegex.matcher(message);
		while (m.find()) {
			count++;
		}
		return count;
	}
}
