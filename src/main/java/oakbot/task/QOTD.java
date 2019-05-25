package oakbot.task;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oakbot.bot.Bot;
import oakbot.bot.PostMessage;
import oakbot.chat.SplitStrategy;
import oakbot.util.ChatBuilder;

/**
 * Posts a quote once per day at midnight.
 * @author Michael Angstadt
 */
public class QOTD implements ScheduledTask {
	@Override
	public long nextRun() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime tomorrow = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
		return now.until(tomorrow, ChronoUnit.MILLIS);
	}

	@Override
	public void run(Bot bot) throws Exception {
		ChatBuilder cb = fromSlashdot();
		bot.broadcast(new PostMessage(cb).splitStrategy(SplitStrategy.WORD));
	}

	ChatBuilder fromSlashdot() throws IOException {
		Document document = Jsoup.parse(httpGet("https://slashdot.org"));
		Element element = document.selectFirst("blockquote[cite='https://slashdot.org'] p");

		return new ChatBuilder() //@formatter:off
			.append(element.text())
			.append(" (").link("source", "https://slashdot.org").append(")"); //@formatter:on
	}

	ChatBuilder fromTheySaidSo() throws IOException {
		JsonNode json;
		{
			ObjectMapper mapper = new ObjectMapper();
			String response = httpGet("http://quotes.rest/qod.json");
			json = mapper.readTree(response);
		}

		JsonNode quoteNode = json.get("contents").get("quotes").get(0);

		String quote = quoteNode.get("quote").asText();
		String author = quoteNode.get("author").asText();

		JsonNode node = quoteNode.get("permalink");
		String permalink = (node == null) ? "https://theysaidso.com" : node.asText();

		ChatBuilder cb = new ChatBuilder();
		boolean quoteHasNewlines = (quote.indexOf('\n') >= 0);
		if (quoteHasNewlines) {
			cb.append(quote).nl();
			cb.append('-').append(author);
			cb.append(" (source: ").append(permalink).append(')');
		} else {
			cb.italic().append('"').append(quote).append('"').italic();
			cb.append(" -").append(author);
			cb.append(' ').link("(source)", permalink);
		}
		return cb;
	}

	String httpGet(String url) throws IOException {
		HttpGet request = new HttpGet(url);
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				return EntityUtils.toString(response.getEntity());
			}
		}
	}
}
