package oakbot.task;

import static oakbot.bot.ChatActions.doNothing;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.HelpDoc;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;

/**
 * Posts an explanation of the latest XKCD comic from
 * <a href="https://explainxkcd.com">explainxkcd.com</a>.
 * @author Michael Angstadt
 */
public class XkcdExplained implements ScheduledTask, Listener {
	private static final Logger logger = Logger.getLogger(XkcdExplained.class.getName());
	private static final Pattern regex = Pattern.compile("https://xkcd.com/(\\d+)");
	private static final int maxTimesToCheckWikiBeforeGivingUp = 8;

	private final Duration timeToWaitBeforeFirstWikiCheck;
	private final Duration timeToWaitBetweenWikiChecks = Duration.ofMinutes(15);

	final Map<Integer, Comic> comicsByRoom = new HashMap<>();

	/**
	 * @param timeToWaitBeforeFirstWikiCheck the amount time to wait after the
	 * comic is posted to the chat room before crawling explainxkcd.com for the
	 * explanation (duration string)
	 */
	public XkcdExplained(String timeToWaitBeforeFirstWikiCheck) {
		this.timeToWaitBeforeFirstWikiCheck = Duration.parse(timeToWaitBeforeFirstWikiCheck);
	}

	@Override
	public String name() {
		return "explainxkcd";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Posts an explanation of an XKCD comic whenever one is posted to the room by a system bot. Content scraped from explainxkcd.com.")
		.build();
		//@formatter:on
	}

	@Override
	public long nextRun() {
		return Duration.ofMinutes(1).toMillis();
	}

	@Override
	public void run(IBot bot) throws Exception {
		synchronized (comicsByRoom) {
			List<Integer> stopChecking = new ArrayList<>();
			for (Map.Entry<Integer, Comic> entry : comicsByRoom.entrySet()) {
				int roomId = entry.getKey();
				Comic comic = entry.getValue();

				if (stillNeedToWait(comic)) {
					continue;
				}

				String url = "https://www.explainxkcd.com/wiki/index.php/" + comic.comicId;
				String explanationHtml;
				try {
					explanationHtml = scrapeExplanationHtml(url);
				} catch (Exception e) {
					comic.checkedWiki();
					if (comic.timesCheckedWiki >= maxTimesToCheckWikiBeforeGivingUp) {
						stopChecking.add(roomId);
						logger.severe("Still no explanation posted for comic #" + comic.comicId + " after checking wiki " + comic.timesCheckedWiki + " times. Giving up.");
					}
					continue;
				}

				stopChecking.add(roomId);

				String explanationMd = ChatBuilder.toMarkdown(explanationHtml, false, url);

				//@formatter:off
				String beginningMd = new ChatBuilder()
					.reply(comic.messageContainingComic.getMessageId())
					.bold().link("XKCD #" + comic.comicId + " Explained", url).append(":").bold()
					.append(" ")
				.toString();
				//@formatter:on

				final int MAX_MESSAGE_LENGTH = 500;
				int trimLength = MAX_MESSAGE_LENGTH - beginningMd.length();

				explanationMd = SplitStrategy.WORD.split(explanationMd, trimLength).get(0);
				String message = beginningMd + explanationMd;

				PostMessage postMessage = new PostMessage(message);
				bot.sendMessage(roomId, postMessage);
			}

			stopChecking.forEach(comicsByRoom::remove);
		}
	}

	private boolean stillNeedToWait(Comic comic) {
		if (comic.timesCheckedWiki == 0) {
			Duration postAge = Duration.between(comic.messageContainingComic.getTimestamp(), LocalDateTime.now());
			return (postAge.compareTo(timeToWaitBeforeFirstWikiCheck) < 0);
		} else {
			Duration lastChecked = Duration.between(comic.lastCheckedWiki, Instant.now());
			return (lastChecked.compareTo(timeToWaitBetweenWikiChecks) < 0);
		}
	}

	private String scrapeExplanationHtml(String url) throws IOException {
		String html;
		try (CloseableHttpClient client = httpClient()) {
			HttpGet request = new HttpGet(url);
			try (CloseableHttpResponse response = client.execute(request)) {
				/*
				 * If a wiki page for the comic has not been created
				 * yet, the response status code will be 404.
				 */
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new IOException();
				}

				html = EntityUtils.toString(response.getEntity());
			}
		}

		Document dom = Jsoup.parse(html);
		Element firstParagraph = dom.selectFirst("p");
		if (firstParagraph == null) {
			throw new IOException();
		}

		/*
		 * If an explanation hasn't been posted yet, the <p> tag may
		 * just contain <br> tags and other whitespace.
		 */
		if (firstParagraph.text().isEmpty()) {
			throw new IOException();
		}

		return firstParagraph.html();
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		boolean postedBySystemBot = message.getUserId() < 1;
		if (postedBySystemBot) {
			Matcher m = regex.matcher(message.getContent().getContent());
			if (m.find()) {
				int comicId = Integer.parseInt(m.group(1));
				synchronized (comicsByRoom) {
					comicsByRoom.put(message.getRoomId(), new Comic(message, comicId));
				}
			}
		}

		return doNothing();
	}

	CloseableHttpClient httpClient() {
		return HttpClients.createDefault();
	}

	static class Comic {
		final ChatMessage messageContainingComic;
		final int comicId;

		Instant lastCheckedWiki;
		int timesCheckedWiki;

		public Comic(ChatMessage messageContainingComic, int comicId) {
			this.messageContainingComic = messageContainingComic;
			this.comicId = comicId;
		}

		public void checkedWiki() {
			lastCheckedWiki = Instant.now();
			timesCheckedWiki++;
		}
	}
}
