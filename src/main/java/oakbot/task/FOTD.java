package oakbot.task;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import oakbot.bot.Bot;
import oakbot.bot.ChatResponse;
import oakbot.chat.SplitStrategy;
import oakbot.util.ChatBuilder;

/**
 * Posts a fact once per day at noon.
 * @author Michael Angstadt
 */
public class FOTD implements ScheduledTask {
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
		String quote = parseQuote(response);
		if (quote == null) {
			return;
		}

		quote = ChatBuilder.toMarkdown(quote, false);
		ChatBuilder cb = new ChatBuilder(quote);
		cb.append(' ').link("(source)", "http://www.refdesk.com");
		bot.broadcast(new ChatResponse(cb, SplitStrategy.WORD));
	}

	private String parseQuote(String html) {
		Pattern p = Pattern.compile("<!------------FOTD START---------------->(.*?)-\\s*Provided\\s*by", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(html);
		return m.find() ? m.group(1).trim() : null;
	}

	/**
	 * Queries the website for the fact of the day.
	 * @return the JSON response
	 * @throws IOException if there's a network problem or a problem parsing the
	 * response
	 */
	private String getResponse() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		HttpGet request = new HttpGet("http://www.refdesk.com");
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				try (InputStream in = response.getEntity().getContent()) {
					byte buf[] = new byte[4096];
					int read;
					while ((read = in.read(buf)) != -1) {
						out.write(buf, 0, read);
					}
				}
			}
		}

		return new String(out.toByteArray());
	}
}
