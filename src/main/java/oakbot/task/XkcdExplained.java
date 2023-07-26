package oakbot.task;

import static oakbot.bot.ChatActions.doNothing;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.HelpDoc;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;
import oakbot.util.Http;
import oakbot.util.HttpFactory;
import oakbot.util.Now;

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
		return new HelpDoc.Builder((Listener)this)
			.summary("Posts an explanation of an XKCD comic whenever one is posted to the room by a system bot.")
			.detail("Content from explainxkcd.com.")
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

				String url = url(comic.comicId);
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

				/**
				 * Ensure the content has no newlines. Newlines prevent the
				 * markdown conversion from happening.
				 */
				explanationHtml = explanationHtml.replaceAll("(\\r\\n|\\n|\\r)", " ");

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
			Duration postAge = Duration.between(comic.messageContainingComic.getTimestamp(), Now.local());
			return (postAge.compareTo(timeToWaitBeforeFirstWikiCheck) < 0);
		} else {
			Duration lastChecked = Duration.between(comic.lastCheckedWiki, Now.instant());
			return (lastChecked.compareTo(timeToWaitBetweenWikiChecks) < 0);
		}
	}

	private String scrapeExplanationHtml(String url) throws IOException {
		Document dom;
		try (Http http = HttpFactory.connect()) {
			Http.Response response = http.get(url);

			/*
			 * If a wiki page for the comic has not been created
			 * yet, the response status code will be 404.
			 */
			if (response.getStatusCode() != 200) {
				throw new IOException();
			}

			dom = response.getBodyAsHtml();
		}

		Node next = nextSiblingThatsNotJustWhitespace(dom.getElementById("Explanation").parentNode());

		/*
		 * A notice such as "This explanation may be incomplete" may be at the
		 * top of the Explanation section. These notices are inside of a <table>
		 * element.
		 */
		if (next instanceof Element) {
			Element element = (Element) next;
			if ("table".equals(element.tagName())) {
				next = nextSiblingThatsNotJustWhitespace(next);
			}
		}

		if (next instanceof Element) {
			Element element = (Element) next;

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
		StringBuilder firstParagraph = new StringBuilder();
		while (true) {
			firstParagraph.append(next.outerHtml());
			next = next.nextSibling();

			if (next instanceof Element) {
				Element element = (Element) next;
				if ("p".equals(element.tagName())) {
					break;
				}
			}
		}
		return firstParagraph.toString().trim();
	}

	private Node nextSiblingThatsNotJustWhitespace(Node node) {
		do {
			node = node.nextSibling();
		} while (node.outerHtml().trim().isEmpty());

		return node;
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
