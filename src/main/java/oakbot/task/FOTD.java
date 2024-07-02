package oakbot.task;

import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;
import oakbot.util.Now;

/**
 * Posts a fact once per day at noon.
 * @author Michael Angstadt
 */
public class FOTD implements ScheduledTask {
	private static final Logger logger = LoggerFactory.getLogger(FOTD.class);
	private static final String URL = "http://www.refdesk.com";
	private static final String ARCHIVE_URL = URL + "/fotd-arch.html";

	@Override
	public String name() {
		return "fotd";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Posts a fact every day.")
			.detail("Facts are from refdesk.com. Fact is posted at 12:00 server time to all non-quiet rooms.")
		.build();
		//@formatter:on
	}

	@Override
	public long nextRun() {
		var now = Now.local();
		var next = now.truncatedTo(ChronoUnit.DAYS).withHour(12);
		if (now.getHour() >= 12) {
			next = next.plusDays(1);
		}
		return now.until(next, ChronoUnit.MILLIS);
	}

	@Override
	public void run(IBot bot) throws Exception {
		String response;
		try (var http = HttpFactory.connect()) {
			response = http.get(URL).getBody();
		}

		var fact = parseFact(response);
		if (fact == null) {
			logger.atWarn().log(() -> "Unable to parse FOTD from " + URL + ".");
			return;
		}

		fact = ChatBuilder.toMarkdown(fact, false, true, URL);
		var cb = new ChatBuilder(fact);

		var isMultiline = fact.contains("\n");
		if (isMultiline) {
			cb.nl().append("Source: " + ARCHIVE_URL);
		} else {
			cb.append(' ').link("(source)", ARCHIVE_URL);
		}

		var postMessage = new PostMessage(cb).splitStrategy(SplitStrategy.WORD);
		bot.broadcastMessage(postMessage);
	}

	private String parseFact(String html) {
		var p = Pattern.compile("<!-- FOTD START -->(.*?)Provided\\s*by", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		var m = p.matcher(html);
		if (!m.find()) {
			return null;
		}

		var fact = m.group(1);

		/*
		 * Trim spaces and dashes from the end of the string (in the past,
		 * there used to be a dash before "provided by").
		 */
		for (var i = fact.length() - 1; i >= 0; i--) {
			var c = fact.charAt(i);
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
}
