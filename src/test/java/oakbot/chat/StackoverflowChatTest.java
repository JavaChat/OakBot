package oakbot.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
	public void sendMessage() throws Exception {
		HttpClient client = mockClient(new AnswerImpl() {
			@Override
			public HttpResponse answer(String method, String uri, String body) throws IOException {
				switch (count) {
				case 1:
					assertEquals("GET", method);
					assertEquals("https://chat.stackoverflow.com/rooms/1", uri);
					return response(200, "value=\"0123456789abcdef0123456789abcdef\"");
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
				}

				return super.answer(method, uri, body);
			}
		});

		StackoverflowChat chat = new StackoverflowChat(client);
		chat.sendMessage(1, "Test1");
		chat.sendMessage(1, "Test2");
		verify(client, times(3)).execute(any(HttpUriRequest.class));
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
			HttpEntity entity = null;
			if (body != null) {
				entity = mock(HttpEntity.class);
				when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
			}

			StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, "");

			HttpResponse response = mock(HttpResponse.class);
			when(response.getEntity()).thenReturn(entity);
			when(response.getStatusLine()).thenReturn(statusLine);

			return response;
		}

		protected Set<NameValuePair> params(String body) {
			return new HashSet<>(URLEncodedUtils.parse(body, Consts.UTF_8));
		}
	};
}
