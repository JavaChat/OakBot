package oakbot.chat;

import java.io.IOException;
import java.net.SocketException;
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

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A connection to Stackoverflow chat.
 * @author Michael Angstadt
 * @see <a href="http://chat.stackoverflow.com">chat.stackoverflow.com</a>
 * @see <a href=
 * "https://github.com/Zirak/SO-ChatBot/blob/master/source/adapter.js">Good
 * explanation of how SO Chat works</a>
 */
public class StackoverflowChat implements ChatConnection {
	private static final Logger logger = Logger.getLogger(StackoverflowChat.class.getName());

	private final CloseableHttpClient client;
	private final Pattern fkeyRegex = Pattern.compile("value=\"([0-9a-f]{32})\"");
	private final Map<Integer, String> fkeyCache = new HashMap<>();
	private final Map<Integer, Long> prevMessageIds = new HashMap<>();
	private final MessageSenderThread sender;
	private final long retryPause, heartbeat;
	private boolean closed;

	/**
	 * Creates a polling connection to Stackoverflow chat.
	 * @param client the HTTP client
	 */
	public StackoverflowChat(CloseableHttpClient client) {
		this(client, 5000, 3000);
	}

	/**
	 * Creates a polling connection to Stackoverflow chat.
	 * @param client the HTTP client
	 * @param retryPause the base amount of time to wait in between request
	 * retries (in milliseconds)
	 * @param heartbeat how often to poll each chat room looking for new
	 * messages (in milliseconds)
	 */
	public StackoverflowChat(CloseableHttpClient client, long retryPause, long heartbeat) {
		this.client = client;
		this.retryPause = retryPause;
		this.heartbeat = heartbeat;

		MessageSenderThread sender = new MessageSenderThread();
		sender.start();
		this.sender = sender;
	}

	@Override
	public void login(String email, String password) throws IOException {
		logger.info("Logging in as " + email + "...");

		String fkey = parseFkeyFromUrl("https://stackoverflow.com/users/login");
		if (fkey == null) {
			throw new IOException("\"fkey\" field not found on page, cannot login.");
		}

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
			throw new InvalidCredentialsException();
		}
	}

	@Override
	public void joinRoom(int roomId) throws RoomNotFoundException, RoomPermissionException, IOException {
		/*
		 * Checks if the room exists and if the bot user can post messages to
		 * it. Then primes the "previous message ID" counter
		 */
		getNewMessages(roomId);
	}

	@Override
	public void leaveRoom(int roomId) throws IOException {
		fkeyCache.remove(roomId);
		prevMessageIds.remove(roomId);
	}

	@Override
	public void sendMessage(int roomId, String message) throws IOException {
		sendMessage(roomId, message, SplitStrategy.NONE);
	}

	@Override
	public void sendMessage(int roomId, String message, SplitStrategy splitStrategy) throws IOException {
		String fkey = getFKey(roomId);
		sender.send(roomId, fkey, message, splitStrategy);
	}

	@Override
	public List<ChatMessage> getMessages(int roomId, int count) throws IOException {
		String fkey = getFKey(roomId);

		HttpPost request = new HttpPost("https://chat.stackoverflow.com/chats/" + roomId + "/events");
		//@formatter:off
		List<NameValuePair> params = Arrays.asList(
			new BasicNameValuePair("mode", "messages"),
			new BasicNameValuePair("msgCount", count + ""),
			new BasicNameValuePair("fkey", fkey)
		);
		//@formatter:on
		request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

		JsonNode node = executeWithRetriesJson(request);
		JsonNode events = node.get("events");
		if (events == null) {
			return Collections.emptyList();
		}

		Iterator<JsonNode> it = events.elements();
		List<ChatMessage> messages = new ArrayList<>();
		while (it.hasNext()) {
			JsonNode element = it.next();
			ChatMessage chatMessage = parseChatMessage(element);
			messages.add(chatMessage);
		}

		return messages;
	}

	private List<ChatMessage> getNewMessages(int room) throws IOException {
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

	@Override
	public void listen(ChatMessageHandler handler) throws IOException {
		while (true) {
			long start = System.currentTimeMillis();

			/*
			 * Make a copy of the room list to prevent concurrent modification
			 * exceptions.
			 */
			List<Integer> rooms = new ArrayList<>(fkeyCache.keySet());

			for (Integer room : rooms) {
				logger.fine("Pinging room " + room);

				//get new messages since last ping
				List<ChatMessage> newMessages = getNewMessages(room);
				logger.fine(newMessages.size() + " new messages found.");
				if (newMessages.isEmpty()) {
					//no new messages
					continue;
				}

				for (ChatMessage message : newMessages) {
					handler.handle(message);
					if (closed) {
						return;
					}
				}

				ChatMessage latestMessage = newMessages.get(newMessages.size() - 1);
				prevMessageIds.put(room, latestMessage.getMessageId());
			}

			//sleep before pinging again
			long elapsed = System.currentTimeMillis() - start;
			long sleep = heartbeat - elapsed;
			if (sleep > 0) {
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	/**
	 * Parses the "fkey" parameter from a webpage.
	 * @param url the URL of the webpage
	 * @return the fkey or null if not found
	 * @throws IOException if there's a problem loading the page
	 */
	private String parseFkeyFromUrl(String url) throws IOException {
		HttpGet request = new HttpGet(url);
		HttpResponse response = executeWithRetries(request);
		if (response == null) {
			throw new IOException("Couldn't load page.");
		}

		String html = EntityUtils.toString(response.getEntity());
		return parseFkey(html);
	}

	/**
	 * Parses the "fkey" parameter from a given HTML page.
	 * @param html the HTML page
	 * @return the fkey or null if not found
	 */
	private String parseFkey(String html) {
		Matcher m = fkeyRegex.matcher(html);
		return m.find() ? m.group(1) : null;
	}

	/**
	 * Gets the "fkey" parameter for a room.
	 * @param room the room ID
	 * @return the fkey
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if messages cannot be posted to this room
	 * @throws IOException if there's a problem connecting to the room
	 */
	private String getFKey(int room) throws IOException {
		String fkey = fkeyCache.get(room);
		if (fkey != null) {
			return fkey;
		}

		String roomUrl = "https://chat.stackoverflow.com/rooms/" + room;
		HttpGet request = new HttpGet(roomUrl);
		HttpResponse response = executeWithRetries(request);
		if (response == null) {
			throw new RoomNotFoundException(room);
		}

		String html = EntityUtils.toString(response.getEntity());
		fkey = parseFkey(html);
		if (fkey == null) {
			throw new IOException("Cannot get room's fkey.");
		}

		if (!canPostToRoom(html)) {
			throw new RoomPermissionException(room);
		}

		fkeyCache.put(room, fkey);
		return fkey;
	}

	/**
	 * Determines if messages can be posted to a room. A room can be inactive,
	 * which means it does not accept new messages. A room can also be
	 * protected, which means only approved users can post.
	 * @param html the HTML of the room
	 * @return true if messages can be posted, false if not
	 */
	private boolean canPostToRoom(String html) {
		/*
		 * The textbox for sending messages won't be there if the bot user can't
		 * post to the room.
		 */
		return html.contains("<textarea id=\"input\">");
	}

	/**
	 * Executes an HTTP request whose response is expected to be JSON. The
	 * request is retried if it fails.
	 * @param request the request to send
	 * @return the parsed JSON response
	 * @throws IOException if there was an I/O error
	 */
	private JsonNode executeWithRetriesJson(HttpUriRequest request) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		while (true) {
			HttpResponse response = executeWithRetries(request);
			try {
				return mapper.readTree(response.getEntity().getContent());
			} catch (JsonParseException e) {
				//make the request again if a non-JSON response is returned
				logger.log(Level.SEVERE, "Could not parse the response as a JSON object.  Retrying the request in " + retryPause + "ms.", e);
				try {
					Thread.sleep(retryPause);
				} catch (InterruptedException e2) {
					throw new IOException(e2);
				}
			}
		}
	}

	/**
	 * Executes an HTTP request, retrying after a short pause if the request
	 * fails.
	 * @param request the request to send
	 * @return the HTTP response, or null if it was a 404 response, or null if
	 * the request couldn't be executed
	 * @throws IOException if there was an I/O error
	 */
	private HttpResponse executeWithRetries(HttpUriRequest request) throws IOException {
		return executeWithRetries(request, null, null);
	}

	/**
	 * Executes an HTTP request, retrying after a short pause if the request
	 * fails.
	 * @param request the request to send
	 * @param numRetries the number of times to retry the request if it fails,
	 * or null to retry forever
	 * @param expectedStatusCode the expected HTTP response status code. If the
	 * response does not have this status code, the request will be retried
	 * @return the HTTP response, or null if it was a 404 response, or null if
	 * the request couldn't be executed
	 * @throws IOException if there was an I/O error
	 */
	private HttpResponse executeWithRetries(HttpUriRequest request, Integer numRetries, Integer expectedStatusCode) throws IOException {
		int attempts = 0;
		long sleep = 0;
		final long maxSleep = TimeUnit.SECONDS.toMillis(60);
		while (numRetries == null || attempts <= numRetries) {
			attempts++;
			if (sleep > 0) {
				try {
					logger.info("Sleeping for " + sleep + " ms before resending the request...");
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					logger.log(Level.INFO, "Sleep interrupted.", e);
					throw new IOException("Sleep interrupted.", e);
				}
			}

			sleep = (attempts + 1) * retryPause;
			if (sleep > maxSleep) {
				sleep = maxSleep;
			}

			HttpResponse response;
			try {
				response = client.execute(request);
			} catch (NoHttpResponseException | SocketException | ConnectTimeoutException | SSLHandshakeException e) {
				logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown from request " + request.getURI() + ".", e);
				continue;
			}

			int actualStatusCode = response.getStatusLine().getStatusCode();
			if (actualStatusCode == 409) {
				//"You can perform this action again in 2 seconds"
				Long sleepValue = parse409Response(response);
				sleep = (sleepValue == null) ? 5000 : sleepValue;
				continue;
			}

			if (actualStatusCode == 404) {
				//chat room does not exist or cannot be posted to
				logger.severe("404 response received from request URI " + request.getURI() + ".");
				return null;
			}

			if (expectedStatusCode != null && expectedStatusCode != actualStatusCode) {
				logger.severe("Expected status code " + expectedStatusCode + ", but was " + actualStatusCode + ".");
				continue;
			}

			return response;
		}
		return null;
	}

	/**
	 * Parses an HTTP 409 response, which indicates that the bot is sending
	 * messages too quickly.
	 * @param response the HTTP 409 response
	 * @return the amount of time (in milliseconds) the bot must wait before SO
	 * Chat will continue to accept chat messages, or null if this value could
	 * not be parsed from the response
	 * @throws IOException if there's a problem getting the response body
	 */
	private Long parse409Response(HttpResponse response) throws IOException {
		//"You can perform this action again in 2 seconds"
		String body = EntityUtils.toString(response.getEntity());
		logger.fine("409 response received: " + body);

		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(body);
		if (!m.find()) {
			return null;
		}

		int seconds = Integer.parseInt(m.group(0));
		return TimeUnit.SECONDS.toMillis(seconds);
	}

	@Override
	public void close() throws IOException {
		flush();

		try {
			client.close();
		} finally {
			closed = true;
		}
	}

	@Override
	public void flush() {
		sender.flush();
	}

	/**
	 * Unmarshals a chat message from its JSON representation.
	 * @param element the JSON element
	 * @return the parsed chat message
	 */
	private static ChatMessage parseChatMessage(JsonNode element) {
		ChatMessage.Builder builder = new ChatMessage.Builder();

		JsonNode value = element.get("content");
		if (value != null) {
			String content = StringEscapeUtils.unescapeHtml4(value.asText());
			builder.content(content);
		}

		value = element.get("edits");
		if (value != null) {
			builder.edits(value.asInt());
		}

		value = element.get("message_id");
		if (value != null) {
			builder.messageId(value.asLong());
		}

		value = element.get("room_id");
		if (value != null) {
			builder.roomId(value.asInt());
		}

		value = element.get("time_stamp");
		if (value != null) {
			LocalDateTime ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(value.asLong() * 1000), ZoneId.systemDefault());
			builder.timestamp(ts);
		}

		value = element.get("user_id");
		if (value != null) {
			builder.userId(value.asInt());
		}

		value = element.get("user_name");
		if (value != null) {
			builder.username(value.asText());
		}

		return builder.build();
	}

	private class MessageSenderThread extends Thread {
		private final int MAX_MESSAGE_LENGTH = 500;
		private final BlockingQueue<ChatPost> messageQueue = new LinkedBlockingQueue<>();
		private final ChatPost LAST = new ChatPost(0, null, null, null);

		public MessageSenderThread() {
			setName(getClass().getSimpleName());
			setDaemon(true);
		}

		public void send(int room, String fkey, String message, SplitStrategy splitStrategy) throws IOException {
			messageQueue.add(new ChatPost(room, fkey, message, splitStrategy));
		}

		public void flush() {
			messageQueue.add(LAST);

			try {
				join();
			} catch (InterruptedException e) {
				//do nothing
			}
		}

		@Override
		public void run() {
			while (true) {
				ChatPost chatPost;
				try {
					chatPost = messageQueue.take();
				} catch (InterruptedException e) {
					return;
				}

				if (chatPost == LAST) {
					return;
				}

				int room = chatPost.roomId;
				String fkey = chatPost.fkey;
				String message = chatPost.message;
				SplitStrategy splitStrategy = chatPost.splitStrategy;

				try {
					String url = "https://chat.stackoverflow.com/chats/" + room + "/messages/new";

					List<String> posts;
					if (message.contains("\n")) {
						//messages with newlines have no length limit
						posts = Arrays.asList(message);
					} else {
						posts = splitStrategy.split(message, MAX_MESSAGE_LENGTH);
					}

					for (String post : posts) {
						send(room, url, fkey, post);
					}
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Problem sending message.  Skipping to next message in queue.", e);
				}
			}
		}

		private void send(int room, String url, String fkey, String message) throws IOException {
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
			if (response == null) {
				return;
			}

			EntityUtils.consumeQuietly(response.getEntity());
			logger.info("Message received.");
		}
	}

	private static class ChatPost {
		private final int roomId;

		/**
		 * The room's fkey is stored here incase the bot leaves a room while the
		 * message queue still has messages waiting to be sent to that room.
		 */
		private final String fkey;

		private final String message;
		private final SplitStrategy splitStrategy;

		public ChatPost(int roomId, String fkey, String message, SplitStrategy splitStrategy) {
			this.roomId = roomId;
			this.fkey = fkey;
			this.message = message;
			this.splitStrategy = splitStrategy;
		}
	}
}