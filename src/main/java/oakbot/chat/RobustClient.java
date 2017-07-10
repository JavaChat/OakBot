package oakbot.chat;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Executes an HTTP request, retrying after a short pause if the request fails
 * due to glitchy network problems such as socket timeouts and empty HTTP
 * responses.
 * @author Michael Angstadt
 */
public class RobustClient {
	private static final Logger logger = Logger.getLogger(RobustClient.class.getName());
	private static final Pattern response409Regex = Pattern.compile("\\d+");

	private final CloseableHttpClient client;
	private final HttpUriRequest request;

	private long retryPause = 5000;
	private int maxAttempts = 3, attempts;
	private List<Integer> expectedStatusCodes;

	/**
	 * @param client the HTTP client
	 * @param request the request to send
	 */
	public RobustClient(CloseableHttpClient client, HttpUriRequest request) {
		this.client = client;
		this.request = request;
	}

	/**
	 * Sets the amount of time to wait between retries (defaults to 5 seconds).
	 * @param retryPause the amount of time in milliseconds
	 * @return this
	 */
	public RobustClient retryPause(long retryPause) {
		this.retryPause = retryPause;
		return this;
	}

	/**
	 * Sets the number of times to try sending the request before giving up
	 * (defaults to 3).
	 * @param attempts the number of attempts (must be greater than zero)
	 * @return this
	 */
	public RobustClient attempts(Integer attempts) {
		this.maxAttempts = attempts;
		return this;
	}

	/**
	 * <p>
	 * Sets the status code(s) that are expected to be returned in the response.
	 * If one of the status codes in this list is not returned, then the request
	 * will be retried. By default, ALL status codes are accepted.
	 * </p>
	 * <p>
	 * HTTP 404 responses are always treated as valid and are always returned.
	 * HTTP 409 responses, which indicate that the bot is sending messages too
	 * quickly, are automatically retried.
	 * </p>
	 * @param statusCodes the status codes
	 * @return this
	 */
	public RobustClient statusCodes(Integer... statusCodes) {
		this.expectedStatusCodes = Arrays.asList(statusCodes);
		return this;
	}

	/**
	 * Sends the request, parsing the response body as JSON. If the body does
	 * not contain valid JSON, then the request is retried.
	 * @return the response
	 * @throws IOException if a valid response was not returned after the
	 * specified number of attempts
	 */
	public JsonResponse asJson() throws IOException {
		attempts = 0;

		ObjectMapper mapper = new ObjectMapper();
		while (attempts < maxAttempts) {
			try (CloseableHttpResponse response = execute()) {
				if (response.getStatusLine().getStatusCode() == 404) {
					return new JsonResponse(response, true);
				}

				JsonNode node;
				try (InputStream in = response.getEntity().getContent()) {
					node = mapper.readTree(in);
				}
				return new JsonResponse(response, node);
			} catch (JsonParseException e) {
				//make the request again if a non-JSON response is returned
				logger.log(Level.SEVERE, "Could not parse the response body as JSON.  Retrying the request in " + retryPause + "ms.", e);

				try {
					Thread.sleep(retryPause);
				} catch (InterruptedException e2) {
					throw new RuntimeException(e2);
				}
			}
		}

		throw new IOException("Was expecting a JSON response body from " + request.getURI() + ", but never got parseable JSON after " + attempts + " attempts.");
	}

	/**
	 * Sends the request.
	 * @return the response
	 * @throws IOException if a valid response was not returned after the
	 * specified number of attempts
	 */
	public CloseableHttpResponse asHttp() throws IOException {
		attempts = 0;
		return execute();
	}

	/**
	 * Sends the request.
	 * @return the response
	 * @throws IOException if a valid response was not returned after the
	 * specified number of attempts
	 */
	private CloseableHttpResponse execute() throws IOException {
		long sleep = 0;
		final long maxSleep = TimeUnit.SECONDS.toMillis(60);
		while (attempts < maxAttempts) {
			attempts++;

			if (sleep > 0) {
				logger.info("Sleeping for " + sleep + " ms before resending the request...");
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			/*
			 * Update the sleep amount for the next attempt (if there is one).
			 */
			sleep = attempts * retryPause;
			if (sleep > maxSleep) {
				sleep = maxSleep;
			}

			/*
			 * Send the request.
			 */
			CloseableHttpResponse response;
			try {
				response = client.execute(request);
			} catch (NoHttpResponseException | SocketException | ConnectTimeoutException | SSLHandshakeException e) {
				logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown from request " + request.getURI() + ". Retrying.", e);
				continue;
			}

			int actualStatusCode = response.getStatusLine().getStatusCode();

			/*
			 * An HTTP 409 response means that the bot is sending messages too
			 * quickly. The response body contains the number of seconds the bot
			 * must wait before it can post another message.
			 */
			if (actualStatusCode == 409) {
				String body = EntityUtils.toString(response.getEntity());
				logger.info("HTTP " + actualStatusCode + " returned [url=" + request.getURI() + "]: " + body);

				Long waitTime = parse409Response(body);
				sleep = (waitTime == null) ? 5000 : waitTime;

				//do not count this against the max attempts
				attempts--;

				HttpClientUtils.closeQuietly(response);
				continue;
			}

			/**
			 * The bot sometimes gets HTTP 429 ("too many requests") responses
			 * when pinging the chat rooms for messages. This issue first
			 * occurred when the bot was in seven rooms at once, so it may have
			 * reached some kind of usage limit. It appears the bot can be in
			 * five rooms at a time without triggering this response code.
			 */
			if (actualStatusCode == 429) {
				String body = EntityUtils.toString(response.getEntity());
				logger.info("HTTP " + actualStatusCode + " returned [url=" + request.getURI() + "]: " + body);

				sleep = 5000;
				HttpClientUtils.closeQuietly(response);
				continue;
			}

			/*
			 * Different requests handle 404s differently, so return the
			 * response if it's a 404.
			 */
			if (actualStatusCode == 404) {
				return response;
			}

			/*
			 * If the status code was incorrect, re-send the request.
			 */
			if (expectedStatusCodes != null && !expectedStatusCodes.contains(actualStatusCode)) {
				String body = EntityUtils.toString(response.getEntity());
				logger.severe("The following status codes were expected " + expectedStatusCodes + ", but the actual status code was " + actualStatusCode + ". Retrying.  The response body was: " + body);

				HttpClientUtils.closeQuietly(response);
				continue;
			}

			return response;
		}

		throw new IOException("Request to " + request.getURI() + " could not be sent after " + attempts + " attempts.");
	}

	/**
	 * Parses an HTTP 409 response, which indicates that the bot is sending
	 * messages too quickly.
	 * @param response the HTTP 409 response body (e.g. "You can perform this
	 * action again in 2 seconds")
	 * @return the amount of time (in milliseconds) the bot must wait before the
	 * chat system will accept new messages, or null if this value could not be
	 * parsed from the response
	 * @throws IOException if there's a problem getting the response body
	 */
	private static Long parse409Response(String body) throws IOException {
		Matcher m = response409Regex.matcher(body);
		if (!m.find()) {
			return null;
		}

		int seconds = Integer.parseInt(m.group(0));
		return TimeUnit.SECONDS.toMillis(seconds);
	}

	/**
	 * Represents an HTTP response whose body contains JSON.
	 * @author Michael Angstadt
	 */
	public static class JsonResponse {
		private final CloseableHttpResponse response;
		private final JsonNode body;
		private final boolean http404;

		public JsonResponse(CloseableHttpResponse response, JsonNode body) {
			this.response = response;
			this.body = body;
			this.http404 = false;
		}

		public JsonResponse(CloseableHttpResponse response, boolean http404) {
			this.response = response;
			this.body = null;
			this.http404 = true;
		}

		public CloseableHttpResponse getResponse() {
			return response;
		}

		public JsonNode getBody() {
			return body;
		}

		public boolean isHttp404() {
			return http404;
		}
	}
}