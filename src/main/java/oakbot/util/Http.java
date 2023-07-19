package oakbot.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper class for sending HTTP requests.
 * @author Michael Angstadt
 */
public class Http implements Closeable {
	private static final Logger logger = Logger.getLogger(Http.class.getName());

	private final CloseableHttpClient client;

	/**
	 * @param client the HTTP client object to wrap
	 */
	public Http(CloseableHttpClient client) {
		this.client = Objects.requireNonNull(client);
	}

	/**
	 * Sends an HTTP GET request.
	 * @param uri the URI
	 * @return the response
	 * @throws IOException if there's a problem sending the request
	 */
	public Response get(String uri) throws IOException {
		HttpGet request = new HttpGet(uri);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Sending request [method=GET; URI=" + uri + "]...");
		}

		return send(request);
	}

	/**
	 * Sends an HTTP POST request.
	 * @param uri the URI
	 * @param parameters the parameters to include in the request body. The
	 * items in this vararg consist of alternating key/value pairs. For example,
	 * the first item is the name of the first parameter, the second item is the
	 * value of the first parameter, the third item is the name of the second
	 * parameter, and so on. Therefore, this vararg must contain an even number
	 * of arguments. Names may not be null. Null values will output the string
	 * "null".
	 * @return the response
	 * @throws IOException if there's a problem sending the request
	 * @throws IllegalArgumentException if there is an odd number of arguments
	 * in the "parameters" vararg
	 */
	public Response post(String uri, Object... parameters) throws IOException {
		if (parameters.length % 2 != 0) {
			throw new IllegalArgumentException("\"parameters\" vararg must have an even number of values.");
		}

		HttpPost request = new HttpPost(uri);

		if (parameters.length > 0) {
			List<NameValuePair> params = new ArrayList<>(parameters.length / 2);
			for (int i = 0; i < parameters.length; i += 2) {
				String name = parameters[i].toString();
				String value = Objects.toString(parameters[i + 1]);
				params.add(new BasicNameValuePair(name, value));
			}
			request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Sending request [method=POST; URI=" + uri + "; params=" + Arrays.toString(parameters) + "]...");
		}

		return send(request);
	}

	/**
	 * <p>
	 * Sends an HTTP request.
	 * </p>
	 * <p>
	 * The chat system returns an HTTP 409 response if the client sends too many
	 * requests too quickly. This method automatically handles such responses by
	 * sleeping the requested amount of time, and then re-sending the request.
	 * It will do this up to five times before giving up, at which point an
	 * {@code IOException} will be thrown.
	 * </p>
	 * @param request the request to send
	 * @return the response
	 * @throws IOException if there was a problem sending the request
	 */
	private Response send(HttpUriRequest request) throws IOException {
		long sleep = 0;
		int attempts = 0;

		while (attempts < 5) {
			if (sleep > 0) {
				logger.info("Sleeping for " + sleep + "ms before resending the request...");
				try {
					Sleeper.sleep(sleep);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			Response response;
			try (CloseableHttpResponse httpResponse = client.execute(request)) {
				int statusCode = httpResponse.getStatusLine().getStatusCode();

				HttpEntity entity = httpResponse.getEntity();
				ContentType contentType = ContentType.getOrDefault(entity);
				byte[] body = EntityUtils.toByteArray(entity);

				response = new Response(statusCode, body, contentType, request.getURI().toString());
			}

			/*
			 * An HTTP 409 response means that the bot is sending messages too
			 * quickly. The response body contains the number of seconds the bot
			 * must wait before it can post another message.
			 */
			if (response.getStatusCode() == 409) {
				String body = response.getBody();
				Long waitTime = parse409Response(body);
				sleep = (waitTime == null) ? 5000 : waitTime;

				logger.info("HTTP " + response.getStatusCode() + " response, sleeping " + sleep + "ms [request-method=" + request.getMethod() + "; request-URI=" + request.getURI() + "]: " + body);

				attempts++;
				continue;
			}

			if (logger.isLoggable(Level.FINE)) {
				String bodyDebug;
				try {
					JsonNode node = response.getBodyAsJson();
					bodyDebug = JsonUtils.prettyPrint(node);
				} catch (JsonProcessingException e) {
					//not JSON
					bodyDebug = response.getBody();
				}
				logger.fine("Received response [status=" + response.getStatusCode() + "; request-method=" + request.getMethod() + "; request-URI=" + request.getURI() + "]: " + bodyDebug);
			}

			return response;
		}

		throw new IOException("Request could not be sent after " + attempts + " attempts [request-method=" + request.getMethod() + "; request-URI=" + request.getURI() + "].");
	}

	private static final Pattern response409Regex = Pattern.compile("\\d+");

	/**
	 * Parses the wait time out of an HTTP 409 response. An HTTP 409 response
	 * indicates that the bot is sending messages too quickly.
	 * @param body the response body (e.g. "You can perform this action again in
	 * 2 seconds")
	 * @return the amount of time (in milliseconds) the bot must wait before the
	 * chat system will accept the request, or null if this value could not be
	 * parsed from the response body
	 */
	private static Long parse409Response(String body) {
		Matcher m = response409Regex.matcher(body);
		if (!m.find()) {
			return null;
		}

		int seconds = Integer.parseInt(m.group(0));
		return TimeUnit.SECONDS.toMillis(seconds);
	}

	/**
	 * Gets the wrapped HTTP client object.
	 * @return the wrapped client object
	 */
	public CloseableHttpClient getClient() {
		return client;
	}

	@Override
	public void close() throws IOException {
		client.close();
	}

	/**
	 * Represents an HTTP response.
	 * @author Michael Angstadt
	 */
	public static class Response {
		private final int statusCode;
		private final byte[] body;
		private final ContentType contentType;
		private final String requestUri;

		/**
		 * @param statusCode the status code (e.g. 200)
		 * @param body the response body
		 * @param contentType the content type of the body
		 * @param requestUri the request URI that generated this response
		 */
		public Response(int statusCode, byte[] body, ContentType contentType, String requestUri) {
			this.statusCode = statusCode;
			this.body = body;
			this.contentType = contentType;
			this.requestUri = requestUri;
		}

		/**
		 * Gets the status code.
		 * @return the status code (e.g. 200)
		 */
		public int getStatusCode() {
			return statusCode;
		}

		/**
		 * Gets the response body.
		 * @return the response body
		 */
		public String getBody() {
			return new String(body, contentType.getCharset());
		}

		/**
		 * Parses the response body as JSON.
		 * @return the parsed JSON
		 * @throws JsonProcessingException if the body could not be parsed as
		 * JSON
		 */
		public JsonNode getBodyAsJson() throws JsonProcessingException {
			return new ObjectMapper().readTree(getBody());
		}

		/**
		 * Parses the response body as HTML.
		 * @return the parsed HTML document
		 */
		public Document getBodyAsHtml() {
			return Jsoup.parse(getBody(), requestUri);
		}

		/**
		 * Parses the response body as XML.
		 * @return the parsed XML document
		 * @throws SAXException if there's a problem parsing the XML
		 */
		public Leaf getBodyAsXml() throws SAXException {
			return Leaf.parse(body);
		}
	}
}
