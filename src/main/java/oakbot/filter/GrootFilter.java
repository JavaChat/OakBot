package oakbot.filter;

import java.util.Random;
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
		var fixed = message.startsWith(ChatBuilder.FIXED_WIDTH_PREFIX);

		var cb = new ChatBuilder();
		if (fixed) {
			cb.fixedWidth();
		}

		/*
		 * Preserve the reply message ID, if present.
		 */
		var m = replyRegex.matcher(message);
		if (m.find()) {
			message = message.substring(m.end());
			cb.append(m.group());
		}

		var rng = new GrootRng(message.hashCode());
		var lines = message.split("\r\n|\r|\n");
		var applyFormatting = !fixed && lines.length == 1;
		for (var line : lines) {
			var grootSentencesToGenerate = line.trim().isEmpty() ? 0 : (countWords(line) / (grootWords.length * 2) + 1);

			//@formatter:off
			var grootLine = IntStream.range(0, grootSentencesToGenerate)
				.mapToObj(i -> grootSentence(applyFormatting, rng))
			.collect(Collectors.joining(" "));
			//@formatter:on

			cb.append(grootLine).nl();
		}

		return cb.toString().stripTrailing();
	}

	private int countWords(String message) {
		var count = 1;
		var m = whitespaceRegex.matcher(message);
		while (m.find()) {
			count++;
		}
		return count;
	}

	private CharSequence grootSentence(boolean applyFormatting, GrootRng rng) {
		var cb = new ChatBuilder();

		var contraction = rng.useContractionForIAm();
		for (var i = 0; i < grootWords.length; i++) {
			var grootWord = grootWords[i];

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

			var bold = rng.formatBold();
			var italic = rng.formatItalic();
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
			return switch (rand()) {
			case 0 -> "Grooot";
			case 1 -> "Groooot";
			default -> "Groot";
			};
		}

		private String endSentence() {
			return switch (rand()) {
			case 0, 1 -> "!";
			case 2 -> "!!";
			case 3 -> "...";
			default -> ".";
			};
		}

		private int rand() {
			return random.nextInt(10);
		}
	}
}
