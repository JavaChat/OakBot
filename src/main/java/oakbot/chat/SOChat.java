package oakbot.chat;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An API for Stackoverflow Chat.
 * @author Michael Angstadt
 */
public class SOChat {
	private static final Logger logger = Logger.getLogger(SOChat.class.getName());
	private static final int MAX_MESSAGE_LENGTH = 497;

	private final Pattern fkeyRegex = Pattern.compile("value=\"([0-9a-f]{32})\"");
	private final HttpClient client = HttpClientBuilder.create().build();
	private final Map<Integer, String> fkeyCache = new HashMap<>();

	/**
	 * Logs into SO Chat.
	 * @param email the login email
	 * @param password the login password
	 * @throws IOException if there's a problem logging in
	 * @throws IllegalArgumentException if the login credentials are bad
	 */
	public void login(String email, String password) throws IOException {
		logger.info("Logging in as " + email + "...");

		String fkey = parseFkey("https://stackoverflow.com/users/login");

		HttpPost request = new HttpPost("https://stackoverflow.com/users/login");

		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("email", email));
		params.add(new BasicNameValuePair("password", password));
		params.add(new BasicNameValuePair("fkey", fkey));

		request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() != 302) {
			throw new IllegalArgumentException("Bad login");
		}
	}

	public void postMessage(int room, String message) throws IOException {
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

		Iterator<String> it = posts.iterator();
		while (it.hasNext()) {
			String post = it.next();
			logger.info("Posting message to room " + room + ": " + post);

			HttpPost request = new HttpPost("https://chat.stackoverflow.com/chats/" + room + "/messages/new");

			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("text", post));
			params.add(new BasicNameValuePair("fkey", fkey));

			request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

			HttpResponse response = client.execute(request);
			int code = response.getStatusLine().getStatusCode();
			logger.info("Response code: " + code);
			EntityUtils.consumeQuietly(response.getEntity());

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

	public List<ChatMessage> getMessages(int room, int num) throws IOException {
		String fkey = getFKey(room);

		HttpPost request = new HttpPost("https://chat.stackoverflow.com/chats/" + room + "/events");

		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("mode", "messages"));
		params.add(new BasicNameValuePair("msgCount", "5"));
		params.add(new BasicNameValuePair("fkey", fkey));

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

	private String getFKey(int room) throws IOException {
		String fkey = fkeyCache.get(room);
		if (fkey == null) {
			fkey = parseFkey("https://chat.stackoverflow.com/rooms/" + room);
			fkeyCache.put(room, fkey);
		}
		return fkey;
	}

	private ChatMessage parseChatMessage(JsonNode element) {
		//example object:
		//{"event_type":1,"time_stamp":1417041460,"content":"test","user_id":13379,"user_name":"Michael","room_id":1,"message_id":20157245}
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