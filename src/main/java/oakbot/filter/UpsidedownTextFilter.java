package oakbot.filter;

import oakbot.command.HelpDoc;
import oakbot.util.CharIterator;

/**
 * Turns text upside down.
 * @author Michael Angstadt
 * @see http://stackoverflow.com/q/24371977/13379
 */
public class UpsidedownTextFilter extends ToggleableFilter {
	private final char[] map;
	{
		String normal, upsideDown;

		//@formatter:off
		normal =      "abcdefghijklmnopqrstuvwxyz";
		upsideDown =  "ɐqɔpǝɟƃɥıɾʞlɯuodbɹsʇnʌʍxʎz";
		
		normal +=     "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		upsideDown += "∀qϽᗡƎℲƃHIſʞ˥WNOԀὉᴚS⊥∩ΛMXʎZ";

		normal +=     "0123456789";
		upsideDown += "0ƖᄅƐㄣϛ9ㄥ86";
		
		normal +=     "<>&,;.?!'\"";
		upsideDown += "><⅋'؛˙¿¡,„";
		//@formatter:on

		var highestAsciiValue = normal.chars().max().getAsInt();
		map = new char[highestAsciiValue + 1];

		for (var i = 0; i < normal.length(); i++) {
			var n = normal.charAt(i);
			var u = upsideDown.charAt(i);
			map[n] = u;
		}
	}

	@Override
	public String name() {
		return "rollover";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Turns the bot upside down.")
			.detail("Toggles a filter that makes all the letters in the messages Oak posts look like they are upside down.")
			.includeSummaryWithDetail(false)
		.build();
		//@formatter:on
	}

	@Override
	public String filter(MessageParts messageParts) {
		var replyPrefix = messageParts.replyPrefix();
		if (replyPrefix == null) {
			replyPrefix = "";
		}

		var message = messageParts.messageContent();

		return replyPrefix + new TextFlipper(message).flip();
	}

	private class TextFlipper {
		private final CharIterator it;
		private final StringBuilder sb;

		private boolean flip = true;

		public TextFlipper(String message) {
			it = new CharIterator(message);
			sb = new StringBuilder(message.length());
		}

		public String flip() {
			while (it.hasNext()) {
				var c = it.next();
				processCharacter(c);
			}

			return sb.toString();
		}

		private void processCharacter(char c) {
			var startOfUrl = (it.prev() == ']' && c == '(');
			if (startOfUrl) {
				//preserve the URL that links are linked to
				flip = false;
				sb.append(c);
				return;
			}

			if (!flip) {
				var endOfUrl = (c == ')');
				if (endOfUrl) {
					flip = true;
				}
				sb.append(c);
				return;
			}

			sb.append(flipIfPossible(c));
		}

		private char flipIfPossible(char c) {
			if (c >= map.length) {
				return c;
			}

			var flipped = map[c];
			return (flipped == 0) ? c : flipped;
		}
	}
}
