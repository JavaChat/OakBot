package oakbot.task;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import oakbot.bot.Bot;
import oakbot.bot.ChatResponse;
import oakbot.chat.SplitStrategy;
import oakbot.util.ChatBuilder;

/**
 * Posts a fact once per day at noon.
 * @author Michael Angstadt
 */
public class FOTD implements ScheduledTask {
	private static final Logger logger = Logger.getLogger(FOTD.class.getName());

	private final String url = "http://www.refdesk.com";

	@Override
	public long nextRun() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime next = now.truncatedTo(ChronoUnit.DAYS).withHour(12);
		if (now.getHour() >= 12) {
			next = next.plusDays(1);
		}
		return now.until(next, ChronoUnit.MILLIS);
	}

	@Override
	public void run(Bot bot) throws Exception {
		String response = getResponse();
		String fact = parseFact(response);
		if (fact == null) {
			logger.warning("Unable to parse FOTD from " + url + ".");
			return;
		}

		fact = ChatBuilder.toMarkdown(fact, false);
		ChatBuilder cb = new ChatBuilder(fact);

		boolean isMultiline = fact.contains("\n");
		if (isMultiline) {
			cb.nl().append("Source: " + url);
		} else {
			cb.append(' ').link("(source)", url);
		}

		bot.broadcast(new ChatResponse(cb, SplitStrategy.WORD));
	}

	private String parseFact(String html) {
		Pattern p = Pattern.compile("<!------------FOTD START---------------->(.*?)-\\s*Provided\\s*by", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(html);
		return m.find() ? m.group(1).trim().replace("�", "'") : null;
	}

	/**
	 * Queries the website for the fact of the day.
	 * @return the HTML response
	 * @throws IOException if there's a network problem
	 */
	private String getResponse() throws IOException {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(url);
			try (CloseableHttpResponse response = client.execute(request)) {
				return EntityUtils.toString(response.getEntity());
			}
		}
	}
}
