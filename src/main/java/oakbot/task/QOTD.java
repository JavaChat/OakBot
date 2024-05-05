package oakbot.task;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
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
			.summary("Posts a quote every day.")
			.detail("Quotes are from slashdot.org. Quote is posted at 0:00 server time to all non-quiet rooms.")
		.build();
		//@formatter:on
	}

	@Override
	public long nextRun() {
		var now = Now.local();
		var tomorrow = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
		return now.until(tomorrow, ChronoUnit.MILLIS);
	}

	@Override
	public void run(IBot bot) throws Exception {
		var cb = fromSlashdot();
		bot.broadcastMessage(new PostMessage(cb).splitStrategy(SplitStrategy.WORD));
	}

	ChatBuilder fromSlashdot() throws IOException {
		Document document;
		try (var http = HttpFactory.connect()) {
			document = http.get("https://slashdot.org").getBodyAsHtml();
		}

		var element = document.selectFirst("blockquote[cite='https://slashdot.org'] p");

		return new ChatBuilder() //@formatter:off
			.append(element.text())
			.append(" (").link("source", "https://slashdot.org").append(")"); //@formatter:on
	}

	ChatBuilder fromTheySaidSo() throws IOException {
		JsonNode json;
		try (var http = HttpFactory.connect()) {
			json = http.get("http://quotes.rest/qod.json").getBodyAsJson();
		}

		var quoteNode = json.get("contents").get("quotes").get(0);

		var quote = quoteNode.get("quote").asText();
		var author = quoteNode.get("author").asText();

		var node = quoteNode.get("permalink");
		var permalink = (node == null) ? "https://theysaidso.com" : node.asText();

		var cb = new ChatBuilder();
		var quoteHasNewlines = (quote.indexOf('\n') >= 0);
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
