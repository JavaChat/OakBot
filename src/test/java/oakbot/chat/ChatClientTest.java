package oakbot.chat;

import static org.junit.Assert.assertEquals;
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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
			.response(200, loginPage("0123456789abcdef0123456789abcdef"))
		
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
			.response(200, loginPage("0123456789abcdef0123456789abcdef"))
		
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
			.response(200, chatRoom("0123456789abcdef0123456789abcdef"))
			
			.request("POST", "https://chat.stackoverflow.com/ws-auth",
				"roomid", "1",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "{\"url\":\"https://URL\"}")
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/events",
				"mode", "messages",
				"msgCount", "1",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "{\"events\":[" +
				"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 1\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157245}" +
			"]}")
			
			.request("POST", "https://chat.stackoverflow.com/chats/leave/all",
				"quiet", "true",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);
		Session session = mock(Session.class);
		doReturn(session).when(ws).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI("https://URL?l=1417023460")));

		try (ChatClient client = new ChatClient(httpClient, ws)) {
			Room room = client.joinRoom(1);
			assertEquals(1, room.getRoomId());
			assertEquals("0123456789abcdef0123456789abcdef", room.getFkey());
			assertSame(room, client.getRoom(1));
			assertTrue(client.isInRoom(1));

			List<Room> rooms = client.getRooms();
			assertEquals(1, rooms.size());
			assertSame(room, rooms.get(0));

			/*
			 * If the room is joined again, it should just return the Room
			 * object.
			 */
			assertSame(room, client.joinRoom(1));
		}

		verify(session).close();
		verifyHttpClient(httpClient, 4);
	}

	private static void verifyHttpClient(CloseableHttpClient httpClient, int requests) throws IOException {
		verify(httpClient, times(requests)).execute(any(HttpUriRequest.class));
		verify(httpClient).close();
	}

	/**
	 * Gets the HTML of the login page.
	 * @param fkey the fkey to populate the page with.
	 * @return the login page HTML
	 * @throws IOException
	 */
	private static String loginPage(String fkey) throws IOException {
		String html = readFile("users-login.html");
		return html.replace("${fkey}", fkey);
	}

	/**
	 * Gets the HTML of a chat room that the bot has permission to post to.
	 * @param fkey the fkey to populate the page with
	 * @return the chat room HTML
	 * @throws IOException
	 */
	private static String chatRoom(String fkey) throws IOException {
		String html = readFile("rooms-1.html");
		return html.replace("${fkey}", fkey);
	}

	private static String readFile(String file) throws IOException {
		URI uri;
		try {
			uri = ChatClientTest.class.getResource(file).toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		Path path = Paths.get(uri);
		return new String(Files.readAllBytes(path));
	}
}
