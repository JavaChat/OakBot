package oakbot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
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
	private final List<Consumer<HttpRequest>> expectedRequests = new ArrayList<>();
	private final List<HttpResponse> responses = new ArrayList<>();
	private final List<IOException> responseExceptions = new ArrayList<>();

	/**
	 * Adds an expected GET request. A call to {@link #response} should be made
	 * right after this to specify the response that should be returned.
	 * @param uri the expected URI
	 * @return this
	 */
	public MockHttpClientBuilder requestGet(String uri) {
		return request("GET", uri, null);
	}

	/**
	 * Adds an expected POST request. A call to {@link #response} should be made
	 * right after this to specify the response that should be returned.
	 * @param uri the expected URI
	 * @param params the name/value pairs of the expected parameters
	 * @return this
	 */
	public MockHttpClientBuilder requestPost(String uri, String... params) {
		return request("POST", uri, null, params);
	}

	/**
	 * Adds an expected request. A call to {@link #response} should be made
	 * right after this to specify the response that should be returned.
	 * @param method the expected method (e.g. "GET")
	 * @param uri the expected URI
	 * @param additionalTests additional tests to run (can be null)
	 * @param params the name/value pairs of the expected parameters (only
	 * applicable for "POST" requests).
	 * @return this
	 */
	public MockHttpClientBuilder request(String method, String uri, Consumer<HttpRequest> additionalTests, String... params) {
		expectedRequests.add(request -> {
			assertEquals(method, request.getRequestLine().getMethod());
			assertEquals(uri, request.getRequestLine().getUri());

			if (additionalTests != null) {
				additionalTests.accept(request);
			}

			if (params.length > 0) {
				var expectedParams = arrayToNameValuePairs(params);
				var actualParams = extractPostParams(request);
				assertEquals(expectedParams, actualParams);
			}
		});

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
	 * Defines the 200 response to send back after a request is received. This
	 * should be called right after {@link #request}.
	 * @param body the response body
	 * @param contentType the response content type
	 * @return this
	 */
	public MockHttpClientBuilder responseOk(byte[] body, ContentType contentType) {
		return response(200, body, contentType);
	}

	/**
	 * Defines the response to send back after a request is received. This
	 * should be called right after {@link #request}.
	 * @param statusCode the status code of the response (e.g. "200")
	 * @param body the response body
	 * @return this
	 */
	public MockHttpClientBuilder response(int statusCode, String body) {
		var entity = new StringEntity(body, StandardCharsets.UTF_8);
		return response(statusCode, entity);
	}

	/**
	 * Defines the response to send back after a request is received. This
	 * should be called right after {@link #request}.
	 * @param statusCode the status code of the response (e.g. "200")
	 * @param body the response body
	 * @param contentType the response content type
	 * @return this
	 */
	public MockHttpClientBuilder response(int statusCode, byte[] body, ContentType contentType) {
		var entity = new ByteArrayEntity(body, contentType);
		return response(statusCode, entity);
	}

	private MockHttpClientBuilder response(int statusCode, HttpEntity entity) {
		var statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, "");

		var response = mock(CloseableHttpResponse.class);
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

		var client = mock(CloseableHttpClient.class);

		try {
			when(client.execute(any(HttpUriRequest.class))).then(new Answer<HttpResponse>() {
				private int requestCount = -1;

				@Override
				public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
					requestCount++;

					if (requestCount >= expectedRequests.size()) {
						fail("The unit test only expected " + expectedRequests.size() + " HTTP requests to be sent, but more were generated.");
					}

					var actualRequest = (HttpRequest) invocation.getArguments()[0];
					expectedRequests.get(requestCount).accept(actualRequest);

					var exception = responseExceptions.get(requestCount);
					if (exception != null) {
						throw exception;
					}

					return responses.get(requestCount);
				}
			});
		} catch (IOException e) {
			//never thrown because we're just setting up a mock object
			throw new UncheckedIOException(e);
		}

		return client;
	}

	private Set<NameValuePair> extractPostParams(HttpRequest request) {
		if (!(request instanceof HttpEntityEnclosingRequest entityRequest)) {
			return Set.of();
		}

		String body;
		try {
			body = EntityUtils.toString(entityRequest.getEntity());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return new HashSet<>(URLEncodedUtils.parse(body, Consts.UTF_8));
	}

	private Set<NameValuePair> arrayToNameValuePairs(String... params) {
		return IntStream.iterate(0, i -> i < params.length, i -> i + 2).mapToObj(i -> {
			var name = params[i];
			var value = params[i + 1];
			return new BasicNameValuePair(name, value);
		}).collect(Collectors.toSet());
	}
}
