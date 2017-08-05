package oakbot.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
public class ChatClientTest {
	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	@Test
	public void login_no_fkey() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.request("GET", "https://stackoverflow.com/users/login")
			.response(200, "garbage data")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.login("email@example.com", "password");
			fail();
		} catch (IOException e) {
			//expected
		}

		verifyHttpClient(httpClient, 1);
	}

	@Test
	public void login_bad_credentials() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.request("GET", "https://stackoverflow.com/users/login")
			.response(200, ResponseSamples.loginPage("0123456789abcdef0123456789abcdef"))
		
			.request("POST", "https://stackoverflow.com/users/login",
				"fkey", "0123456789abcdef0123456789abcdef",
				"email", "email@example.com",
				"password", "password"
			)
			.response(200, "")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.login("email@example.com", "password");
			fail();
		} catch (InvalidCredentialsException e) {
			//expected
		}

		verifyHttpClient(httpClient, 2);
	}

	@Test
	public void login() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.request("GET", "https://stackoverflow.com/users/login")
			.response(200, ResponseSamples.loginPage("0123456789abcdef0123456789abcdef"))
		
			.request("POST", "https://stackoverflow.com/users/login",
				"fkey", "0123456789abcdef0123456789abcdef",
				"email", "email@example.com",
				"password", "password"
			)
			.response(302, "")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.login("email@example.com", "password");
		}

		verifyHttpClient(httpClient, 2);
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

		verifyHttpClient(httpClient, 0);
	}

	@Test
	public void joinRoom_not_found() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.request("GET", "https://chat.stackoverflow.com/rooms/1")
			.response(404, "")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.joinRoom(1);
			fail();
		} catch (RoomNotFoundException e) {
			//expected
		}

		verifyHttpClient(httpClient, 1);
	}

	@Test
	public void joinRoom_no_fkey() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.request("GET", "https://chat.stackoverflow.com/rooms/1")
			.response(200, "garbage data")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.joinRoom(1);
			fail();
		} catch (IOException e) {
			//expected
		}

		verifyHttpClient(httpClient, 1);
	}

	@Test
	public void joinRoom() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
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
		doReturn(session).when(ws).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417023460")));

		try (ChatClient client = new ChatClient(httpClient, ws)) {
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
		verifyHttpClient(httpClient, 4);
	}

	@Test
	public void joinRoom_that_has_no_messages() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
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
		doReturn(session).when(ws).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=0")));

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			client.joinRoom(1);
		}

		verify(session).close();
		verifyHttpClient(httpClient, 4);
	}

	@Test
	public void leave_room() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
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
		doReturn(session).when(ws).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417023460")));

		try (ChatClient client = new ChatClient(httpClient, ws)) {
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
		verifyHttpClient(httpClient, 4);
	}

	private static void verifyHttpClient(CloseableHttpClient httpClient, int requests) throws IOException {
		verify(httpClient, times(requests)).execute(any(HttpUriRequest.class));
		verify(httpClient).close();
	}
}
