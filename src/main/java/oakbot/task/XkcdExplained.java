package oakbot.task;

import static oakbot.bot.ChatActions.doNothing;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.HelpDoc;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;
import oakbot.util.Now;

/**
 * Posts an explanation of the latest XKCD comic from
 * <a href="https://explainxkcd.com">explainxkcd.com</a>.
 * @author Michael Angstadt
 */
public class XkcdExplained implements ScheduledTask, Listener {
	private static final Logger logger = LoggerFactory.getLogger(XkcdExplained.class);
	private static final Pattern regex = Pattern.compile("https://xkcd.com/(\\d+)");
	private static final int maxTimesToCheckWikiBeforeGivingUp = 8;

	private final Duration timeToWaitBeforeFirstWikiCheck;
	private final Duration timeToWaitBetweenWikiChecks = Duration.ofMinutes(15);

	final Map<Integer, Comic> comicsByRoom = new HashMap<>();

	/**
	 * @param timeToWaitBeforeFirstWikiCheck the amount time to wait after the
	 * comic is posted to the chat room before crawling explainxkcd.com for the
	 * explanation
	 */
	public XkcdExplained(Duration timeToWaitBeforeFirstWikiCheck) {
		this.timeToWaitBeforeFirstWikiCheck = timeToWaitBeforeFirstWikiCheck;
	}

	@Override
	public String name() {
		return "explainxkcd";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((Listener)this)
			.summary("Posts an explanation of an XKCD comic whenever one is posted to the room by a system bot.")
			.detail("Content from explainxkcd.com.")
		.build();
		//@formatter:on
	}

	@Override
	public Duration nextRun() {
		return Duration.ofMinutes(1);
	}

	@Override
	public void run(IBot bot) throws Exception {
		var stopChecking = new ArrayList<Integer>();
		for (var entry : comicsByRoom.entrySet()) {
			var roomId = entry.getKey();
			var comic = entry.getValue();

			if (stillNeedToWait(comic)) {
				continue;
			}

			var url = url(comic.comicId);
			String explanationHtml;
			try {
				explanationHtml = scrapeExplanationHtml(url);
			} catch (Exception e) {
				comic.checkedWiki();
				if (comic.timesCheckedWiki >= maxTimesToCheckWikiBeforeGivingUp) {
					stopChecking.add(roomId);
					logger.atError().log(() -> "Still no explanation posted for comic #" + comic.comicId + " after checking wiki " + comic.timesCheckedWiki + " times. Giving up.");
				}
				continue;
			}

			stopChecking.add(roomId);

			/*
			 * Ensure the content has no newlines. Newlines prevent the
			 * markdown conversion from happening.
			 */
			explanationHtml = explanationHtml.replaceAll("(\\r\\n|\\n|\\r)", " ");

			var explanationMd = ChatBuilder.toMarkdown(explanationHtml, false, false, url);

			//@formatter:off
			String beginningMd = new ChatBuilder()
				.bold().link("XKCD #" + comic.comicId + " Explained", url).append(":").bold()
				.append(" ")
			.toString();
			//@formatter:on

			final var MAX_MESSAGE_LENGTH = 500;
			var trimLength = MAX_MESSAGE_LENGTH - beginningMd.length();

			explanationMd = SplitStrategy.WORD.split(explanationMd, trimLength).get(0);
			var message = beginningMd + explanationMd;

			var postMessage = new PostMessage(message).parentId(comic.messageContainingComic.id());
			bot.sendMessage(roomId, postMessage);
		}

		stopChecking.forEach(comicsByRoom::remove);
	}

	private String url(int comicId) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("www.explainxkcd.com")
			.setPathSegments("wiki", "index.php", Integer.toString(comicId))
		.toString();	
		//@formatter:on
	}

	private boolean stillNeedToWait(Comic comic) {
		if (comic.timesCheckedWiki == 0) {
			var postAge = Duration.between(comic.messageContainingComic.timestamp(), Now.local());
			return (postAge.compareTo(timeToWaitBeforeFirstWikiCheck) < 0);
		} else {
			var lastChecked = Duration.between(comic.lastCheckedWiki, Now.instant());
			return (lastChecked.compareTo(timeToWaitBetweenWikiChecks) < 0);
		}
	}

	private String scrapeExplanationHtml(String url) throws IOException {
		Document dom;
		try (var http = HttpFactory.connect()) {
			var response = http.get(url);

			/*
			 * If a wiki page for the comic has not been created
			 * yet, the response status code will be 404.
			 */
			if (response.getStatusCode() != 200) {
				throw new IOException();
			}

			dom = response.getBodyAsHtml();
		}

		var next = nextSiblingThatsNotJustWhitespace(dom.getElementById("Explanation").parentNode());

		/*
		 * A notice such as "This explanation may be incomplete" may be at the
		 * top of the Explanation section. These notices are inside of a <table>
		 * element.
		 */
		next = skipExplanationIncompleteTable(next);

		if (next instanceof Element element) {
			/*
			 * If an explanation hasn't been posted yet, then we may have
			 * reached the next section of the wiki page.
			 */
			if ("h2".equals(element.tagName())) {
				throw new IOException();
			}

			/*
			 * Sometimes, the first paragraph is wrapped in a <p> element.
			 */
			if ("p".equals(element.tagName())) {
				/*
				 * If an explanation hasn't been posted yet, the <p> tag may
				 * just contain <br> tags and other whitespace.
				 */
				if (element.text().isEmpty()) {
					throw new IOException();
				}

				return element.html();
			}
		}

		/*
		 * Other times, the content of the first paragraph is not wrapped in a
		 * <p> element, but subsequent paragraphs are.
		 */
		var firstParagraph = new StringBuilder();
		while (true) {
			firstParagraph.append(next.outerHtml());
			next = next.nextSibling();

			if (next instanceof Element element) {
				if ("p".equals(element.tagName())) {
					break;
				}
			}
		}
		return firstParagraph.toString().trim();
	}

	private Node skipExplanationIncompleteTable(Node node) {
		if (node instanceof Element element) {
			//table may be wrapped in a div
			if ("div".equals(element.tagName())) {
				element = element.firstElementChild();
				if (element == null) {
					return node;
				}
			}

			if ("table".equals(element.tagName())) {
				return nextSiblingThatsNotJustWhitespace(node);
			}
		}

		return node;
	}

	private Node nextSiblingThatsNotJustWhitespace(Node node) {
		do {
			node = node.nextSibling();
		} while (node.outerHtml().trim().isEmpty());

		return node;
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		var postedBySystemBot = message.userId() < 1;
		if (postedBySystemBot) {
			var m = regex.matcher(message.content().getContent());
			if (m.find()) {
				var comicId = Integer.parseInt(m.group(1));
				comicsByRoom.put(message.roomId(), new Comic(message, comicId));
			}
		}

		return doNothing();
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
			lastCheckedWiki = Now.instant();
			timesCheckedWiki++;
		}
	}
}
