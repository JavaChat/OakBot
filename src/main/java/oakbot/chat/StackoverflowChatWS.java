package oakbot.chat;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.stream.Collectors;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Builder;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oakbot.util.JsonUtils;

/**
 * A connection to Stack Overflow Chat that uses web sockets to retrieve new
 * messages. This class is thread-safe (fingers crossed).
 * @author Michael Angstadt
 * @see <a href="http://chat.stackoverflow.com">chat.stackoverflow.com</a>
 * @see <a href=
 * "https://github.com/Zirak/SO-ChatBot/blob/master/source/adapter.js">Good
 * explanation of how SO Chat works</a>
 */
public class StackoverflowChatWS implements ChatConnection {
	private static final Logger logger = Logger.getLogger(StackoverflowChatWS.class.getName());
	private static final String DOMAIN = "stackoverflow.com";
	private static final String CHAT_DOMAIN = "https://chat." + DOMAIN;
	private static final int MAX_MESSAGE_LENGTH = 500;
	private static final Pattern fkeyRegex = Pattern.compile("value=\"([0-9a-f]{32})\"");

	private final Http http;
	private final WebSocketContainer webSocketClient;

	private final Map<Integer, String> fkeyCache = new HashMap<>();

	private final Map<Integer, Session> websocketSessions = new HashMap<>();

	private final BlockingQueue<ChatMessage> newMessages = new LinkedBlockingQueue<>();
	private final ChatMessage CLOSE_MESSAGE = new ChatMessage.Builder().build();

	/**
	 * Creates a web socket connection to Stack Overflow Chat.
	 * @param httpClient the HTTP client (used to interact to the chat room)
	 * @param webSocketClient the web socket client (used to listen for new
	 * messages)
	 */
	public StackoverflowChatWS(CloseableHttpClient httpClient, WebSocketContainer webSocketClient) {
		this.http = new Http(httpClient);
		this.webSocketClient = webSocketClient;
	}

	@Override
	public void login(String email, String password) throws InvalidCredentialsException, IOException {
		String fkey = parseFkeyFromUrl("https://" + DOMAIN + "/users/login");
		if (fkey == null) {
			throw new IOException("\"fkey\" field not found on login page, cannot login.");
		}

		//@formatter:off
		Response response = http.post("https://" + DOMAIN + "/users/login",
			"email", email,
			"password", password,
			"fkey", fkey
		);
		//@formatter:on

		int statusCode = response.getStatusCode();
		if (statusCode != 302) {
			throw new InvalidCredentialsException();
		}
	}

	@Override
	public void joinRoom(int roomId) throws RoomNotFoundException, RoomPermissionException, IOException {
		if (isInRoom(roomId)) {
			//already joined
			return;
		}

		String websocketUrl = getWebsocketUrl(roomId);

		Builder configBuilder = ClientEndpointConfig.Builder.create();
		configBuilder.configurator(new Configurator() {
			@Override
			public void beforeRequest(Map<String, List<String>> headers) {
				headers.put("Origin", Arrays.asList(CHAT_DOMAIN));
			}
		});

		Session session;
		try {
			logger.info("Connecting to web socket [room=" + roomId + "]: " + websocketUrl);
			session = webSocketClient.connectToServer(new Endpoint() {
				private final ObjectMapper mapper = new ObjectMapper();

				@Override
				public void onOpen(Session session, EndpointConfig config) {
					session.addMessageHandler(String.class, (json) -> {
						JsonNode node;
						try {
							node = mapper.readTree(json);
						} catch (IOException e) {
							logger.log(Level.SEVERE, "Problem parsing JSON from web socket: " + json, e);
							return;
						}

						if (logger.isLoggable(Level.FINE)) {
							logger.fine("[room " + roomId + "]: Received message:\n" + JsonUtils.prettyPrint(node) + "\n");
						}

						JsonNode roomNode = node.get("r" + roomId);
						if (roomNode == null) {
							return;
						}

						JsonNode eventsNode = roomNode.get("e");
						if (eventsNode == null || !eventsNode.isArray()) {
							return;
						}

						for (JsonNode eventNode : eventsNode) {
							JsonNode eventTypeNode = eventNode.get("event_type");
							if (eventTypeNode == null) {
								continue;
							}

							int eventType = eventTypeNode.asInt();
							if (eventType != 1 && eventType != 2) {
								continue;
							}

							ChatMessage message = ChatMessageParser.fromWebSocket(eventNode);
							newMessages.add(message);
						}
					});
				}

				@Override
				public void onError(Session session, Throwable t) {
					logger.log(Level.SEVERE, "Could not connect to web socket [room=" + roomId + "].", t);
				}
			}, configBuilder.build(), new URI(websocketUrl));

			logger.info("Web socket connection successful [room=" + roomId + "]: " + websocketUrl);
		} catch (DeploymentException | URISyntaxException e) {
			throw new IOException(e);
		}

		synchronized (this) {
			websocketSessions.put(roomId, session);
		}
	}

	private String getWebsocketUrl(int roomId) throws IOException {
		String fkey = getFKey(roomId);

		//@formatter:off
		Response response = http.post(CHAT_DOMAIN + "/ws-auth",
			"roomid", roomId,
			"fkey", fkey
		);
		//@formatter:on

		String url = response.getBodyAsJson().get("url").asText();

		List<ChatMessage> messages = getMessages(roomId, 1);
		ChatMessage latest = messages.isEmpty() ? null : messages.get(0);
		long time = (latest == null) ? 0 : latest.getTimestamp().toEpochSecond(ZoneOffset.UTC);
		return url + "?l=" + time;
	}

	@Override
	public void leaveRoom(int roomId) {
		String fkey;
		synchronized (this) {
			if (!isInRoom(roomId)) {
				return;
			}

			fkey = fkeyCache.get(roomId);
		}

		/*
		 * If we get this far, then it should always have an fkey, but check to
		 * be sure.
		 */
		if (fkey == null) {
			return;
		}

		/*
		 * Send a leave request to the room. It's not crucial that this request
		 * succeed because it doesn't really do much. All it does is make the
		 * bot's user portrait disappear from the room list (however, it only
		 * disappears if you refresh the web browser). You'd think it would play
		 * that "fall & fade away" animation that you see when someone leaves,
		 * but it doesn't.
		 */
		try {
			//@formatter:off
			http.post(CHAT_DOMAIN + "/chats/leave/" + roomId,
				"quiet", "true", //setting this parameter to "false" results in an error
				"fkey", fkey
			);
			//@formatter:on
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem leaving room " + roomId + ".", e);
		}

		/*
		 * The fkey does not need to be removed from the fkey cache--fkeys stay
		 * the same during the entire login session.
		 */
		//fkeyCache.remove(roomId);

		Session session;
		synchronized (this) {
			session = websocketSessions.remove(roomId);
		}

		try {
			session.close();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem closing websocket for room " + roomId + ".", e);
		}
	}

	@Override
	public long sendMessage(int roomId, String message) throws IOException {
		return sendMessage(roomId, message, SplitStrategy.NONE).get(0);
	}

	@Override
	public List<Long> sendMessage(int roomId, String message, SplitStrategy splitStrategy) throws IOException {
		String fkey = getFKey(roomId);

		List<String> parts;
		if (message.contains("\n")) {
			//messages with newlines have no length limit
			parts = Arrays.asList(message);
		} else {
			parts = splitStrategy.split(message, MAX_MESSAGE_LENGTH);
		}

		List<Long> messageIds = new ArrayList<>(parts.size());
		for (String part : parts) {
			//@formatter:off
			Response response = http.post(CHAT_DOMAIN + "/chats/" + roomId + "/messages/new",
				"text", part,
				"fkey", fkey
			);
			//@formatter:on

			if (response.getStatusCode() == 404) {
				/*
				 * If we got this far, it means the room has an fkey, which
				 * means the room exists. So if a 404 response is returned when
				 * trying to send a message, it more likely means that the bot's
				 * permission to post messages has been revoked.
				 * 
				 * If a 404 response is returned from this request, the response
				 * body reads:
				 * "The room does not exist, or you do not have permission"
				 */
				throw new RoomNotFoundException(roomId);
			}

			JsonNode body = response.getBodyAsJson();
			long id = body.get("id").asLong();
			messageIds.add(id);
		}

		return messageIds;
	}

	@Override
	public List<ChatMessage> getMessages(int roomId, int count) throws IOException {
		String fkey = getFKey(roomId);

		//@formatter:off
		Response response = http.post(CHAT_DOMAIN + "/chats/" + roomId + "/events",
			"mode", "messages",
			"msgCount", count,
			"fkey", fkey
		);
		//@formatter:on

		if (response.getStatusCode() == 404) {
			throw new RoomNotFoundException(roomId);
		}

		JsonNode body = response.getBodyAsJson();
		JsonNode events = body.get("events");

		List<ChatMessage> messages;
		if (events == null) {
			messages = new ArrayList<>(0);
		} else {
			messages = new ArrayList<>();
			Iterator<JsonNode> it = events.elements();
			while (it.hasNext()) {
				JsonNode element = it.next();
				ChatMessage chatMessage = ChatMessageParser.fromHttp(element);
				messages.add(chatMessage);
			}
		}

		return messages;
	}

	@Override
	public boolean deleteMessage(int roomId, long messageId) throws IOException {
		String fkey = getFKey(roomId);

		//@formatter:off
		Response response = http.post(CHAT_DOMAIN + "/messages/" + messageId + "/delete",
			"fkey", fkey
		);
		//@formatter:on

		//@formatter:off
		/*
		 * An HTTP 302 response is returned if the message ID does not reference any
		 * message that has ever existed before.
		 * 
		 * The response body may contain the following strings (note that the double quotes are PART OF THE RESPONSE BODY):
		 * "ok"
		 * "This message has already been deleted."
		 * "It is too late to delete this message"
		 * "You can only delete your own messages"
		 */
		//@formatter:on

		return response.getBody().equals("\"ok\"");
	}

	@Override
	public boolean editMessage(int roomId, long messageId, String updatedMessage) throws IOException {
		String fkey = getFKey(roomId);

		//@formatter:off
		Response response = http.post(CHAT_DOMAIN + "/messages/" + messageId,
			"text", updatedMessage,
			"fkey", fkey
		);
		//@formatter:on

		//@formatter:off
		/*
		 * The response body may contain the following strings (note that the double quotes are PART OF THE RESPONSE BODY):
		 * "ok"
		 * "This message has already been deleted and cannot be edited"
		 * "It is too late to edit this message."
		 * "You can only edit your own messages"
		 */
		//@formatter:on

		return response.getBody().equals("\"ok\"");
	}

	@Override
	public List<UserInfo> getUserInfo(int roomId, List<Integer> userIds) throws IOException {
		//@formatter:off
		Response response = http.post(CHAT_DOMAIN + "/user/info",
			"ids", StringUtils.join(userIds, ","),
			"roomId", roomId
		);
		//@formatter:on

		if (response.getStatusCode() == 404) {
			return null;
		}

		JsonNode usersNode = response.getBodyAsJson().get("users");
		if (usersNode == null) {
			return null;
		}

		List<UserInfo> users = new ArrayList<>(usersNode.size());
		for (JsonNode userNode : usersNode) {
			UserInfo.Builder builder = new UserInfo.Builder();
			builder.userId(userNode.get("id").asInt());
			builder.roomId(roomId);
			builder.username(userNode.get("name").asText());

			String profilePicture;
			String emailHash = userNode.get("email_hash").asText();
			if (emailHash.startsWith("!")) {
				profilePicture = emailHash.substring(1);
			} else {
				profilePicture = "https://www.gravatar.com/avatar/" + emailHash + "?d=identicon&s=128";
			}
			builder.profilePicture(profilePicture);

			builder.reputation(userNode.get("reputation").asInt());
			builder.moderator(userNode.get("is_moderator").asBoolean());
			builder.owner(userNode.get("is_owner").asBoolean());
			builder.lastPost(timestamp(userNode.get("last_post").asLong()));
			builder.lastSeen(timestamp(userNode.get("last_seen").asLong()));

			users.add(builder.build());
		}
		return users;
	}

	@Override
	public List<PingableUser> getPingableUsers(int roomId) throws IOException {
		Response response = http.get(CHAT_DOMAIN + "/rooms/pingable/" + roomId);

		JsonNode root = response.getBodyAsJson();
		List<PingableUser> users = new ArrayList<>(root.size());
		for (JsonNode node : root) {
			if (!node.isArray() || node.size() < 4) {
				continue;
			}

			long userId = node.get(0).asLong();
			String username = node.get(1).asText();
			LocalDateTime lastPost = timestamp(node.get(3).asLong());

			users.add(new PingableUser(roomId, userId, username, lastPost));
		}
		return users;
	}

	@Override
	public RoomInfo getRoomInfo(int roomId) throws IOException {
		Response response = http.get(CHAT_DOMAIN + "/rooms/thumbs/" + roomId);
		if (response.getStatusCode() == 404) {
			return null;
		}

		JsonNode root = response.getBodyAsJson();

		int id = root.get("id").asInt();
		String name = root.get("name").asText();
		String description = root.get("description").asText();
		List<String> tags = Jsoup.parse(root.get("tags").asText()).getElementsByTag("a").stream().map(Element::html).collect(Collectors.toList());

		return new RoomInfo(id, name, description, tags);
	}

	@Override
	public void listen(ChatMessageHandler handler) {
		while (true) {
			ChatMessage message;
			try {
				message = newMessages.take();
			} catch (InterruptedException e) {
				break;
			}

			if (message == CLOSE_MESSAGE) {
				break;
			}

			if (message.getEdits() == 0) {
				handler.onMessage(message);
			} else {
				handler.onMessageEdited(message);
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
		Response response = http.get(url);
		return parseFkey(response.getBody());
	}

	/**
	 * Parses the "fkey" parameter from an HTML page.
	 * @param html the HTML page
	 * @return the fkey or null if not found
	 */
	private static String parseFkey(String html) {
		Matcher m = fkeyRegex.matcher(html);
		return m.find() ? m.group(1) : null;
	}

	/**
	 * Gets the "fkey" parameter for a room.
	 * @param roomId the room ID
	 * @return the fkey
	 * @throws RoomNotFoundException if the room does not exist
	 * @throws RoomPermissionException if messages cannot be posted to this room
	 * @throws IOException if there's a network problem
	 */
	private synchronized String getFKey(int roomId) throws IOException {
		String fkey = fkeyCache.get(roomId);
		if (fkey != null) {
			return fkey;
		}

		Response response = http.get(CHAT_DOMAIN + "/rooms/" + roomId);

		/*
		 * A 404 response is returned if the room doesn't exist.
		 * 
		 * A 404 also seems to be returned if the room is inactive, but the bot
		 * does not have enough reputation/privileges to see inactive rooms. If
		 * I view the same room in a web browser under my personal account
		 * (which has over 20k rep and is a room owner), I can see the room. And
		 * when I make the bot login under my own account and then view an
		 * inactive room, the bot does not get a 404.
		 */
		boolean nonExistant = response.getStatusCode() == 404;
		if (nonExistant) {
			throw new RoomNotFoundException(roomId);
		}

		String body = response.getBody();
		fkey = parseFkey(body);
		if (fkey == null) {
			throw new IOException("Could not get fkey of room " + roomId + ".");
		}

		/*
		 * The textbox for sending messages won't be there if the bot can't post
		 * to the room.
		 */
		boolean canPostToRoom = body.contains("<textarea id=\"input\">");
		if (!canPostToRoom) {
			throw new RoomPermissionException(roomId);
		}

		fkeyCache.put(roomId, fkey);
		return fkey;
	}

	@Override
	public synchronized void close() throws IOException {
		flush();

		newMessages.clear();

		//leave all rooms
		if (!websocketSessions.isEmpty()) {
			int anyRoomId = websocketSessions.keySet().iterator().next();
			String fkey = fkeyCache.get(anyRoomId);

			try {
				//@formatter:off
				http.post(CHAT_DOMAIN + "/chats/leave/all",
					"quiet", "true", //setting this parameter to "false" results in an error
					"fkey", fkey
				);
				//@formatter:on
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Problem leaving all rooms.", e);
			}
		}

		for (Session session : websocketSessions.values()) {
			try {
				session.close();
			} catch (IOException e) {
				//ignore
			}
		}

		try {
			http.close();
		} catch (IOException e) {
			//ignore
		}

		try {
			newMessages.put(CLOSE_MESSAGE);
		} catch (InterruptedException e) {
			//ignore
		}
	}

	@Override
	public void flush() {
		//empty
	}

	/**
	 * Determines if the chat connection is currently watching a room.
	 * @param roomId the room ID
	 * @return true if it's watching the room, false if not
	 */
	private synchronized boolean isInRoom(int roomId) {
		return websocketSessions.containsKey(roomId);
	}

	/**
	 * Converts a timestamp to a {@link LocalDateTime} instance.
	 * @param ts the timestamp (seconds since epoch)
	 * @return the {@link LocalDateTime} instance
	 */
	private static LocalDateTime timestamp(long ts) {
		Instant instant = Instant.ofEpochSecond(ts);
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
	}

	private static class ChatMessageParser {
		private static final Pattern fixedWidthRegex = Pattern.compile("^<pre class='(full|partial)'>(.*?)</pre>$", Pattern.DOTALL);
		private static final Pattern multiLineRegex = Pattern.compile("^<div class='(full|partial)'>(.*?)</div>$", Pattern.DOTALL);

		/**
		 * Parse a chat message from an HTTP response.
		 * @param element the JSON element
		 * @return the parsed message
		 */
		public static ChatMessage fromHttp(JsonNode element) {
			return fromWebSocket(element);
		}

		/**
		 * Parse a chat message from a web socket.
		 * @param element the JSON element
		 * @return the parsed message
		 */
		public static ChatMessage fromWebSocket(JsonNode element) {
			ChatMessage.Builder builder = new ChatMessage.Builder();

			JsonNode value = element.get("content");
			if (value != null) {
				String content = value.asText();
				String inner = extractFixedFontContent(content);
				boolean fixedFont = (inner != null);
				if (!fixedFont) {
					inner = extractMultiLineContent(content);
					if (inner == null) {
						inner = content;
					}
				}

				builder.content(inner, fixedFont);
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
				LocalDateTime ts = timestamp(value.asLong());
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

		/**
		 * Extracts the message content from a message that is formatted in
		 * fixed font. Fixed font messages are enclosed in a &lt;pre&gt; tag.
		 * @param content the complete chat message content
		 * @return the extracted message content or null if the message is not
		 * fixed-font
		 */
		private static String extractFixedFontContent(String content) {
			Matcher m = fixedWidthRegex.matcher(content);
			return m.find() ? m.group(2) : null;
		}

		/**
		 * Extracts the message content from a multi-line message. Multi-line
		 * messages are enclosed in a &lt;div&gt; tag. Also converts &lt;br&gt;
		 * tags to newlines.
		 * @param content the complete chat message content
		 * @return the extracted message content or null if the message is not
		 * multi-line
		 */
		private static String extractMultiLineContent(String content) {
			Matcher m = multiLineRegex.matcher(content);
			if (!m.find()) {
				return null;
			}

			return m.group(2).replace(" <br> ", "\n");
		}
	}

	/**
	 * Helper class for sending HTTP requests.
	 * @author Michael Angstadt
	 */
	private static class Http implements Closeable {
		private static final Pattern response409Regex = Pattern.compile("\\d+");
		private final CloseableHttpClient client;

		public Http(CloseableHttpClient client) {
			this.client = client;
		}

		/**
		 * Gets an HTTP GET request.
		 * @param uri the URI
		 * @return the response
		 * @throws IOException if there's a problem sending the request
		 */
		public Response get(String uri) throws IOException {
			HttpGet request = new HttpGet(uri);
			return send(request);
		}

		/**
		 * Gets an HTTP POST request.
		 * @param uri the URI
		 * @param parameters the parameters to include in the request body (key,
		 * value, key, value, etc)
		 * @return the response
		 * @throws IOException if there's a problem sending the request
		 */
		public Response post(String uri, Object... parameters) throws IOException {
			if (parameters.length % 2 != 0) {
				throw new IllegalArgumentException("\"parameters\" vararg must have an even number of values.");
			}

			HttpPost request = new HttpPost(uri);

			if (parameters.length > 0) {
				List<NameValuePair> params = new ArrayList<>(parameters.length / 2);
				for (int i = 0; i < parameters.length; i += 2) {
					params.add(new BasicNameValuePair(parameters[i].toString(), parameters[i + 1].toString()));
				}
				request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));
			}

			return send(request);
		}

		/**
		 * Sends an HTTP request. If an HTTP 409 response is returned, this
		 * method will sleep the requested amount of time, and then retry the
		 * request.
		 * @param request the request
		 * @return the response
		 * @throws IOException if there was a problem sending the request
		 */
		private Response send(HttpUriRequest request) throws IOException {
			long sleep = 0;
			int attempts = 0;

			while (attempts < 5) {
				if (sleep > 0) {
					logger.info("Sleeping for " + sleep + "ms before resending the request...");
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}

				int statusCode;
				String body;
				try (CloseableHttpResponse response = client.execute(request)) {
					statusCode = response.getStatusLine().getStatusCode();
					body = EntityUtils.toString(response.getEntity());
				}

				/*
				 * An HTTP 409 response means that the bot is sending messages
				 * too quickly. The response body contains the number of seconds
				 * the bot must wait before it can post another message.
				 */
				if (statusCode == 409) {
					logger.info("HTTP " + statusCode + " response [url=" + request.getURI() + "]: " + body);
					Long waitTime = parse409Response(body);
					sleep = (waitTime == null) ? 5000 : waitTime;
					attempts++;
					continue;
				}

				return new Response(statusCode, body);
			}

			throw new IOException("Request to " + request.getURI() + " could not be sent after " + attempts + " attempts.");
		}

		/**
		 * Parses an HTTP 409 response, which indicates that the bot is sending
		 * messages too quickly.
		 * @param response the HTTP 409 response body (e.g. "You can perform
		 * this action again in 2 seconds")
		 * @return the amount of time (in milliseconds) the bot must wait before
		 * the chat system will accept new messages, or null if this value could
		 * not be parsed from the response
		 * @throws IOException if there's a problem getting the response body
		 */
		private static Long parse409Response(String body) throws IOException {
			Matcher m = response409Regex.matcher(body);
			if (!m.find()) {
				return null;
			}

			int seconds = Integer.parseInt(m.group(0));
			return TimeUnit.SECONDS.toMillis(seconds);
		}

		@Override
		public void close() throws IOException {
			client.close();
		}
	}

	/**
	 * Represents an HTTP response.
	 * @author Michael Angstadt
	 */
	private static class Response {
		private int statusCode;
		private String body;

		public Response(int statusCode, String body) {
			this.statusCode = statusCode;
			this.body = body;
		}

		/**
		 * Gets the status code.
		 * @return the status code
		 */
		public int getStatusCode() {
			return statusCode;
		}

		/**
		 * Gets the response body as a string.
		 * @return the response body
		 */
		public String getBody() {
			return body;
		}

		/**
		 * Parses the response body as JSON.
		 * @return the parsed JSON
		 * @throws JsonProcessingException if the body could not be parsed as
		 * JSON
		 */
		public JsonNode getBodyAsJson() throws JsonProcessingException {
			ObjectMapper mapper = new ObjectMapper();
			try {
				return mapper.readTree(body);
			} catch (JsonProcessingException e) {
				throw e;
			} catch (IOException e) {
				//should never be thrown
				throw new RuntimeException(e);
			}
		}
	}
}
