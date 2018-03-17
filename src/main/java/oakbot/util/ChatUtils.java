package oakbot.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Utilities related to chat rooms.
 * @author Michael Angstadt
 */
public class ChatUtils {
	/**
	 * Parses the fkey field out of an HTML page.
	 * @param html the HTML page
	 * @return the fkey or null if not found
	 */
	public static String parseFkey(String html) {
		Document document = Jsoup.parse(html);
		Elements elements = document.select("input[name=fkey]");
		return elements.isEmpty() ? null : elements.first().attr("value");
	}
}
