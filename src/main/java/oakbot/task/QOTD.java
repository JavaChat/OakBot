package oakbot.task;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.github.mangstadt.sochat4j.SplitStrategy;
import com.github.mangstadt.sochat4j.util.Http;

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
	public Duration nextRun() {
		var now = Now.local();
		var tomorrow = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
		return Duration.between(now, tomorrow);
	}

	@Override
	public void run(IBot bot) throws Exception {
		var cb = fromSlashdot();
		bot.broadcastMessage(new PostMessage(cb).splitStrategy(SplitStrategy.WORD));
	}

	ChatBuilder fromSlashdot() throws IOException {
		Http.Response response;
		try (var http = HttpFactory.connect()) {
			response = http.get("https://slashdot.org");
		}

		var element = response.getBodyAsHtml().selectFirst("blockquote[cite='https://slashdot.org'] p");

		//@formatter:off
		return new ChatBuilder()
			.append(element.text())
			.append(" (").link("source", "https://slashdot.org").append(")");
		//@formatter:on
	}

	ChatBuilder fromTheySaidSo() throws IOException {
		Http.Response response;
		try (var http = HttpFactory.connect()) {
			response = http.get("http://quotes.rest/qod.json");
		}

		var json = response.getBodyAsJson();

		var quoteNode = json.path("contents").path("quotes").path(0);

		var quote = quoteNode.path("quote").asText();
		var author = quoteNode.path("author").asText();

		var permalinkNode = quoteNode.path("permalink");
		var permalink = permalinkNode.isMissingNode() ? "https://theysaidso.com" : permalinkNode.asText();

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
