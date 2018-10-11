package oakbot.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class ChatClientTest {
	@Test(expected = IllegalStateException.class)
	public void joinRoom_not_logged_in() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.joinRoom(1);
		}
	}

	@Test
	public void getRoom_has_not_been_joined() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			assertNull(client.getRoom(1));
		}

		verifyNumberOfRequestsSent(httpClient, 0);
	}

	@Test
	public void joinRoom_not_found() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.login("0123456789abcdef0123456789abcdef", "email", "password", true)
				
			.request("GET", "https://chat.stackoverflow.com/rooms/1")
			.response(404, "")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.login("email", "password");
			client.joinRoom(1);
			fail();
		} catch (RoomNotFoundException e) {
			//expected
		}

		verifyNumberOfRequestsSent(httpClient, 3);
	}

	@Test
	public void joinRoom_no_fkey() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.login("0123456789abcdef0123456789abcdef", "email", "password", true)
			.request("GET", "https://chat.stackoverflow.com/rooms/1")
			.response(200, "garbage data")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.login("email", "password");
			client.joinRoom(1);
			fail();
		} catch (IOException e) {
			//expected
		}

		verifyNumberOfRequestsSent(httpClient, 3);
	}

	@Test
	public void joinRoom() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.login("0123456789abcdef0123456789abcdef", "email", "password", true)
			
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
			
			.request("POST", "https://chat.stackoverflow.com/chats/leave/all",
				"quiet", "true",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);
		Session session = mock(Session.class);

		//@formatter:off
		when(ws.connectToServer(
			any(Endpoint.class),
			any(ClientEndpointConfig.class),
			eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=" + webSocketTimestamp(1417041460)))
		)).thenReturn(session);
		//@formatter:on

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.login("email", "password");

			Room room = client.joinRoom(1);
			assertEquals(1, room.getRoomId());
			assertEquals("0123456789abcdef0123456789abcdef", room.getFkey());

			assertSame(room, client.getRoom(1));
			assertTrue(client.isInRoom(1));
			assertEquals(Arrays.asList(room), client.getRooms());

			/*
			 * If the room is joined again, it should just return the Room
			 * object.
			 */
			assertSame(room, client.joinRoom(1));
		}

		verify(session).close();
		verifyNumberOfRequestsSent(httpClient, 6);
	}

	@Test
	public void joinRoom_that_has_no_messages() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.login("0123456789abcdef0123456789abcdef", "email", "password", true)
				
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
				//empty
			.build())
			
			.request("POST", "https://chat.stackoverflow.com/chats/leave/all",
				"quiet", "true",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);
		Session session = mock(Session.class);

		//@formatter:off
		when(ws.connectToServer(
			any(Endpoint.class),
			any(ClientEndpointConfig.class),
			eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=0"))
		)).thenReturn(session);
		//@formatter:on

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.login("email", "password");
			client.joinRoom(1);
		}

		verify(session).close();
		verifyNumberOfRequestsSent(httpClient, 6);
	}

	@Test
	public void leave_room() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.login("0123456789abcdef0123456789abcdef", "email", "password", true)
				
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
			
			.request("POST", "https://chat.stackoverflow.com/chats/leave/1",
				"quiet", "true",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);
		Session session = mock(Session.class);

		//@formatter:off
		when(ws.connectToServer(
			any(Endpoint.class),
			any(ClientEndpointConfig.class),
			eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=" + webSocketTimestamp(1417041460)))
		)).thenReturn(session);
		//@formatter:on

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.login("email", "password");

			Room room = client.joinRoom(1);
			assertEquals(1, room.getRoomId());
			assertEquals("0123456789abcdef0123456789abcdef", room.getFkey());

			assertSame(room, client.getRoom(1));
			assertTrue(client.isInRoom(1));
			assertEquals(Arrays.asList(room), client.getRooms());

			room.leave();
			assertNull(client.getRoom(1));
			assertFalse(client.isInRoom(1));
			assertEquals(Arrays.asList(), client.getRooms());
		}

		verify(session).close();
		verifyNumberOfRequestsSent(httpClient, 6);
	}

	private static void verifyNumberOfRequestsSent(CloseableHttpClient httpClient, int requests) throws IOException {
		verify(httpClient, times(requests)).execute(any(HttpUriRequest.class));
		verify(httpClient).close();
	}

	/**
	 * This conversion is needed for the unit test to run on other machines. I
	 * think it has something to do with the default timezone.
	 * @param messageTs the timestamp of the chat message
	 * @return the value that will be put in the web socket URL
	 */
	private static long webSocketTimestamp(long messageTs) {
		Instant instant = Instant.ofEpochSecond(messageTs);
		LocalDateTime dt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
		return dt.toEpochSecond(ZoneOffset.UTC);
	}
}
