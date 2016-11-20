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

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
		HttpClient client = mockClient(new AnswerImpl() {
			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (count) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://stackoverflow.com/users/login", uri);
					return response(200, "value=\"0123456789abcdef0123456789abcdef\"");
				case 2:
					assertEquals("POST", method);
					assertEquals("https://stackoverflow.com/users/login", uri);

					Set<NameValuePair> actual = new HashSet<>(URLEncodedUtils.parse(body, Consts.UTF_8));
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", "0123456789abcdef0123456789abcdef"),
						new BasicNameValuePair("email", "email@example.com"),
						new BasicNameValuePair("password", "password")
					));
					//@formatter:on
					assertEquals(expected, actual);

					return response(200, "");
				}

				return super.answer(method, uri, body);
			}
		});

		StackoverflowChat chat = new StackoverflowChat(client);
		try {
			chat.login("email@example.com", "password");
			fail();
		} catch (IllegalArgumentException e) {
			//expected
		}

		verify(client, times(2)).execute(any(HttpUriRequest.class));
	}

	@Test
	public void login() throws Exception {
		HttpClient client = mockClient(new AnswerImpl() {
			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (count) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://stackoverflow.com/users/login", uri);
					return response(200, "value=\"0123456789abcdef0123456789abcdef\"");
				case 2:
					assertEquals("POST", method);
					assertEquals("https://stackoverflow.com/users/login", uri);

					Set<NameValuePair> actual = new HashSet<>(URLEncodedUtils.parse(body, Consts.UTF_8));
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", "0123456789abcdef0123456789abcdef"),
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

		StackoverflowChat chat = new StackoverflowChat(client);
		chat.login("email@example.com", "password");

		verify(client, times(2)).execute(any(HttpUriRequest.class));
	}

	@Test
	public void joinRoom() throws Exception {
		HttpClient client = mockClient(new AnswerImpl() {
			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (count) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, "404 NOT FOUND");
				case 2:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, "value=\"0123456789abcdef0123456789abcdef\"");
				case 3:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, "value=\"0123456789abcdef0123456789abcdef\" <textarea id=\"input\"></textarea>");
				case 4:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/events", uri);
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", "0123456789abcdef0123456789abcdef"),
						new BasicNameValuePair("mode", "messages"),
						new BasicNameValuePair("msgCount", "1")
					));
					//@formatter:on
					Set<NameValuePair> actual = params(body);
					assertEquals(expected, actual);

					return response(200, "{}");
				}

				return super.answer(method, uri, body);
			}
		});

		StackoverflowChat chat = new StackoverflowChat(client);
		try {
			chat.joinRoom(1);
			fail();
		} catch (IOException e) {
			//expected
		}
		try {
			chat.joinRoom(1);
			fail();
		} catch (IOException e) {
			//expected
		}
		chat.joinRoom(1);
		chat.flush(); //should block until the message queue is empty
		verify(client, times(4)).execute(any(HttpUriRequest.class));
	}

	@Test
	public void sendMessage() throws Exception {
		HttpClient client = mockClient(new AnswerImpl() {
			private long prevRequestSent;

			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (count) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, "value=\"0123456789abcdef0123456789abcdef\" <textarea id=\"input\"></textarea>");
				case 2:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/messages/new", uri);
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", "0123456789abcdef0123456789abcdef"),
						new BasicNameValuePair("text", "Test1")
					));
					//@formatter:on
					Set<NameValuePair> actual = params(body);
					assertEquals(expected, actual);

					return response(200, "{}");
				case 3:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/messages/new", uri);
					//@formatter:off
					expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", "0123456789abcdef0123456789abcdef"),
						new BasicNameValuePair("text", "Test2")
					));
					//@formatter:on
					actual = params(body);
					assertEquals(expected, actual);

					return response(200, "{}");
				case 4:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/2", uri);
					return response(200, "value=\"abcdef0123456789abcdef0123456789\" <textarea id=\"input\"></textarea>");
				case 5:
					prevRequestSent = System.currentTimeMillis();
					assertEquals("https://chat.stackoverflow.com/chats/2/messages/new", uri);
					//@formatter:off
					expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", "abcdef0123456789abcdef0123456789"),
						new BasicNameValuePair("text", "Test3")
					));
					//@formatter:on
					actual = params(body);
					assertEquals(expected, actual);

					return response(409, "You can perform this action again in 2 seconds");
				case 6:
					long diff = System.currentTimeMillis() - prevRequestSent;
					assertTrue(diff >= 2000);

					assertEquals("https://chat.stackoverflow.com/chats/2/messages/new", uri);
					//@formatter:off
					expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", "abcdef0123456789abcdef0123456789"),
						new BasicNameValuePair("text", "Test3")
					));
					//@formatter:on
					actual = params(body);
					assertEquals(expected, actual);

					return response(200, "{}");
				}

				return super.answer(method, uri, body);
			}
		});

		StackoverflowChat chat = new StackoverflowChat(client);
		chat.sendMessage(1, "Test1");
		chat.sendMessage(1, "Test2");
		chat.sendMessage(2, "Test3");
		chat.flush(); //should block until the message queue is empty
		verify(client, times(6)).execute(any(HttpUriRequest.class));
	}

	@Test
	public void getMessages_non_JSON_response() throws Exception {
		HttpClient client = mockClient(new AnswerImpl() {
			private long prevRequestSent;

			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (count) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, "value=\"0123456789abcdef0123456789abcdef\" <textarea id=\"input\"></textarea>");
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

		StackoverflowChat chat = new StackoverflowChat(client, 500);
		Iterator<ChatMessage> messages = chat.getMessages(1, 1).iterator();

		ChatMessage message = messages.next();
		assertEquals("message 1", message.getContent());
		assertEquals(0, message.getEdits());
		assertEquals(20157245L, message.getMessageId());
		assertEquals(1, message.getRoomId());
		assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1417041460000L), ZoneId.systemDefault()), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User1", message.getUsername());

		assertFalse(messages.hasNext());

		verify(client, times(3)).execute(any(HttpUriRequest.class));
	}

	@Test
	public void getMessages() throws Exception {
		HttpClient client = mockClient(new AnswerImpl() {
			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (count) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, "value=\"0123456789abcdef0123456789abcdef\" <textarea id=\"input\"></textarea>");
				case 2:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/events", uri);
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", "0123456789abcdef0123456789abcdef"),
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
						"{\"event_type\":1,\"time_stamp\":1417043460,\"user_id\":51,\"user_name\":\"User2\",\"room_id\":1,\"message_id\":20157246,\"edits\":2}," +
						"{\"event_type\":1,\"time_stamp\":1417045460,\"content\":\"message 3\",\"user_id\":50,\"user_name\":\"User1\",\"room_id\":1,\"message_id\":20157247}" +
					"]}");
					//@formatter:on
				}

				return super.answer(method, uri, body);
			}
		});

		StackoverflowChat chat = new StackoverflowChat(client);
		Iterator<ChatMessage> messages = chat.getMessages(1, 3).iterator();

		ChatMessage message = messages.next();
		assertEquals("message 1", message.getContent());
		assertEquals(0, message.getEdits());
		assertEquals(20157245L, message.getMessageId());
		assertEquals(1, message.getRoomId());
		assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1417041460000L), ZoneId.systemDefault()), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User1", message.getUsername());

		message = messages.next();
		assertNull(message.getContent()); //deleted message
		assertEquals(2, message.getEdits());
		assertEquals(20157246L, message.getMessageId());
		assertEquals(1, message.getRoomId());
		assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1417043460000L), ZoneId.systemDefault()), message.getTimestamp());
		assertEquals(51, message.getUserId());
		assertEquals("User2", message.getUsername());

		message = messages.next();
		assertEquals("message 3", message.getContent());
		assertEquals(0, message.getEdits());
		assertEquals(20157247L, message.getMessageId());
		assertEquals(1, message.getRoomId());
		assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1417045460000L), ZoneId.systemDefault()), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User1", message.getUsername());

		assertFalse(messages.hasNext());

		verify(client, times(2)).execute(any(HttpUriRequest.class));
	}

	@Test
	public void getNewMessages() throws Exception {
		HttpClient client = mockClient(new AnswerImpl() {
			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (count) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, "value=\"0123456789abcdef0123456789abcdef\" <textarea id=\"input\"></textarea>");
				case 2:
					assertEquals("POST", method);
					assertEquals("https://chat.stackoverflow.com/chats/1/events", uri);
					//@formatter:off
					Set<NameValuePair> expected = new HashSet<>(Arrays.asList(
						new BasicNameValuePair("fkey", "0123456789abcdef0123456789abcdef"),
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
						new BasicNameValuePair("fkey", "0123456789abcdef0123456789abcdef"),
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
						new BasicNameValuePair("fkey", "0123456789abcdef0123456789abcdef"),
						new BasicNameValuePair("mode", "messages"),
						new BasicNameValuePair("msgCount", "5")
					));
					//@formatter:on
					actual = params(body);
					assertEquals(expected, actual);

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
						new BasicNameValuePair("fkey", "0123456789abcdef0123456789abcdef"),
						new BasicNameValuePair("mode", "messages"),
						new BasicNameValuePair("msgCount", "10")
					));
					//@formatter:on
					actual = params(body);
					assertEquals(expected, actual);

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
				}

				return super.answer(method, uri, body);
			}
		});

		StackoverflowChat chat = new StackoverflowChat(client);

		Iterator<ChatMessage> messages = chat.getNewMessages(1).iterator();
		{
			assertFalse(messages.hasNext());
		}

		messages = chat.getNewMessages(1).iterator();
		{
			ChatMessage message = messages.next();
			assertEquals("message 4", message.getContent());

			message = messages.next();
			assertEquals("message 5", message.getContent());

			assertFalse(messages.hasNext());
		}

		messages = chat.getNewMessages(1).iterator();
		{
			assertFalse(messages.hasNext());
		}

		messages = chat.getNewMessages(1).iterator();
		{
			ChatMessage message = messages.next();
			assertEquals("message 6", message.getContent());

			message = messages.next();
			assertEquals("message 7", message.getContent());

			message = messages.next();
			assertEquals("message 8", message.getContent());

			message = messages.next();
			assertEquals("message 9", message.getContent());

			message = messages.next();
			assertEquals("message 10", message.getContent());

			message = messages.next();
			assertEquals("message 11", message.getContent());

			message = messages.next();
			assertEquals("message 12", message.getContent());

			assertFalse(messages.hasNext());
		}

		verify(client, times(6)).execute(any(HttpUriRequest.class));
	}

	private static HttpClient mockClient(AnswerImpl answer) throws IOException {
		HttpClient client = mock(HttpClient.class);
		doAnswer(answer).when(client).execute(any(HttpUriRequest.class));
		return client;
	}

	private static class AnswerImpl implements Answer<HttpResponse> {
		protected int count = 0;

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

			count++;
			return answer(method, uri, body);
		}

		protected HttpResponse answer(String method, String uri, String body) throws IOException {
			fail("Request not handled.");
			return null;
		}

		protected HttpResponse response(int statusCode, String body) throws IOException {
			HttpEntity entity = new StringEntity(body);
			StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, "");

			HttpResponse response = new BasicHttpResponse(statusLine);
			response.setEntity(entity);
			return response;
		}

		protected Set<NameValuePair> params(String body) {
			return new HashSet<>(URLEncodedUtils.parse(body, Consts.UTF_8));
		}
	};
}
