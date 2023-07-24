package oakbot.task;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.Http;
import oakbot.util.HttpFactory;
import oakbot.util.Now;

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
			.summary("Posts a fact every day.")
			.detail("Facts are from refdesk.com. Fact is posted at 12:00 server time to all non-quiet rooms.")
		.build();
		//@formatter:on
	}

	@Override
	public long nextRun() {
		LocalDateTime now = Now.local();
		LocalDateTime next = now.truncatedTo(ChronoUnit.DAYS).withHour(12);
		if (now.getHour() >= 12) {
			next = next.plusDays(1);
		}
		return now.until(next, ChronoUnit.MILLIS);
	}

	@Override
	public void run(IBot bot) throws Exception {
		String response;
		try (Http http = HttpFactory.connect()) {
			response = http.get(url).getBody();
		}

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
}
