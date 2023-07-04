package oakbot.task;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import oakbot.bot.Bot;
import oakbot.bot.PostMessage;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.util.ChatBuilder;

/**
 * Posts an explanation of the latest XKCD comic from explainxkcd.com.
 * @author Michael Angstadt
 */
public class XkcdExplainTask implements ScheduledTask {
	private final long timeToWaitBeforePosting;

	private long nextRun = TimeUnit.MINUTES.toMillis(1);
	private ChatMessage comicMessage;
	private Integer comicId;
	private boolean waitedForWikiToUpdate = false;

	/**
	 * @param timeToWaitBeforePosting the amount time to wait after the comic is
	 * posted to the chat room before crawling explainxkcd.com for the
	 * explanation (milliseconds)
	 */
	public XkcdExplainTask(long timeToWaitBeforePosting) {
		this.timeToWaitBeforePosting = timeToWaitBeforePosting;
	}

	@Override
	public synchronized long nextRun() {
		return nextRun;
	}

	@Override
	public synchronized void run(Bot bot) throws Exception {
		boolean noComicsPostedYet = (comicId == null);
		if (noComicsPostedYet) {
			return;
		}

		if (!waitedForWikiToUpdate) {
			nextRun = timeToWaitBeforePosting;
			waitedForWikiToUpdate = true;
			return;
		}

		try {
			String message = getExplainationMessage(comicId, comicMessage.getMessageId());
			PostMessage postMessage = new PostMessage(message);
			bot.sendMessage(comicMessage.getRoomId(), postMessage);
		} finally {
			comicId = null;
			comicMessage = null;
			waitedForWikiToUpdate = false;
			nextRun = TimeUnit.MINUTES.toMillis(1);
		}
	}

	/**
	 * This method is called when a new comic has been posted.
	 * @param comicId the ID of the comic
	 * @param comicMessage the chat message that contains the comic
	 */
	public synchronized void comicPosted(int comicId, ChatMessage comicMessage) {
		this.comicId = comicId;
		this.comicMessage = comicMessage;
	}

	/**
	 * Makes an HTTP GET request to the given URL. This method is
	 * package-private so it can be overridden in unit tests.
	 * @param url the URL
	 * @return the response body
	 * @throws IOException if there's a network problem
	 */
	String get(String url) throws IOException {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(url);
			try (CloseableHttpResponse response = client.execute(request)) {
				return EntityUtils.toString(response.getEntity());
			}
		}
	}

	/**
	 * Gets the message to post to the chat room that contains the explanation
	 * of the comic. This method is package-private so it can be unit tested.
	 * @param comicId the comic ID
	 * @param messageId the message ID of the chat message that contains the
	 * comic
	 * @return the message to post
	 * @throws IOException if there's a problem getting the explanation from the
	 * wiki
	 * @throws IllegalArgumentException if the explanation text cannot be
	 * located within the wiki's HTML page
	 */
	String getExplainationMessage(int comicId, long messageId) throws IOException {
		String url = "https://www.explainxkcd.com/wiki/index.php/" + comicId;
		Document html = Jsoup.parse(get(url));
		Element firstParagraph = html.selectFirst("p");
		if (firstParagraph == null) {
			throw new IllegalArgumentException("Unable to locate first paragraph of 'XKCD Explained' for comic " + comicId + ".");
		}

		String replyMd = new ChatBuilder().reply(messageId).toString();

		String explainationHtml = firstParagraph.html();
		String explainationMd = ChatBuilder.toMarkdown(explainationHtml, false);

		String readMoreMd = new ChatBuilder().append(" ").link("Read more", url).toString();

		int trimLength = 500 - replyMd.length() - readMoreMd.length(); //max message length is 500

		explainationMd = SplitStrategy.WORD.split(explainationMd, trimLength).get(0);
		return replyMd + explainationMd + readMoreMd;
	}
}
