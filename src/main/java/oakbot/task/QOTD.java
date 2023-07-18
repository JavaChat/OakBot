package oakbot.task;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.Http;
import oakbot.util.HttpFactory;
import oakbot.util.Now;

/**
 * Posts a quote once per day at midnight.
 * @author Michael Angstadt
 */
public class QOTD implements ScheduledTask {
	@Override
	public String name() {
		return "qotd";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Posts a quote every day at 12am from slashdot.org.")
		.build();
		//@formatter:on
	}

	@Override
	public long nextRun() {
		LocalDateTime now = Now.local();
		LocalDateTime tomorrow = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
		return now.until(tomorrow, ChronoUnit.MILLIS);
	}

	@Override
	public void run(IBot bot) throws Exception {
		ChatBuilder cb = fromSlashdot();
		bot.broadcastMessage(new PostMessage(cb).splitStrategy(SplitStrategy.WORD));
	}

	ChatBuilder fromSlashdot() throws IOException {
		Document document;
		try (Http http = HttpFactory.connect()) {
			document = http.get("https://slashdot.org").getBodyAsHtml();
		}

		Element element = document.selectFirst("blockquote[cite='https://slashdot.org'] p");

		return new ChatBuilder() //@formatter:off
			.append(element.text())
			.append(" (").link("source", "https://slashdot.org").append(")"); //@formatter:on
	}

	ChatBuilder fromTheySaidSo() throws IOException {
		JsonNode json;
		try (Http http = HttpFactory.connect()) {
			json = http.get("http://quotes.rest/qod.json").getBodyAsJson();
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
}
