package oakbot.filter;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.util.ChatBuilder;

/**
 * Translates a message from English to Wadu hek.
 * @author Michael Angstadt
 */
public class WaduFilter extends ChatResponseFilter {
	private final Pattern whitespaceRegex = Pattern.compile("\\s+");
	private final Pattern replyRegex = Pattern.compile("^:\\d+\\s");

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
		boolean applyFormatting = !fixed && lines.length == 1;
		boolean firstLine = true;
		for (String line : lines) {
			if (!firstLine) {
				cb.nl();
			}
			firstLine = false;

			if (fixed) {
				cb.fixed();
			}

			if (line.trim().isEmpty()) {
				continue;
			}

			int waduWordCount = countWords(line) / 5 + 1;
			boolean previousWordWasWadu = false;
			boolean startOfNewSentence = true;
			for (int i = 0; i < waduWordCount || previousWordWasWadu; i++) {
				boolean sayWadu;
				if (i == 0) {
					sayWadu = true;
				} else if (previousWordWasWadu) {
					//say "wadu" again?
					sayWadu = random.nextInt(10) < 1;
				} else {
					sayWadu = true;
				}

				if (sayWadu) {
					int letterACount;
					switch (random.nextInt(10)) {
					case 0:
						letterACount = 3;
						break;
					case 1:
					case 2:
						letterACount = 2;
						break;
					default:
						letterACount = 1;
						break;
					}

					int letterUCount;
					switch (random.nextInt(10)) {
					case 0:
						letterUCount = 3;
						break;
					case 1:
					case 2:
						letterUCount = 2;
						break;
					default:
						letterUCount = 1;
						break;
					}

					boolean italics = random.nextInt(10) < 1;
					boolean bold = random.nextInt(10) < 1;
					if (applyFormatting) {
						if (italics) {
							cb.italic();
						}
						if (bold) {
							cb.bold();
						}
					}

					String wadu;
					{
						StringBuilder sb = new StringBuilder();
						sb.append(startOfNewSentence ? 'W' : 'w');
						for (int j = 0; j < letterACount; j++) {
							sb.append('a');
						}
						sb.append('d');
						for (int j = 0; j < letterUCount; j++) {
							sb.append('u');
						}
						wadu = sb.toString();
					}

					boolean allCaps = random.nextInt(20) < 1;
					if (allCaps) {
						wadu = wadu.toUpperCase();
					}
					cb.append(wadu);

					if (applyFormatting) {
						if (italics) {
							cb.italic();
						}
						if (bold) {
							cb.bold();
						}
					}

					previousWordWasWadu = true;
					startOfNewSentence = false;
				} else {
					boolean italics = random.nextInt(10) < 1;
					boolean bold = random.nextInt(10) < 1;
					if (applyFormatting) {
						if (italics) {
							cb.italic();
						}
						if (bold) {
							cb.bold();
						}
					}

					boolean allCaps = random.nextInt(20) < 1;
					cb.append(allCaps ? "HEK" : "hek");

					if (applyFormatting) {
						if (italics) {
							cb.italic();
						}
						if (bold) {
							cb.bold();
						}
					}

					boolean lastWord = (i >= waduWordCount - 1);
					boolean endSentence = lastWord || random.nextInt(10) < 3;
					if (endSentence) {
						String ending;
						switch (random.nextInt(20)) {
						case 0:
							ending = "!!!";
							break;
						case 1:
						case 2:
							ending = "!!";
							break;
						case 3:
						case 4:
							ending = "!";
							break;
						case 5:
						case 6:
							ending = "?";
							break;
						case 7:
							ending = "...";
							break;
						default:
							ending = ".";
							break;
						}
						cb.append(ending);
					}

					startOfNewSentence = endSentence;
					previousWordWasWadu = false;
				}

				cb.append(' ');
			}
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
