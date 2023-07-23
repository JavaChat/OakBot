package oakbot.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import oakbot.util.Sleeper;

/**
 * @author Michael Angstadt
 */
@SuppressWarnings("resource")
public class ChatHttpTest {
	@Before
	public void before() {
		Sleeper.startUnitTest();
	}

	@After
	public void after() {
		Sleeper.endUnitTest();
	}

	/**
	 * Retry the request when a HTTP 409 response is returned.
	 */
	@Test
	public void http_409_response() throws Exception {
		CloseableHttpResponse response1 = mockResponse(409, "You can perform this action again in 2 seconds");
		CloseableHttpResponse response2 = mockResponse(200, "");

		CloseableHttpClient client = mock(CloseableHttpClient.class);
		when(client.execute(any(HttpUriRequest.class))).thenReturn(response1, response2);

		ChatHttp http = new ChatHttp(client);
		http.get("uri");
		assertEquals(2000, Sleeper.getTimeSlept());
	}

	/**
	 * If it cannot parse the wait time from a HTTP 409 response, then wait 5
	 * seconds before retrying the request.
	 */
	@Test
	public void http_409_response_cannot_parse_wait_time() throws Exception {
		CloseableHttpResponse response1 = mockResponse(409, "Leave me alone!");
		CloseableHttpResponse response2 = mockResponse(200, "");

		CloseableHttpClient client = mock(CloseableHttpClient.class);
		when(client.execute(any(HttpUriRequest.class))).thenReturn(response1, response2);

		ChatHttp http = new ChatHttp(client);

		http.get("uri");
		assertEquals(5000, Sleeper.getTimeSlept());
	}

	/**
	 * If HTTP 409 responses are continually returned, give up after five tries.
	 */
	@Test
	public void http_409_response_five_tries() throws Exception {
		CloseableHttpResponse response = mockResponse(409, "You can perform this action again in 2 seconds");

		CloseableHttpClient client = mock(CloseableHttpClient.class);
		when(client.execute(any(HttpUriRequest.class))).thenReturn(response);

		ChatHttp http = new ChatHttp(client);

		try {
			http.get("uri");
			fail();
		} catch (IOException e) {
			assertEquals(8000, Sleeper.getTimeSlept());
		}
	}

	private static CloseableHttpResponse mockResponse(int statusCode, String body) throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, ""));
		when(response.getEntity()).thenReturn(new StringEntity(body));
		return response;
	}
}
