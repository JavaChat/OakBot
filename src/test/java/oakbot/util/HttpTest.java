package oakbot.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author Michael Angstadt
 */
@SuppressWarnings("resource")
public class HttpTest {
	/**
	 * Make sure it generates the Response object properly.
	 */
	@Test
	public void response() throws Exception {
		CloseableHttpResponse r = mockResponse(200, "The body");
		CloseableHttpClient client = mock(CloseableHttpClient.class);
		when(client.execute(any(HttpUriRequest.class))).thenReturn(r);

		Http http = new Http(client);

		Http.Response response = http.get("uri");
		assertEquals(200, response.getStatusCode());
		assertEquals("The body", response.getBody());
		try {
			response.getBodyAsJson();
			fail();
		} catch (JsonProcessingException e) {
			//expected
		}
	}

	/**
	 * Make sure it generates the Response object properly.
	 */
	@Test
	public void response_json() throws Exception {
		CloseableHttpResponse r = mockResponse(200, "{}");
		CloseableHttpClient client = mock(CloseableHttpClient.class);
		when(client.execute(any(HttpUriRequest.class))).thenReturn(r);

		Http http = new Http(client);

		Http.Response response = http.get("uri");
		assertEquals(200, response.getStatusCode());
		assertEquals("{}", response.getBody());
		assertNotNull(response.getBodyAsJson());
	}

	/**
	 * Vararg parameter must have an even number of arguments.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void post_parameters_odd() throws Exception {
		Http http = new Http(mock(CloseableHttpClient.class));
		http.post("uri", "one");
	}

	/**
	 * Vararg parameter may be empty.
	 */
	@Test
	public void post_parameters_none() throws Exception {
		CloseableHttpClient client = mock(CloseableHttpClient.class);
		when(client.execute(any(HttpUriRequest.class))).then(invocation -> {
			HttpPost request = (HttpPost) invocation.getArguments()[0];
			assertNull(request.getEntity());
			return mockResponse(200, "");
		});

		Http http = new Http(client);
		http.post("uri");
		verify(client).execute(any(HttpUriRequest.class));
	}

	/**
	 * Parameter names cannot be null.
	 */
	@Test(expected = NullPointerException.class)
	public void post_parameters_null_name() throws Exception {
		Http http = new Http(mock(CloseableHttpClient.class));
		http.post("uri", null, "value");
	}

	/**
	 * Parameter values may be null.
	 */
	@Test
	public void post_parameters_null_value() throws Exception {
		CloseableHttpClient client = mock(CloseableHttpClient.class);
		when(client.execute(any(HttpUriRequest.class))).then(invocation -> {
			HttpPost request = (HttpPost) invocation.getArguments()[0];
			String body = EntityUtils.toString(request.getEntity());
			assertEquals("one=null", body);

			return mockResponse(200, "");
		});

		Http http = new Http(client);
		http.post("uri", "one", null);
		verify(client).execute(any(HttpUriRequest.class));
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

		HttpSleepless http = new HttpSleepless(client);
		http.get("uri");
		assertEquals(2000, http.timeSlept);

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

		HttpSleepless http = new HttpSleepless(client);

		http.get("uri");
		assertEquals(5000, http.timeSlept);
	}

	/**
	 * If HTTP 409 responses are continually returned, give up after five tries.
	 */
	@Test
	public void http_409_response_five_tries() throws Exception {
		CloseableHttpResponse response = mockResponse(409, "You can perform this action again in 2 seconds");

		CloseableHttpClient client = mock(CloseableHttpClient.class);
		when(client.execute(any(HttpUriRequest.class))).thenReturn(response);

		HttpSleepless http = new HttpSleepless(client);

		try {
			http.get("uri");
			fail();
		} catch (IOException e) {
			assertEquals(8000, http.timeSlept);
		}
	}

	/**
	 * Create a version of the {@link Http} class that does not actually call
	 * {@link Thread#sleep} so that the unit test does not take forever to run.
	 * @author Michael Angstadt
	 */
	private static class HttpSleepless extends Http {
		private long timeSlept;

		public HttpSleepless(CloseableHttpClient client) {
			super(client);
		}

		@Override
		void sleep(long ms) {
			timeSlept += ms;
		}
	}

	private static CloseableHttpResponse mockResponse(int statusCode, String body) throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, ""));
		when(response.getEntity()).thenReturn(new StringEntity(body));
		return response;
	}
}
