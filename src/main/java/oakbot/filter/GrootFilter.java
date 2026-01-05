package oakbot.filter;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.StringUtils;

/**
 * Translates a message from English to Groot.
 * @author Michael Angstadt
 */
public class GrootFilter extends ToggleableFilter {
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
	public String filter(MessageParts messageParts) {
		var cb = new ChatBuilder();

		var fixed = messageParts.fixedWidth();
		if (fixed) {
			cb.fixedWidth();
		}

		/*
		 * Preserve the reply message ID, if present.
		 */
		var replyPrefix = messageParts.replyPrefix();
		if (replyPrefix != null) {
			cb.append(replyPrefix);
		}

		var message = messageParts.messageContent();
		var rng = new GrootRng(message.hashCode());
		var lines = message.split("\r\n|\r|\n");
		var applyFormatting = !fixed && lines.length == 1;
		var sentenceBuilder = new GrootSentenceBuilder(rng, applyFormatting);

		//@formatter:off
		Arrays.stream(lines)
			.map(line -> translate(line, sentenceBuilder))
		.forEach(grootLine -> cb.append(grootLine).nl());
		//@formatter:on

		return cb.toString().stripTrailing();
	}

	private String translate(String original, GrootSentenceBuilder sentenceBuilder) {
		var num = calculateHowManyGrootSentencesToGenerate(original);
		return generateGrootSentences(sentenceBuilder, num);
	}

	private int calculateHowManyGrootSentencesToGenerate(String line) {
		if (line.isBlank()) {
			return 0;
		}

		var wordCount = StringUtils.countWords(line);
		var grootWords = GrootWord.values().length;

		return wordCount / (grootWords * 2) + 1;
	}

	private String generateGrootSentences(GrootSentenceBuilder sentenceBuilder, int num) {
		//@formatter:off
		return Stream.generate(sentenceBuilder::nextSentence)
			.limit(num)
		.collect(Collectors.joining(" "));
		//@formatter:on
	}

	private enum GrootWord {
		I("I"), AM("am"), GROOT("Groot");

		private final String word;

		private GrootWord(String word) {
			this.word = word;
		}

		@Override
		public String toString() {
			return word;
		}
	}

	private class GrootSentenceBuilder {
		private final GrootRng rng;
		private final boolean applyFormatting;
		private boolean useContractionForIAm;

		private GrootSentenceBuilder(GrootRng rng, boolean applyFormatting) {
			this.rng = rng;
			this.applyFormatting = applyFormatting;
		}

		private String nextSentence() {
			useContractionForIAm = rng.useContractionForIAm();

			//@formatter:off
			var sentence = Arrays.stream(GrootWord.values())
				.filter(this::skipAmIfContractionUsed)
				.map(this::buildWord)
			.collect(Collectors.joining(" "));
			//@formatter:on

			return sentence + rng.endSentence();
		}

		private boolean skipAmIfContractionUsed(GrootWord grootWord) {
			return !(grootWord == GrootWord.AM && useContractionForIAm);
		}

		private String buildWord(GrootWord grootWord) {
			ChatBuilder cb = new ChatBuilder();

			var bold = rng.formatBold();
			var italic = rng.formatItalic();
			applyFormatting(italic, bold, cb);

			String word = grootWord.toString();
			if (grootWord == GrootWord.I && useContractionForIAm) {
				word = "I'm";
			}
			if (grootWord == GrootWord.GROOT) {
				word = rng.grootWithVariableOs();
			}

			if (rng.useCaps()) {
				word = word.toUpperCase();
			}

			cb.append(word);

			applyFormatting(italic, bold, cb);

			return cb.toString();
		}

		private void applyFormatting(boolean italic, boolean bold, ChatBuilder cb) {
			if (applyFormatting) {
				if (italic) {
					cb.italic();
				}

				if (bold) {
					cb.bold();
				}
			}
		}
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
