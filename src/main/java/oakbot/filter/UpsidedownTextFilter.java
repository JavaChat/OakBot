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
		
		normal +=     "()<>[]&_,;.?!'\"";
		upsideDown += ")(><][⅋‾'؛˙¿¡,„";
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

		for (int i = 0; i < message.length(); i++) {
			char n = message.charAt(i);
			if (n >= map.length) {
				sb.append(n);
				continue;
			}

			char u = map[n];
			sb.append((u == 0) ? n : u);
		}

		return sb.toString();
	}
}
