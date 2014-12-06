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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A connection to Stackoverflow chat.
 * @author Michael Angstadt
 * @see <a href="http://chat.stackoverflow.com">chat.stackoverflow.com</a>
 */
public class StackoverflowChat implements ChatConnection {
	private static final Logger logger = Logger.getLogger(StackoverflowChat.class.getName());
	private static final int MAX_MESSAGE_LENGTH = 497;

	private final HttpClient client;
	private final Pattern fkeyRegex = Pattern.compile("value=\"([0-9a-f]{32})\"");
	private final Map<Integer, String> fkeyCache = new HashMap<>();
	private final Map<Integer, Long> prevMessageIds = new HashMap<>();

	/**
	 * Creates a new connection to Stackoverflow chat.
	 * @param client the HTTP client
	 */
	public StackoverflowChat(HttpClient client) {
		this.client = client;
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
		List<String> posts = new ArrayList<>();
		while (message.length() > MAX_MESSAGE_LENGTH) {
			int pos = message.lastIndexOf(' ', MAX_MESSAGE_LENGTH);
			if (pos < 0) {
				posts.add(message.substring(0, MAX_MESSAGE_LENGTH) + "...");
				message = message.substring(MAX_MESSAGE_LENGTH);
			} else {
				posts.add(message.substring(0, pos) + "...");
				message = message.substring(pos + 1);
			}
		}
		posts.add(message);

		String fkey = getFKey(room);

		String url = "https://chat.stackoverflow.com/chats/" + room + "/messages/new";
		Iterator<String> it = posts.iterator();
		while (it.hasNext()) {
			String post = it.next();
			logger.info("Posting message to room " + room + ": " + post);

			HttpPost request = new HttpPost(url);
			//@formatter:off
			List<NameValuePair> params = Arrays.asList(
				new BasicNameValuePair("text", post),
				new BasicNameValuePair("fkey", fkey)
			);
			//@formatter:on
			request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

			HttpResponse response = client.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			EntityUtils.consumeQuietly(response.getEntity());
			if (statusCode != 200) {
				throw new IOException("Problem sending message: HTTP " + statusCode);
			}
			logger.info("Message received.");

			//an HTTP 409 response is returned if messages are sent too quickly
			if (it.hasNext()) {
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
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

		HttpResponse response = client.execute(request);
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
		HttpResponse response = client.execute(request);
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
	private String getFKey(int room) throws IOException {
		String fkey = fkeyCache.get(room);
		if (fkey == null) {
			fkey = parseFkey("https://chat.stackoverflow.com/rooms/" + room);
			fkeyCache.put(room, fkey);
		}
		return fkey;
	}

	/**
	 * Unmarshals a chat message from its JSON representation.
	 * @param element the JSON element
	 * @return the parsed chat message
	 */
	private ChatMessage parseChatMessage(JsonNode element) {
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
}