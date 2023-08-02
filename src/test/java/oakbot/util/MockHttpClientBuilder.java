package oakbot.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Builds mock implementations of {@link CloseableHttpClient} objects.
 * @author Michael Angstadt
 */
public class MockHttpClientBuilder {
	private final List<ExpectedRequest> expectedRequests = new ArrayList<>();
	private final List<HttpResponse> responses = new ArrayList<>();
	private final List<IOException> responseExceptions = new ArrayList<>();

	/**
	 * Adds an expected GET request. A call to {@link #response} should be made
	 * right after this to specify the response that should be returned.
	 * @param uri the expected URI
	 * @return this
	 */
	public MockHttpClientBuilder requestGet(String uri) {
		return request("GET", uri);
	}

	/**
	 * Adds an expected POST request. A call to {@link #response} should be made
	 * right after this to specify the response that should be returned.
	 * @param uri the expected URI
	 * @param params the name/value pairs of the expected parameters
	 * @return this
	 */
	public MockHttpClientBuilder requestPost(String uri, String... params) {
		return request("POST", uri, params);
	}

	/**
	 * Adds an expected request. A call to {@link #response} should be made
	 * right after this to specify the response that should be returned.
	 * @param method the expected method (e.g. "GET")
	 * @param uri the expected URI
	 * @param params the name/value pairs of the expected parameters (only
	 * applicable for "POST" requests).
	 * @return this
	 */
	public MockHttpClientBuilder request(String method, String uri, String... params) {
		expectedRequests.add(new ExpectedRequest(method, uri, params));
		return this;
	}

	/**
	 * Defines the 200 response to send back after a request is received. This
	 * should be called right after {@link #request}.
	 * @param body the response body
	 * @return this
	 */
	public MockHttpClientBuilder responseOk(String body) {
		return response(200, body);
	}

	/**
	 * Defines the response to send back after a request is received. This
	 * should be called right after {@link #request}.
	 * @param statusCode the status code of the response (e.g. "200")
	 * @param body the response body
	 * @return this
	 */
	public MockHttpClientBuilder response(int statusCode, String body) {
		HttpEntity entity = new StringEntity(body, StandardCharsets.UTF_8);

		StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, "");

		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		when(response.getStatusLine()).thenReturn(statusLine);
		when(response.getEntity()).thenReturn(entity);
		responses.add(response);

		responseExceptions.add(null);

		return this;
	}

	/**
	 * Defines an exception to throw after sending a request. This should be
	 * called right after {@link #request}.
	 * @param exception the exception to throw
	 * @return this
	 */
	public MockHttpClientBuilder response(IOException exception) {
		responses.add(null);
		responseExceptions.add(exception);
		return this;
	}

	/**
	 * Builds the final {@link CloseableHttpClient} mock object.
	 * @return the mock object
	 */
	public CloseableHttpClient build() {
		if (expectedRequests.size() != responses.size()) {
			throw new IllegalStateException("Request/response list sizes do not match.");
		}

		CloseableHttpClient client = mock(CloseableHttpClient.class);

		try {
			when(client.execute(any(HttpUriRequest.class))).then(new Answer<HttpResponse>() {
				private int requestCount = -1;

				@Override
				public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
					requestCount++;

					if (requestCount >= expectedRequests.size()) {
						fail("The unit test only expected " + expectedRequests.size() + " HTTP requests to be sent, but more were generated.");
					}

					HttpRequest actualRequest = (HttpRequest) invocation.getArguments()[0];
					ExpectedRequest expectedRequest = expectedRequests.get(requestCount);

					assertEquals(expectedRequest.method, actualRequest.getRequestLine().getMethod());
					assertEquals(expectedRequest.uri, actualRequest.getRequestLine().getUri());

					if (actualRequest instanceof HttpPost) {
						HttpPost actualPostRequest = (HttpPost) actualRequest;
						String body = EntityUtils.toString(actualPostRequest.getEntity());
						Set<NameValuePair> params = new HashSet<>(URLEncodedUtils.parse(body, Consts.UTF_8));
						assertEquals(expectedRequest.params, params);
					}

					IOException exception = responseExceptions.get(requestCount);
					if (exception != null) {
						throw exception;
					}

					return responses.get(requestCount);
				}
			});
		} catch (IOException e) {
			//never thrown because it is a mock object
			throw new UncheckedIOException(e);
		}

		return client;
	}

	/**
	 * Represents a request that the test is expecting the code to send out.
	 * @author Michael Angstadt
	 */
	private static class ExpectedRequest {
		private final String method;
		private final String uri;
		private final Set<NameValuePair> params;

		/**
		 * @param method the expected method (e.g. "GET")
		 * @param uri the expected URI
		 * @param params the name/value pairs of the expected parameters (only
		 * applicable for "POST" requests).
		 */
		public ExpectedRequest(String method, String uri, String... params) {
			if ("GET".equals(method) && params.length > 0) {
				throw new IllegalArgumentException("GET requests cannot have parameters.");
			}
			if (params.length % 2 != 0) {
				throw new IllegalArgumentException("\"params\" vararg must have an even number of arguments.");
			}

			this.method = method;
			this.uri = uri;

			this.params = new HashSet<>();
			for (int i = 0; i < params.length; i += 2) {
				String name = params[i];
				String value = params[i + 1];
				this.params.add(new BasicNameValuePair(name, value));
			}
		}
	}
}