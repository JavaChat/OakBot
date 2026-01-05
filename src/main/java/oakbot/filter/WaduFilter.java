package oakbot.filter;

import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;

import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.StringUtils;

/**
 * Translates a message from English to Wadu hek.
 * @author Michael Angstadt
 */
public class WaduFilter extends ToggleableFilter {
	private final Pattern replyRegex = Pattern.compile("^:\\d+\\s");

	@Override
	public String name() {
		return "wadu";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Wadu hek?")
			.detail("Toggles a filter that makes Oak speak in Wadu Hek.")
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

		var rng = new WaduRng(message.hashCode());
		var lines = message.split("\r\n|\r|\n");
		var applyFormatting = !fixed && lines.length == 1;
		var generator = new WaduWordGenerator(rng, applyFormatting);

		//@formatter:off
		Arrays.stream(lines)
			.map(line -> translate(line, generator))
		.forEach(waduLine -> cb.append(waduLine).nl());
		//@formatter:on

		return cb.toString().stripTrailing();
	}

	private String translate(String original, WaduWordGenerator generator) {
		var num = calculateHowManyWaduWordsToGenerate(original);
		return generator.generateWords(num);
	}

	private int calculateHowManyWaduWordsToGenerate(String line) {
		if (line.isBlank()) {
			return 0;
		}

		var wordCount = StringUtils.countWords(line);
		return wordCount / 5 + 1;
	}

	private class WaduWordGenerator {
		private final WaduRng rng;
		private final boolean applyFormatting;
		private ChatBuilder cb;

		public WaduWordGenerator(WaduRng rng, boolean applyFormatting) {
			this.rng = rng;
			this.applyFormatting = applyFormatting;
		}

		public String generateWords(int wordsToGenerate) {
			cb = new ChatBuilder();
			var previousWordWasWadu = false; //every sequence of one or more "wadu"s must end with a single "hek"
			var startOfNewSentence = true;

			for (var i = 0; i < wordsToGenerate || previousWordWasWadu; i++) {
				boolean sayWadu = shouldWaduBeTheNextWord(i, previousWordWasWadu);

				if (sayWadu) {
					appendWadu(startOfNewSentence);

					startOfNewSentence = false;
				} else {
					appendHek();

					var lastWord = (i >= wordsToGenerate - 1);
					var endSentence = lastWord || rng.endSentence();
					if (endSentence) {
						cb.append(rng.ending());
					}

					startOfNewSentence = endSentence;
				}

				previousWordWasWadu = sayWadu;
				cb.append(' ');
			}

			return cb.toString();
		}

		private boolean shouldWaduBeTheNextWord(int i, boolean previousWordWasWadu) {
			if (i == 0) {
				return true;
			}

			return previousWordWasWadu ? rng.sayWaduAgain() : true;
		}

		private void appendWadu(boolean startOfNewSentence) {
			var letterACount = rng.letterACount();
			var letterUCount = rng.letterUCount();
			var italics = rng.formatItalic();
			var bold = rng.formatBold();
			var allCaps = rng.caps();

			applyFormatting(bold, italics);

			cb.append((allCaps || startOfNewSentence) ? 'W' : 'w');
			cb.repeat(allCaps ? 'A' : 'a', letterACount);
			cb.append(allCaps ? 'D' : 'd');
			cb.repeat(allCaps ? 'U' : 'u', letterUCount);

			applyFormatting(bold, italics);
		}

		private void appendHek() {
			var italics = rng.formatItalic();
			var bold = rng.formatBold();
			var allCaps = rng.caps();

			applyFormatting(bold, italics);

			cb.append(allCaps ? "HEK" : "hek");

			applyFormatting(bold, italics);
		}

		private void applyFormatting(boolean bold, boolean italics) {
			if (applyFormatting) {
				if (bold) {
					cb.bold();
				}
				if (italics) {
					cb.italic();
				}
			}
		}
	}

	private class WaduRng {
		private final Random random;

		public WaduRng(int seed) {
			this.random = new Random(seed);
		}

		private boolean sayWaduAgain() {
			return rand(10) < 1;
		}

		private boolean formatBold() {
			return rand(10) < 1;
		}

		private boolean formatItalic() {
			return rand(10) < 1;
		}

		private int letterACount() {
			return switch (rand(10)) {
			case 0 -> 3;
			case 1, 2 -> 2;
			default -> 1;
			};
		}

		private int letterUCount() {
			return switch (rand(10)) {
			case 0 -> 3;
			case 1, 2 -> 2;
			default -> 1;
			};
		}

		private boolean caps() {
			return rand(20) < 1;
		}

		private boolean endSentence() {
			return rand(10) < 3;
		}

		private String ending() {
			return switch (rand(20)) {
			case 0 -> "!!!";
			case 1, 2 -> "!!";
			case 3, 4 -> "!";
			case 5, 6 -> "?";
			case 7 -> "...";
			default -> ".";
			};
		}

		private int rand(int i) {
			return random.nextInt(i);
		}
	}
}
