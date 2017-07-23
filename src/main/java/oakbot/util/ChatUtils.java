package oakbot.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities related to chat rooms.
 * @author Michael Angstadt
 */
public class ChatUtils {
	private static final Pattern fkeyRegex = Pattern.compile("value=\"([0-9a-f]{32})\"");

	/**
	 * Parses the "fkey" field out of an HTML page.
	 * @param html the HTML page
	 * @return the fkey or null if not found
	 */
	public static String parseFkey(String html) {
		Matcher m = fkeyRegex.matcher(html);
		return m.find() ? m.group(1) : null;
	}
}
