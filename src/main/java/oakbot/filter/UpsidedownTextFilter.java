package oakbot.filter;

/**
 * Turns text upside down.
 * @author Michael Angstadt
 * @see http://stackoverflow.com/q/24371977/13379
 */
public class UpsidedownTextFilter extends ChatResponseFilter {
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

		int max = -1;
		for (int i = 0; i < normal.length(); i++) {
			char n = normal.charAt(i);
			if (n > max) {
				max = n;
			}
		}
		map = new char[max + 1];

		for (int i = 0; i < normal.length(); i++) {
			char n = normal.charAt(i);
			char u = upsideDown.charAt(i);
			map[n] = u;
		}
	}

	@Override
	public String filter(String message) {
		StringBuilder sb = new StringBuilder(message.length());

		char prev = 0;
		boolean flip = true, reply = false;
		for (int i = 0; i < message.length(); i++) {
			char n = message.charAt(i);

			if (i == 0 && n == ':') {
				reply = true;
				sb.append(n);
				prev = n;
				continue;
			}

			if (reply) {
				if (Character.isDigit(n)) {
					sb.append(n);
					prev = n;
					continue;
				}
				reply = false;
			}

			if (prev == ']' && n == '(') {
				//URL
				flip = false;
				sb.append(n);
				prev = n;
				continue;
			}

			if (!flip) {
				if (n == ')') {
					flip = true;
				}
				sb.append(n);
				prev = n;
				continue;
			}

			if (n < map.length) {
				char u = map[n];
				sb.append((u == 0) ? n : u);
			} else {
				sb.append(n);
			}

			prev = n;
		}

		return sb.toString();
	}
}
