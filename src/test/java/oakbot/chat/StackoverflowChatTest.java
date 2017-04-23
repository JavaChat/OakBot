package oakbot.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Michael Angstadt
 */
public class StackoverflowChatTest {
	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	@Test
	public void login_failed() throws Exception {
		CloseableHttpClient client = mockClient(new AnswerImpl() {
			private final String loginFkey = "0123456789abcdef0123456789abcdef";

			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (requestCount) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://stackoverflow.com/users/login", uri);
					return response(200, loginPage(loginFkey));
				case 2:
					assertEquals("POST", method);
					assertEquals("https://stackoverflow.com/users/login", uri);

					Set<NameValuePair> actual = new HashSet<>(URLEncodedUtils.parse(body, Consts.UTF_8));
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", loginFkey),
						new BasicNameValuePair("email", "email@example.com"),
						new BasicNameValuePair("password", "password")
					));
					//@formatter:on
					assertEquals(expected, actual);

					return response(200, ""); //302 is returned when login is successful
				}

				return super.answer(method, uri, body);
			}
		});

		try (StackoverflowChat chat = new StackoverflowChat(client)) {
			chat.login("email@example.com", "password");
			fail();
		} catch (InvalidCredentialsException e) {
			//expected
		}

		verify(client, times(2)).execute(any(HttpUriRequest.class));
		verify(client).close();
	}

	@Test
	public void login() throws Exception {
		CloseableHttpClient client = mockClient(new AnswerImpl() {
			private final String loginFkey = "0123456789abcdef0123456789abcdef";

			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (requestCount) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://stackoverflow.com/users/login", uri);
					return response(200, loginPage(loginFkey));
				case 2:
					assertEquals("POST", method);
					assertEquals("https://stackoverflow.com/users/login", uri);

					Set<NameValuePair> actual = new HashSet<>(URLEncodedUtils.parse(body, Consts.UTF_8));
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", loginFkey),
						new BasicNameValuePair("email", "email@example.com"),
						new BasicNameValuePair("password", "password")
					));
					//@formatter:on
					assertEquals(expected, actual);

					return response(302, "");
				}

				return super.answer(method, uri, body);
			}
		});

		try (StackoverflowChat chat = new StackoverflowChat(client)) {
			chat.login("email@example.com", "password");
		}

		verify(client, times(2)).execute(any(HttpUriRequest.class));
		verify(client).close();
	}

	@Test
	public void joinRoom() throws Exception {
		CloseableHttpClient client = mockClient(new AnswerImpl() {
			private final String fkey = "0123456789abcdef0123456789abcdef";

			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (requestCount) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);

					/*
					 * Room doesn't exist.
					 */
					return response(404, "");
				case 2:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);

					/*
					 * The lack of an fkey is treated as an IOException because
					 * every chat room should have an fkey.
					 */
					return response(200, "");
				case 3:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, protectedChatRoom(fkey));
				case 4:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, chatRoom(fkey));
				case 5:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/events", uri);
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", fkey),
						new BasicNameValuePair("mode", "messages"),
						new BasicNameValuePair("msgCount", "10")
					));
					//@formatter:on
					Set<NameValuePair> actual = params(body);
					assertEquals(expected, actual);

					return response(200, "{}");
				case 6:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/leave/1", uri);
					//@formatter:off
					expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", fkey),
						new BasicNameValuePair("quiet", "true")
					));
					//@formatter:on
					actual = params(body);
					assertEquals(expected, actual);

					return response(200, "\"ok\"");
				}

				return super.answer(method, uri, body);
			}
		});

		try (StackoverflowChat chat = new StackoverflowChat(client)) {
			try {
				chat.joinRoom(1);
				fail();
			} catch (RoomNotFoundException e) {
				//expected
			}
			try {
				chat.joinRoom(1);
				fail();
			} catch (IOException e) {
				//expected
			}
			try {
				chat.joinRoom(1);
				fail();
			} catch (RoomPermissionException e) {
				//expected
			}

			chat.joinRoom(1);
		}

		verify(client, times(6)).execute(any(HttpUriRequest.class));
		verify(client).close();
	}

	@Test
	public void sendMessage() throws Exception {
		List<Integer> joinedRooms = new ArrayList<>();
		Multimap<Integer, String> messages = ArrayListMultimap.create();
		CloseableHttpClient client = mockClient(new AnswerImpl() {
			private boolean slept = false;

			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (method) {
				case "GET":
					if (uri.equals("https://chat.stackoverflow.com/rooms/1")) {
						assertFalse(joinedRooms.contains(1));
						joinedRooms.add(1);
						return response(200, "value=\"0123456789abcdef0123456789abcdef\" <textarea id=\"input\"></textarea>");
					}
					if (uri.equals("https://chat.stackoverflow.com/rooms/2")) {
						assertFalse(joinedRooms.contains(2));
						joinedRooms.add(2);
						return response(200, "value=\"0123456789abcdef0123456789abcdef\" <textarea id=\"input\"></textarea>");
					}
					break;
				case "POST":
					if (messages.size() == 2 && !slept) {
						slept = true;
						return response(409, "You can perform this action again in 2 seconds");
					}
					if (uri.equals("https://chat.stackoverflow.com/chats/1/messages/new")) {
						messages.put(1, body);
						return response(200, "{\"id\":1,\"time\":" + (System.currentTimeMillis() / 1000) + "}");
					} else if (uri.equals("https://chat.stackoverflow.com/chats/2/messages/new")) {
						messages.put(2, body);
						return response(200, "{\"id\":2,\"time\":" + (System.currentTimeMillis() / 1000) + "}");
					}
					break;
				}

				fail("Unexpected request: " + method + " " + uri + " " + body);
				return null;
			}
		});

		long start = System.currentTimeMillis();
		try (StackoverflowChat chat = new StackoverflowChat(client)) {
			chat.sendMessage(1, "Test1");
			chat.sendMessage(1, "Test2");
			chat.sendMessage(2, "Test3");
		}
		long end = System.currentTimeMillis();
		long elapsed = end - start;

		assertTrue(elapsed >= 2000);
		assertEquals(Arrays.asList(1, 2), joinedRooms);
		assertEquals(2, messages.get(1).size());
		assertEquals(1, messages.get(2).size());

		verify(client, times(6)).execute(any(HttpUriRequest.class));
		verify(client).close();
	}

	@Test
	public void getMessages_non_JSON_response() throws Exception {
		CloseableHttpClient client = mockClient(new AnswerImpl() {
			private long prevRequestSent;
			private final String fkey = "0123456789abcdef0123456789abcdef";

			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (requestCount) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, chatRoom(fkey));
				case 2:
					prevRequestSent = System.currentTimeMillis();
					return response(200, "<html>error!</html>");
				case 3:
					long diff = System.currentTimeMillis() - prevRequestSent;
					prevRequestSent = System.currentTimeMillis();
					assertTrue(diff >= 500); //should sleep before retrying

					//@formatter:off
					return response(200,
					"{\"events\":[" +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 1\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157245}" +
					"]}");
					//@formatter:on
				}

				return super.answer(method, uri, body);
			}
		});

		try (StackoverflowChat chat = new StackoverflowChat(client, 500, 0)) {
			Iterator<ChatMessage> messages = chat.getMessages(1, 1).iterator();

			ChatMessage message = messages.next();
			assertEquals("message 1", message.getContent());
			assertEquals(20157245L, message.getMessageId());
			assertEquals(1, message.getRoomId());
			assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1417041460000L), ZoneId.systemDefault()), message.getTimestamp());
			assertEquals(50, message.getUserId());
			assertEquals("User1", message.getUsername());

			assertFalse(messages.hasNext());
		}

		verify(client, times(3)).execute(any(HttpUriRequest.class));
		verify(client).close();
	}

	@Test
	public void getMessages() throws Exception {
		CloseableHttpClient client = mockClient(new AnswerImpl() {
			private final String fkey = "0123456789abcdef0123456789abcdef";

			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (requestCount) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, chatRoom(fkey));
				case 2:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/events", uri);
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", fkey),
						new BasicNameValuePair("mode", "messages"),
						new BasicNameValuePair("msgCount", "3")
					));
					//@formatter:on
					Set<NameValuePair> actual = params(body);
					assertEquals(expected, actual);

					//@formatter:off
					return response(200,
					"{\"events\":[" +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 1\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157245}," +
						"{\"event_type\":1,\"time_stamp\":1417043460,\"user_id\":51,\"user_name\":\"User2\",\"room_id\":1,\"message_id\":20157246,\"edits\":0}," +
						"{\"event_type\":1,\"time_stamp\":1417045460,\"content\":\"message 3\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157247}" +
					"]}");
					//@formatter:on
				}

				return super.answer(method, uri, body);
			}
		});

		try (StackoverflowChat chat = new StackoverflowChat(client)) {
			Iterator<ChatMessage> messages = chat.getMessages(1, 3).iterator();

			ChatMessage message = messages.next();
			assertEquals("message 1", message.getContent());
			assertEquals(20157245L, message.getMessageId());
			assertEquals(1, message.getRoomId());
			assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1417041460000L), ZoneId.systemDefault()), message.getTimestamp());
			assertEquals(50, message.getUserId());
			assertEquals("User1", message.getUsername());

			message = messages.next();
			assertNull(message.getContent()); //deleted message
			assertEquals(20157246L, message.getMessageId());
			assertEquals(1, message.getRoomId());
			assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1417043460000L), ZoneId.systemDefault()), message.getTimestamp());
			assertEquals(51, message.getUserId());
			assertEquals("User2", message.getUsername());

			message = messages.next();
			assertEquals("message 3", message.getContent());
			assertEquals(20157247L, message.getMessageId());
			assertEquals(1, message.getRoomId());
			assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1417045460000L), ZoneId.systemDefault()), message.getTimestamp());
			assertEquals(50, message.getUserId());
			assertEquals("User1", message.getUsername());

			assertFalse(messages.hasNext());
		}

		verify(client, times(2)).execute(any(HttpUriRequest.class));
		verify(client).close();
	}

	//TODO fix
	@Ignore
	@Test
	public void listen() throws Exception {
		CloseableHttpClient client = mockClient(new AnswerImpl() {
			private final String fkey = "0123456789abcdef0123456789abcdef";

			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (requestCount) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, chatRoom(fkey));
				case 2:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/events", uri);
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", fkey),
						new BasicNameValuePair("mode", "messages"),
						new BasicNameValuePair("msgCount", "1")
					));
					//@formatter:on
					Set<NameValuePair> actual = params(body);
					assertEquals(expected, actual);

					//@formatter:off
					return response(200,
					"{\"events\":[" +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 1\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157245}," +
						"{\"event_type\":1,\"time_stamp\":1417043460,\"content\":\"message 2\",\"user_id\":51,\"user_name\":\"User2\",\"room_id\":1,\"message_id\":20157246,\"edits\":2}," +
						"{\"event_type\":1,\"time_stamp\":1417045460,\"content\":\"message 3\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157247}" +
					"]}");
					//@formatter:on
				case 3:
				case 4:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/events", uri);
					//@formatter:off
					expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", fkey),
						new BasicNameValuePair("mode", "messages"),
						new BasicNameValuePair("msgCount", "5")
					));
					//@formatter:on
					actual = params(body);
					assertEquals(expected, actual);

					//@formatter:off
					return response(200,
					"{\"events\":[" +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 1\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157245}," +
						"{\"event_type\":1,\"time_stamp\":1417043460,\"content\":\"message 2\",\"user_id\":51,\"user_name\":\"User2\",\"room_id\":1,\"message_id\":20157246,\"edits\":2}," +
						"{\"event_type\":1,\"time_stamp\":1417045460,\"content\":\"message 3\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157247}," +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 4\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157248}," +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 5\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157249}" +
					"]}");
					//@formatter:on
				case 5:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/events", uri);
					//@formatter:off
					expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", fkey),
						new BasicNameValuePair("mode", "messages"),
						new BasicNameValuePair("msgCount", "5")
					));
					//@formatter:on
					actual = params(body);
					assertEquals(expected, actual);

					/*
					 * It notices that all the messages are new, which means
					 * there may be some new messages it hasn't seen. So, it
					 * will make another request to increase the number of
					 * messages it receives.
					 */
					//@formatter:off
					return response(200,
					"{\"events\":[" +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 8\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157250}," +
						"{\"event_type\":1,\"time_stamp\":1417043460,\"content\":\"message 9\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157251}," +
						"{\"event_type\":1,\"time_stamp\":1417045460,\"content\":\"message 10\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157252}," +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 11\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157253}," +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 12\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157254}" +
					"]}");
					//@formatter:on
				case 6:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/events", uri);
					//@formatter:off
					expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", fkey),
						new BasicNameValuePair("mode", "messages"),
						new BasicNameValuePair("msgCount", "10")
					));
					//@formatter:on
					actual = params(body);
					assertEquals(expected, actual);

					/*
					 * This time, it sees a message it has seen before, which
					 * means it's not missing any new messages. It can now parse
					 * the new messages and send them to the handler, safely
					 * knowing it has not missed any.
					 */
					//@formatter:off
					return response(200,
					"{\"events\":[" +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 3\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157247}," +
						"{\"event_type\":1,\"time_stamp\":1417043460,\"content\":\"message 4\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157248}," +
						"{\"event_type\":1,\"time_stamp\":1417045460,\"content\":\"message 5\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157249}," +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 6\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157250}," +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 7\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157251}," +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 8\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157252}," +
						"{\"event_type\":1,\"time_stamp\":1417043460,\"content\":\"message 9\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157253}," +
						"{\"event_type\":1,\"time_stamp\":1417045460,\"content\":\"message 10\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157254}," +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 11\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157255}," +
						"{\"event_type\":1,\"time_stamp\":1417041460,\"content\":\"message 12\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157256}" +
					"]}");
					//@formatter:on
				case 7:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/leave/1", uri);
					//@formatter:off
					expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", fkey),
						new BasicNameValuePair("quiet", "true")
					));
					//@formatter:on
					actual = params(body);
					assertEquals(expected, actual);

					return response(200, "\"ok\"");
				}

				return super.answer(method, uri, body);
			}
		});

		StackoverflowChat chat = new StackoverflowChat(client, 0, 10);
		chat.joinRoom(1);
		AtomicInteger number = new AtomicInteger(4);
		chat.listen(new ChatMessageHandler() {
			@Override
			public void onMessage(ChatMessage message) {
				assertEquals("message " + number.getAndIncrement(), message.getContent());
				if (number.get() > 12) {
					try {
						chat.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}

			@Override
			public void onMessageEdited(ChatMessage message) {
				//empty
			}

			@Override
			public void onError(int roomId, Exception thrown) {
				//empty
			}
		});

		verify(client, times(7)).execute(any(HttpUriRequest.class));
		verify(client).close();
	}

	private static CloseableHttpClient mockClient(AnswerImpl answer) throws IOException {
		CloseableHttpClient client = mock(CloseableHttpClient.class);
		doAnswer(answer).when(client).execute(any(HttpUriRequest.class));
		return client;
	}

	private static class AnswerImpl implements Answer<HttpResponse> {
		protected int requestCount = 0;

		@Override
		public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
			HttpRequest request = (HttpRequest) invocation.getArguments()[0];
			String uri = request.getRequestLine().getUri();
			String method = request.getRequestLine().getMethod();

			String body = null;
			if (request instanceof HttpPost) {
				HttpPost post = (HttpPost) request;
				body = EntityUtils.toString(post.getEntity());
			}

			requestCount++;
			return answer(method, uri, body);
		}

		protected HttpResponse answer(String method, String uri, String body) throws IOException {
			fail("An extra request was generated, which was not anticipated by the unit test.");
			return null;
		}

		/**
		 * Generates a mock response
		 * @param statusCode the status code
		 * @param body the response body
		 * @return the response
		 * @throws UnsupportedEncodingException if the body contains unsupported
		 * characters
		 */
		protected static HttpResponse response(int statusCode, String body) throws UnsupportedEncodingException {
			HttpEntity entity = new StringEntity(body);
			StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, "");

			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getStatusLine()).thenReturn(statusLine);
			when(response.getEntity()).thenReturn(entity);
			return response;
		}

		/**
		 * Parses parameters from a POST request body.
		 * @param body the request body
		 * @return the parameters
		 */
		protected static Set<NameValuePair> params(String body) {
			return new HashSet<>(URLEncodedUtils.parse(body, Consts.UTF_8));
		}
	};

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

	/**
	 * Gets the HTML of a chat room that the bot does not have permission to
	 * post to.
	 * @param fkey the fkey to populate the page with
	 * @return the chat room HTML
	 * @throws IOException
	 */
	private static String protectedChatRoom(String fkey) throws IOException {
		String html = readFile("rooms-15-protected.html");
		return html.replace("${fkey}", fkey);
	}

	private static String readFile(String file) throws IOException {
		URI uri;
		try {
			uri = StackoverflowChatTest.class.getResource(file).toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		Path path = Paths.get(uri);
		return new String(Files.readAllBytes(path));
	}
}
