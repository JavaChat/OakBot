package oakbot.filter;

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
		for (var line : lines) {
			var waduWordsToGenerate = line.isBlank() ? 0 : StringUtils.countWords(line) / 5 + 1;
			appendWaduLine(waduWordsToGenerate, applyFormatting, rng, cb);
			cb.nl();
		}

		return cb.toString().stripTrailing();
	}

	private void appendWaduLine(int waduWordsToGenerate, boolean applyFormatting, WaduRng rng, ChatBuilder cb) {
		var previousWordWasWadu = false;
		var startOfNewSentence = true;
		for (var i = 0; i < waduWordsToGenerate || previousWordWasWadu; i++) {
			boolean sayWadu;
			if (i == 0) {
				sayWadu = true;
			} else if (previousWordWasWadu) {
				sayWadu = rng.sayWaduAgain();
			} else {
				sayWadu = true;
			}

			if (sayWadu) {
				appendWadu(applyFormatting, startOfNewSentence, rng, cb);

				startOfNewSentence = false;
			} else {
				appendHek(applyFormatting, rng, cb);

				var lastWord = (i >= waduWordsToGenerate - 1);
				var endSentence = lastWord || rng.endSentence();
				if (endSentence) {
					cb.append(rng.ending());
				}

				startOfNewSentence = endSentence;
			}

			previousWordWasWadu = sayWadu;
			cb.append(' ');
		}
	}

	private void appendWadu(boolean applyFormatting, boolean startOfNewSentence, WaduRng rng, ChatBuilder cb) {
		var letterACount = rng.letterACount();
		var letterUCount = rng.letterUCount();
		var italics = rng.formatItalic();
		var bold = rng.formatBold();
		var allCaps = rng.caps();

		if (applyFormatting) {
			if (italics) {
				cb.italic();
			}
			if (bold) {
				cb.bold();
			}
		}

		cb.append((allCaps || startOfNewSentence) ? 'W' : 'w');
		cb.repeat(allCaps ? 'A' : 'a', letterACount);
		cb.append(allCaps ? 'D' : 'd');
		cb.repeat(allCaps ? 'U' : 'u', letterUCount);

		if (applyFormatting) {
			if (bold) {
				cb.bold();
			}
			if (italics) {
				cb.italic();
			}
		}
	}

	private void appendHek(boolean applyFormatting, WaduRng rng, ChatBuilder cb) {
		var italics = rng.formatItalic();
		var bold = rng.formatBold();
		var allCaps = rng.caps();

		if (applyFormatting) {
			if (italics) {
				cb.italic();
			}
			if (bold) {
				cb.bold();
			}
		}

		cb.append(allCaps ? "HEK" : "hek");

		if (applyFormatting) {
			if (bold) {
				cb.bold();
			}
			if (italics) {
				cb.italic();
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
