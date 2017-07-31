package oakbot.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.LogManager;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.BeforeClass;
import org.junit.Test;

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

	@Test
	public void sendMessage() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			//join room 1=========================
				
			.request("GET", "https://chat.stackoverflow.com/rooms/1")
			.response(200, ResponseSamples.chatRoom("0123456789abcdef0123456789abcdef"))
			
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
			
			//send message=========================
			
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
		Room room = chatClient.joinRoom(1);
		assertEquals(1, room.sendMessage("one"));

		verifyHttpClient(httpClient, 4);
	}

	@Test
	public void sendMessage_split_strategy() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			//join room 1=========================
				
			.request("GET", "https://chat.stackoverflow.com/rooms/1")
			.response(200, ResponseSamples.chatRoom("0123456789abcdef0123456789abcdef"))
			
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
			
			//send message=========================
			
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
			
			//close the chat client=========================
			
			.request("POST", "https://chat.stackoverflow.com/chats/leave/all",
				"quiet", "true",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "")
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
			//join room=========================
				
			.request("GET", "https://chat.stackoverflow.com/rooms/1")
			.response(200, ResponseSamples.chatRoom("0123456789abcdef0123456789abcdef"))
			
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
			
			//send message=========================
			
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
			//join room 1=========================
				
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
}
