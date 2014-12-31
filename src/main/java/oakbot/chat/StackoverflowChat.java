package oakbot.chat;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A connection to Stackoverflow chat.
 * @author Michael Angstadt
 * @see <a href="http://chat.stackoverflow.com">chat.stackoverflow.com</a>
 * @see <a
 * href="https://github.com/Zirak/SO-ChatBot/blob/master/source/adapter.js">Good
 * explanation of how SO Chat works</a>
 */
public class StackoverflowChat implements ChatConnection {
	private static final Logger logger = Logger.getLogger(StackoverflowChat.class.getName());

	private final HttpClient client;
	private final Pattern fkeyRegex = Pattern.compile("value=\"([0-9a-f]{32})\"");
	private final Map<Integer, String> fkeyCache = new HashMap<>();
	private final Map<Integer, Long> prevMessageIds = new HashMap<>();

	private final MessageSenderThread sender;

	/**
	 * Creates a new connection to Stackoverflow chat.
	 * @param client the HTTP client
	 */
	public StackoverflowChat(HttpClient client) {
		this(client, TimeUnit.SECONDS.toMillis(4));
	}

	/**
	 * Creates a new connection to Stackoverflow chat.
	 * @param client the HTTP client
	 * @param the amount of time to pause between message sends (in
	 * milliseconds)
	 */
	public StackoverflowChat(HttpClient client, long pauseBetweenMessages) {
		MessageSenderThread sender = new MessageSenderThread(pauseBetweenMessages);
		sender.start();

		this.client = client;
		this.sender = sender;
	}

	@Override
	public void login(String email, String password) throws IOException {
		logger.info("Logging in as " + email + "...");

		String fkey = parseFkey("https://stackoverflow.com/users/login");

		HttpPost request = new HttpPost("https://stackoverflow.com/users/login");
		//@formatter:off
		List<NameValuePair> params = Arrays.asList(
			new BasicNameValuePair("email", email),
			new BasicNameValuePair("password", password),
			new BasicNameValuePair("fkey", fkey)
		);
		//@formatter:on
		request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

		HttpResponse response = client.execute(request);
		int statusCode = response.getStatusLine().getStatusCode();
		EntityUtils.consumeQuietly(response.getEntity());
		if (statusCode != 302) {
			throw new IllegalArgumentException("Bad login");
		}
	}

	@Override
	public void sendMessage(int room, String message) throws IOException {
		sendMessage(room, message, SplitStrategy.NONE);
	}

	@Override
	public void sendMessage(int room, String message, SplitStrategy splitStrategy) throws IOException {
		sender.send(room, message, splitStrategy);
	}

	@Override
	public List<ChatMessage> getMessages(int room, int num) throws IOException {
		String fkey = getFKey(room);

		HttpPost request = new HttpPost("https://chat.stackoverflow.com/chats/" + room + "/events");
		//@formatter:off
		List<NameValuePair> params = Arrays.asList(
			new BasicNameValuePair("mode", "messages"),
			new BasicNameValuePair("msgCount", num + ""),
			new BasicNameValuePair("fkey", fkey)
		);
		//@formatter:on
		request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

		HttpResponse response = executeWithRetries(request, null);
		ObjectMapper mapper = new ObjectMapper();
		String json = EntityUtils.toString(response.getEntity());
		logger.finest(json);

		JsonNode node = mapper.readTree(json);
		JsonNode events = node.get("events");
		Iterator<JsonNode> it = events.elements();
		List<ChatMessage> messages = new ArrayList<>();
		while (it.hasNext()) {
			JsonNode element = it.next();
			ChatMessage chatMessage = parseChatMessage(element);
			messages.add(chatMessage);
		}

		return messages;
	}

	@Override
	public List<ChatMessage> getNewMessages(int room) throws IOException {
		Long prevMessageId = prevMessageIds.get(room);
		if (prevMessageId == null) {
			List<ChatMessage> messages = getMessages(room, 1);

			if (messages.isEmpty()) {
				prevMessageId = 0L;
			} else {
				ChatMessage last = messages.get(messages.size() - 1);
				prevMessageId = last.getMessageId();
			}
			prevMessageIds.put(room, prevMessageId);

			return Collections.emptyList();
		}

		//keep retrieving more and more messages until we got all of the ones that came in since we last pinged
		List<ChatMessage> messages;
		for (int count = 5; true; count += 5) {
			messages = getMessages(room, count);
			if (messages.isEmpty() || messages.get(0).getMessageId() <= prevMessageId) {
				break;
			}
		}

		//only return the new messages
		int pos = -1;
		for (int i = 0; i < messages.size(); i++) {
			ChatMessage message = messages.get(i);
			if (message.getMessageId() > prevMessageId) {
				pos = i;
				break;
			}
		}

		if (pos < 0) {
			return Collections.emptyList();
		}

		prevMessageIds.put(room, messages.get(messages.size() - 1).getMessageId());
		return messages.subList(pos, messages.size());
	}

	/**
	 * Parses the infamous "fkey" parameter from a webpage.
	 * @param url the URL of the webpage
	 * @return the fkey or null if not found
	 * @throws IOException if there's a problem loading the page
	 */
	private String parseFkey(String url) throws IOException {
		HttpGet request = new HttpGet(url);
		HttpResponse response = executeWithRetries(request, null);
		String html = EntityUtils.toString(response.getEntity());
		Matcher m = fkeyRegex.matcher(html);
		return m.find() ? m.group(1) : null;
	}

	/**
	 * Gets the "fkey" for a room
	 * @param room the room ID
	 * @return the fkey
	 * @throws IOException if there's a problem getting the fkey
	 */
	private synchronized String getFKey(int room) throws IOException {
		String fkey = fkeyCache.get(room);
		if (fkey == null) {
			fkey = parseFkey("https://chat.stackoverflow.com/rooms/" + room);
			fkeyCache.put(room, fkey);
		}
		return fkey;
	}

	/**
	 * Executes an HTTP request, retrying after a short pause if the request
	 * fails.
	 * @param request the request to send
	 * @param numRetries the number of times to retry the request if it fails,
	 * or null to retry forever
	 * @return the HTTP response
	 * @throws IOException if there was an I/O error
	 */
	private HttpResponse executeWithRetries(HttpUriRequest request, Integer numRetries) throws IOException {
		return executeWithRetries(request, numRetries, null);
	}

	/**
	 * Executes an HTTP request, retrying after a short pause if the request
	 * fails.
	 * @param request the request to send
	 * @param numRetries the number of times to retry the request if it fails,
	 * or null to retry forever
	 * @param expectedStatusCode the expected HTTP response status code. If the
	 * response does not have this status code, the request will be retried
	 * @return the HTTP response
	 * @throws IOException if there was an I/O error
	 */
	private HttpResponse executeWithRetries(HttpUriRequest request, Integer numRetries, Integer expectedStatusCode) throws IOException {
		int retries = 0;
		while (numRetries == null || retries <= numRetries) {
			int sleep = (retries + 1) * 5;
			if (sleep > 60) {
				sleep = 60;
			}

			try {
				HttpResponse response = client.execute(request);
				if (expectedStatusCode == null) {
					return response;
				}

				int actualStatusCode = response.getStatusLine().getStatusCode();
				if (expectedStatusCode == actualStatusCode) {
					return response;
				}

				logger.severe("Expected status code " + expectedStatusCode + ", but was " + actualStatusCode + ".  Trying again in " + sleep + " seconds.");
			} catch (NoHttpResponseException e) {
				logger.log(Level.SEVERE, "Could not send HTTP " + request.getMethod() + " request to " + request.getURI() + ".  Trying again in " + sleep + " seconds.", e);
			}

			try {
				TimeUnit.SECONDS.sleep(sleep);
			} catch (InterruptedException e) {
				throw new IOException("Sleep interrupted D:<", e);
			}

			retries++;
		}
		return null;
	}

	@Override
	public void flush() {
		sender.finish();
	}

	/**
	 * Unmarshals a chat message from its JSON representation.
	 * @param element the JSON element
	 * @return the parsed chat message
	 */
	private static ChatMessage parseChatMessage(JsonNode element) {
		ChatMessage chatMessage = new ChatMessage();

		JsonNode value = element.get("content");
		if (value != null) {
			chatMessage.setContent(value.asText());
		}

		value = element.get("edits");
		if (value != null) {
			chatMessage.setEdits(value.asInt());
		}

		value = element.get("message_id");
		if (value != null) {
			chatMessage.setMessageId(value.asLong());
		}

		value = element.get("room_id");
		if (value != null) {
			chatMessage.setRoomId(value.asInt());
		}

		value = element.get("time_stamp");
		if (value != null) {
			LocalDateTime ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(value.asLong() * 1000), ZoneId.systemDefault());
			chatMessage.setTimestamp(ts);
		}

		value = element.get("user_id");
		if (value != null) {
			chatMessage.setUserId(value.asInt());
		}

		value = element.get("user_name");
		if (value != null) {
			chatMessage.setUsername(value.asText());
		}

		return chatMessage;
	}

	private class MessageSenderThread extends Thread {
		private final int MAX_MESSAGE_LENGTH = 500;
		private final long pauseBetweenMessages;

		private volatile IOException thrown;
		private volatile boolean finish = false;
		private long prevMessageSent;
		private final BlockingQueue<ChatPost> messageQueue = new LinkedBlockingQueue<>();

		public MessageSenderThread(long pauseBetweenMessages) {
			this.pauseBetweenMessages = pauseBetweenMessages;
			setName(getClass().getSimpleName());
			setDaemon(true);
		}

		public void send(int room, String message, SplitStrategy splitStrategy) throws IOException {
			if (!isAlive()) {
				throw thrown;
			}

			messageQueue.add(new ChatPost(room, message, splitStrategy));
		}

		public void finish() {
			finish = true;
			interrupt();

			try {
				join();
			} catch (InterruptedException e) {
				//do nothing
			}
		}

		@Override
		public void run() {
			while (true) {
				if (finish && messageQueue.isEmpty()) {
					return;
				}

				ChatPost chatPost;
				try {
					chatPost = messageQueue.take();
				} catch (InterruptedException e) {
					if (finish && !messageQueue.isEmpty()) {
						continue;
					}
					return;
				}

				int room = chatPost.room;
				String message = chatPost.post;
				SplitStrategy splitStrategy = chatPost.splitStrategy;

				try {
					String fkey = getFKey(room);

					String url = "https://chat.stackoverflow.com/chats/" + room + "/messages/new";
					List<String> posts = splitStrategy.split(message, MAX_MESSAGE_LENGTH);

					for (String post : posts) {
						send(room, url, fkey, post);
					}
				} catch (IOException e) {
					thrown = e;
					break;
				}
			}
		}

		private void send(int room, String url, String fkey, String message) throws IOException {
			long now = System.currentTimeMillis();
			long sleep = pauseBetweenMessages - (now - prevMessageSent);
			if (sleep > 0) {
				try {
					TimeUnit.MILLISECONDS.sleep(sleep);
				} catch (InterruptedException e) {
					//sleep for the remaining time
					sleep -= System.currentTimeMillis() - now;
					if (sleep > 0) {
						try {
							TimeUnit.MILLISECONDS.sleep(sleep);
						} catch (InterruptedException e1) {
							//ignore
						}
					}
				}
			}

			logger.info("Posting message to room " + room + ": " + message);

			HttpPost request = new HttpPost(url);
			//@formatter:off
			List<NameValuePair> params = Arrays.asList(
				new BasicNameValuePair("text", message),
				new BasicNameValuePair("fkey", fkey)
			);
			//@formatter:on
			request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

			HttpResponse response = executeWithRetries(request, null, 200);
			EntityUtils.consumeQuietly(response.getEntity());
			logger.info("Message received.");

			prevMessageSent = System.currentTimeMillis();
		}
	}

	private static class ChatPost {
		private final int room;
		private final String post;
		private final SplitStrategy splitStrategy;

		public ChatPost(int room, String post, SplitStrategy splitStrategy) {
			this.room = room;
			this.post = post;
			this.splitStrategy = splitStrategy;
		}
	}
}