package oakbot.filter;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Translates a message from English to Wadu hek.
 * @author Michael Angstadt
 */
public class WaduFilter extends ToggleableFilter {
	private final Pattern whitespaceRegex = Pattern.compile("\\s+");
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

		WaduRng rng = new WaduRng(message.hashCode());
		String[] lines = message.split("\r\n|\r|\n");
		boolean applyFormatting = !fixed && lines.length == 1;
		for (String line : lines) {
			if (fixed) {
				cb.fixed();
			}

			int waduWordsToGenerate = line.trim().isEmpty() ? 0 : countWords(line) / 5 + 1;
			appendWaduLine(waduWordsToGenerate, applyFormatting, rng, cb);
			cb.nl();
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

	private void appendWaduLine(int waduWordsToGenerate, boolean applyFormatting, WaduRng rng, ChatBuilder cb) {
		boolean previousWordWasWadu = false;
		boolean startOfNewSentence = true;
		for (int i = 0; i < waduWordsToGenerate || previousWordWasWadu; i++) {
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

				boolean lastWord = (i >= waduWordsToGenerate - 1);
				boolean endSentence = lastWord || rng.endSentence();
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
		int letterACount = rng.letterACount();
		int letterUCount = rng.letterUCount();
		boolean italics = rng.formatItalic();
		boolean bold = rng.formatBold();
		boolean allCaps = rng.caps();

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
		boolean italics = rng.formatItalic();
		boolean bold = rng.formatBold();
		boolean allCaps = rng.caps();

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
			switch (rand(10)) {
			case 0:
				return 3;
			case 1:
			case 2:
				return 2;
			default:
				return 1;
			}
		}

		private int letterUCount() {
			switch (rand(10)) {
			case 0:
				return 3;
			case 1:
			case 2:
				return 2;
			default:
				return 1;
			}
		}

		private boolean caps() {
			return rand(20) < 1;
		}

		private boolean endSentence() {
			return rand(10) < 3;
		}

		private String ending() {
			switch (rand(20)) {
			case 0:
				return "!!!";
			case 1:
			case 2:
				return "!!";
			case 3:
			case 4:
				return "!";
			case 5:
			case 6:
				return "?";
			case 7:
				return "...";
			default:
				return ".";
			}
		}

		private int rand(int i) {
			return random.nextInt(i);
		}
	}
}
