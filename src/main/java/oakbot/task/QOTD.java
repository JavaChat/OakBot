package oakbot.task;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oakbot.bot.Bot;
import oakbot.bot.ChatResponse;
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
		JsonNode response = getResponse();
		JsonNode quoteNode = response.get("contents").get("quotes").get(0);

		String quote = quoteNode.get("quote").asText();
		String author = quoteNode.get("author").asText();
		String permalink = quoteNode.get("permalink").asText();

		ChatBuilder cb = new ChatBuilder();
		cb.italic().append('"').append(quote).append('"').italic();
		cb.append(" -").append(author);
		cb.append(' ').link("(source)", permalink);

		bot.broadcast(new ChatResponse(cb, SplitStrategy.WORD));
	}

	/**
	 * Queries the quote website for the quote of the day.
	 * @return the JSON response
	 * @throws IOException if there's a network problem or a problem parsing the
	 * response
	 */
	private JsonNode getResponse() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		HttpGet request = new HttpGet("http://quotes.rest/qod.json");
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				try (InputStream in = response.getEntity().getContent()) {
					return mapper.readTree(in);
				}
			}
		}
	}
}
