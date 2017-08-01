package oakbot.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.BeforeClass;
import org.junit.Test;

import oakbot.chat.event.Event;
import oakbot.chat.event.MessagePostedEvent;

/**
 * @author Michael Angstadt
 */
@SuppressWarnings("resource")
public class RoomTest {
	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	/**
	 * Anything that is not JSON should be silently ignored.
	 */
	@Test
	public void webSocket_ignore_bad_data() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		chatClient.joinRoom(1);

		wsRoom1.send("garbage data");

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_MessagePostedEvent() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(MessagePostedEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.newMessage(1, 1417041460, "one", 50, "User", 20157245)
		.build());
		//@formatter:on

		assertEquals(1, events.size());

		MessagePostedEvent event = (MessagePostedEvent) events.get(0);
		assertEquals(1, event.getEventId());
		assertEquals(timestamp(1417041460), event.getTimestamp());
		assertEquals("one", event.getMessage().getContent());
		assertEquals(0, event.getMessage().getEdits());
		assertEquals(0, event.getMessage().getMentionedUserId());
		assertEquals(20157245, event.getMessage().getMessageId());
		assertEquals(0, event.getMessage().getParentMessageId());
		assertEquals(1, event.getMessage().getRoomId());
		assertEquals("Sandbox", event.getMessage().getRoomName());
		assertEquals(0, event.getMessage().getStars());
		assertEquals(timestamp(1417041460), event.getMessage().getTimestamp());
		assertEquals(50, event.getMessage().getUserId());
		assertEquals("User", event.getMessage().getUsername());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_ignore_messages_from_other_rooms() throws Exception {
		//TODO
	}

	@Test
	public void sendMessage() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
			
			//====send message
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "one",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.newMessage(1))
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);
		assertEquals(1, room.sendMessage("one"));

		verifyHttpClient(httpClient, 4);
	}

	@Test
	public void sendMessage_split_strategy() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
			
			//====send message
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "Java is an island of Indonesia. With a population of over 141 million (the island itself) or 145 million (the administrative region), Java is home to 56.7 percent of the Indonesian population and is the most populous island on Earth. The Indonesian capital city, Jakarta, is located on western Java. Much of Indonesian history took place on Java. It was the center of powerful Hindu-Buddhist empires, the Islamic sultanates, and the core of the colonial Dutch East Indies. Java was also the ...",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.newMessage(1))
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "center of the Indonesian struggle for independence during the 1930s and 1940s. Java dominates Indonesia politically, economically and culturally.",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.newMessage(2))
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);
		Session session = mock(Session.class);
		doReturn(session).when(ws).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417023460")));

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);
		assertEquals(Arrays.asList(1L, 2L), room.sendMessage("Java is an island of Indonesia. With a population of over 141 million (the island itself) or 145 million (the administrative region), Java is home to 56.7 percent of the Indonesian population and is the most populous island on Earth. The Indonesian capital city, Jakarta, is located on western Java. Much of Indonesian history took place on Java. It was the center of powerful Hindu-Buddhist empires, the Islamic sultanates, and the core of the colonial Dutch East Indies. Java was also the center of the Indonesian struggle for independence during the 1930s and 1940s. Java dominates Indonesia politically, economically and culturally.", SplitStrategy.WORD));

		verifyHttpClient(httpClient, 5);
	}

	@Test
	public void sendMessage_posting_too_fast() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
			
			//====send message
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "one",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(409, "You can perform this action again in 2 seconds")
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "one",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.newMessage(1))
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);
		Session session = mock(Session.class);
		doReturn(session).when(ws).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417023460")));

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room1 = chatClient.joinRoom(1);

		long start = System.currentTimeMillis();
		assertEquals(1, room1.sendMessage("one"));
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed >= 2000);

		verifyHttpClient(httpClient, 5);
	}

	@Test
	public void sendMessage_cannot_post() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			//====join room
				
			.request("GET", "https://chat.stackoverflow.com/rooms/1")
			.response(200, ResponseSamples.protectedChatRoom("0123456789abcdef0123456789abcdef"))
			
			.request("POST", "https://chat.stackoverflow.com/ws-auth",
				"roomid", "1",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.wsAuth("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247"))
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/events",
				"mode", "messages",
				"msgCount", "1",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.events()
				.event(1417041460, "message 1", 50, "User1", 1, 20157245)
			.build())
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);
		Session session = mock(Session.class);
		doReturn(session).when(ws).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417023460")));

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);
		try {
			room.sendMessage("one");
			fail();
		} catch (RoomPermissionException e) {
			//expected
		}

		verifyHttpClient(httpClient, 3);
	}

	/**
	 * @see ChatClientTest#leave_room
	 */
	@Test
	public void leave() throws Exception {
		//empty
	}

	private static void verifyHttpClient(CloseableHttpClient httpClient, int requests) throws IOException {
		verify(httpClient, times(requests)).execute(any(HttpUriRequest.class));
	}

	/**
	 * Represents a mock web socket server.
	 * @author Michael Angstadt
	 */
	private static class MockWebSocketServer {
		private Whole<String> messageHandler;

		/**
		 * @param container the mock container. This object is shared amongst
		 * all the {@link MockWebSocketServer} instances (each instance is for a
		 * single chat room).
		 * @param url the expected URL that the room will use to connect to the
		 * web socket
		 */
		@SuppressWarnings("unchecked")
		public MockWebSocketServer(WebSocketContainer container, String url) throws Exception {
			Session session = mock(Session.class);
			doAnswer((invocation) -> {
				messageHandler = (Whole<String>) invocation.getArguments()[1];
				return null;
			}).when(session).addMessageHandler(eq(String.class), any(Whole.class));

			when(container.connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI(url)))).then((invocation) -> {
				Endpoint endpoint = (Endpoint) invocation.getArguments()[0];
				endpoint.onOpen(session, mock(EndpointConfig.class));
				return session;
			});
		}

		/**
		 * Sends a web socket message.
		 * @param message the message to send
		 */
		public void send(String message) {
			messageHandler.onMessage(message);
		}
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
}
