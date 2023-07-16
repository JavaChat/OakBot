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

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Posts a fact once per day at noon.
 * @author Michael Angstadt
 */
public class FOTD implements ScheduledTask {
	private static final Logger logger = Logger.getLogger(FOTD.class.getName());

	private final String url = "http://www.refdesk.com";
	private final String archiveUrl = url + "/fotd-arch.html";

	@Override
	public String name() {
		return "fotd";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Posts a fact every day at 12pm from refdesk.com.")
		.build();
		//@formatter:on
	}

	@Override
	public long nextRun() {
		LocalDateTime now = now();
		LocalDateTime next = now.truncatedTo(ChronoUnit.DAYS).withHour(12);
		if (now.getHour() >= 12) {
			next = next.plusDays(1);
		}
		return now.until(next, ChronoUnit.MILLIS);
	}

	@Override
	public void run(IBot bot) throws Exception {
		String response = get(url);
		String fact = parseFact(response);
		if (fact == null) {
			logger.warning("Unable to parse FOTD from " + url + ".");
			return;
		}

		fact = ChatBuilder.toMarkdown(fact, false, url);
		ChatBuilder cb = new ChatBuilder(fact);

		boolean isMultiline = fact.contains("\n");
		if (isMultiline) {
			cb.nl().append("Source: " + archiveUrl);
		} else {
			cb.append(' ').link("(source)", archiveUrl);
		}

		PostMessage postMessage = new PostMessage(cb).splitStrategy(SplitStrategy.WORD);
		bot.broadcastMessage(postMessage);
	}

	private String parseFact(String html) {
		Pattern p = Pattern.compile("<!-- FOTD START -->(.*?)Provided\\s*by", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(html);
		if (!m.find()) {
			return null;
		}

		String fact = m.group(1);

		/*
		 * Trim spaces and dashes from the end of the string (in the past,
		 * there used to be a dash before "provided by").
		 */
		for (int i = fact.length() - 1; i >= 0; i--) {
			char c = fact.charAt(i);
			if (Character.isWhitespace(c) || c == '-') {
				continue;
			}

			fact = fact.substring(0, i + 1);
			break;
		}

		//trim whitespace from the beginning of the string
		fact = fact.trim();

		//fix single quote characters
		fact = fact.replace("�", "'");

		return fact;
	}

	/**
	 * Makes an HTTP GET request to the given URL. This method is
	 * package-private so it can be overridden in unit tests.
	 * @param url the URL
	 * @return the response body
	 * @throws IOException if there's a network problem
	 */
	String get(String url) throws IOException {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(url);
			try (CloseableHttpResponse response = client.execute(request)) {
				return EntityUtils.toString(response.getEntity());
			}
		}
	}

	/**
	 * Gets the current time. This method is package private so unit tests can
	 * override it.
	 * @return the current time
	 */
	LocalDateTime now() {
		return LocalDateTime.now();
	}
}
