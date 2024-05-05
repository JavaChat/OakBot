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
			char n = normal.charAt(i);
			char u = upsideDown.charAt(i);
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
	public String filter(String message) {
		var sb = new StringBuilder(message.length());

		var flip = true;
		var inReplySyntax = false;
		var it = new CharIterator(message);
		while (it.hasNext()) {
			var n = it.next();

			if (it.index() == 0 && n == ':') {
				//preserve reply syntax
				inReplySyntax = true;
				sb.append(n);
				continue;
			}

			if (inReplySyntax) {
				if (Character.isDigit(n)) {
					sb.append(n);
					continue;
				}
				inReplySyntax = false;
			}

			if (it.prev() == ']' && n == '(') {
				//preserve the URL that links are linked to
				flip = false;
				sb.append(n);
				continue;
			}

			if (!flip) {
				if (n == ')') {
					//end of link URL
					flip = true;
				}
				sb.append(n);
				continue;
			}

			sb.append(flipIfPossible(n));
		}

		return sb.toString();
	}

	private char flipIfPossible(char c) {
		if (c >= map.length) {
			return c;
		}

		var flipped = map[c];
		return (flipped == 0) ? c : flipped;
	}
}
