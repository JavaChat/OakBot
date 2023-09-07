package oakbot.filter;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Translates a message from English to Groot.
 * @author Michael Angstadt
 */
public class GrootFilter extends ToggleableFilter {
	private static final Pattern whitespaceRegex = Pattern.compile("\\s+");
	private static final Pattern replyRegex = Pattern.compile("^:\\d+\\s");

	private static final String[] grootWords = { "I", "am", "Groot" };
	private static final int I = 0;
	private static final int AM = 1;
	private static final int GROOT = 2;

	@Override
	public String name() {
		return "groot";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("I am Groot.")
			.detail("Toggles a filter that makes Oak speak in Groot.")
			.includeSummaryWithDetail(false)
		.build();
		//@formatter:on
	}

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

		GrootRng rng = new GrootRng(message.hashCode());
		String[] lines = message.split("\r\n|\r|\n");
		boolean applyFormatting = !fixed && lines.length == 1;
		for (String line : lines) {
			if (fixed) {
				cb.fixed();
			}

			int grootSentencesToGenerate = line.trim().isEmpty() ? 0 : (countWords(line) / (grootWords.length * 2) + 1);

			//@formatter:off
			String grootLine = IntStream.range(0, grootSentencesToGenerate)
				.mapToObj(i -> grootSentence(applyFormatting, rng))
			.collect(Collectors.joining(" "));
			//@formatter:on

			cb.append(grootLine).nl();
		}

		return cb.toString().stripTrailing();
	}

	private int countWords(String message) {
		int count = 1;
		Matcher m = whitespaceRegex.matcher(message);
		while (m.find()) {
			count++;
		}
		return count;
	}

	private CharSequence grootSentence(boolean applyFormatting, GrootRng rng) {
		ChatBuilder cb = new ChatBuilder();

		boolean contraction = rng.useContractionForIAm();
		for (int i = 0; i < grootWords.length; i++) {
			String grootWord = grootWords[i];

			if (i == AM && contraction) {
				/*
				 * Do not output "am" if the contraction "I'm" was used.
				 */
				continue;
			}

			/*
			 * Insert space between each word.
			 */
			if (i > 0) {
				cb.append(' ');
			}

			boolean bold = rng.formatBold();
			boolean italic = rng.formatItalic();
			if (applyFormatting) {
				if (bold) {
					cb.bold();
				}

				if (italic) {
					cb.italic();
				}
			}

			if (i == I && contraction) {
				grootWord = "I'm";
			}
			if (i == GROOT) {
				grootWord = rng.grootWithVariableOs();
			}

			if (rng.useCaps()) {
				grootWord = grootWord.toUpperCase();
			}

			cb.append(grootWord);

			if (applyFormatting) {
				if (italic) {
					cb.italic();
				}

				if (bold) {
					cb.bold();
				}
			}
		}

		return cb.append(rng.endSentence());
	}

	private class GrootRng {
		private final Random random;

		public GrootRng(int seed) {
			this.random = new Random(seed);
		}

		private boolean formatBold() {
			return rand() < 2;
		}

		private boolean formatItalic() {
			return rand() < 2;
		}

		private boolean useCaps() {
			return rand() < 2;
		}

		private boolean useContractionForIAm() {
			return rand() < 1;
		}

		private String grootWithVariableOs() {
			switch (rand()) {
			case 0:
				return "Grooot";
			case 1:
				return "Groooot";
			default:
				return "Groot";
			}
		}

		private String endSentence() {
			switch (rand()) {
			case 0:
			case 1:
				return "!";
			case 2:
				return "!!";
			case 3:
				return "...";
			default:
				return ".";
			}
		}

		private int rand() {
			return random.nextInt(10);
		}
	}
}
