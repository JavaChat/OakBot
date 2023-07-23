package oakbot.util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
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

	protected final CloseableHttpClient client;

	/**
	 * @param client the HTTP client object to wrap
	 */
	public Http(CloseableHttpClient client) {
		this.client = Objects.requireNonNull(client);
	}

	/**
	 * Sends an HTTP HEAD request.
	 * @param uri the URI
	 * @return the response
	 * @throws IOException if there's a problem sending the request
	 */
	public Response head(String uri) throws IOException {
		HttpHead request = new HttpHead(uri);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Sending request [method=HEAD; URI=" + uri + "]...");
		}

		return send(request);
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
	 * Sends an HTTP request.
	 * @param request the request to send
	 * @return the response
	 * @throws IOException if there was a problem sending the request
	 */
	protected Response send(HttpUriRequest request) throws IOException {
		try (CloseableHttpResponse response = client.execute(request)) {
			int statusCode = response.getStatusLine().getStatusCode();

			HttpEntity entity = response.getEntity();
			ContentType contentType;
			byte[] body;
			if (entity == null) {
				contentType = null;
				body = null;
			} else {
				contentType = ContentType.getOrDefault(entity);
				body = EntityUtils.toByteArray(entity);
			}

			return new Response(statusCode, body, contentType, request.getURI().toString());
		}
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
		 * @return the response body or null if there is no body (e.g. HEAD
		 * requests)
		 */
		public String getBody() {
			if (body == null) {
				return null;
			}

			Charset charset = contentType.getCharset();
			return (charset == null) ? new String(body) : new String(body, charset);
		}

		/**
		 * Parses the response body as JSON.
		 * @return the parsed JSON or null if there is no response body (e.g.
		 * HEAD requests)
		 * @throws JsonProcessingException if the body could not be parsed as
		 * JSON
		 */
		public JsonNode getBodyAsJson() throws JsonProcessingException {
			String bodyStr = getBody();
			return (bodyStr == null) ? null : new ObjectMapper().readTree(bodyStr);
		}

		/**
		 * Parses the response body as JSON and maps the data to a Java class.
		 * @param clazz the class
		 * @return the parsed JSON or null if there is no response body (e.g.
		 * HEAD requests)
		 * @throws JsonProcessingException if the body could not be parsed as
		 * JSON
		 */
		public <T> T getBodyAsJson(Class<T> clazz) throws JsonProcessingException {
			String bodyStr = getBody();
			return (bodyStr == null) ? null : new ObjectMapper().readValue(bodyStr, clazz);
		}

		/**
		 * Parses the response body as HTML.
		 * @return the parsed HTML document or null if there is no response body
		 * (e.g. HEAD requests)
		 */
		public Document getBodyAsHtml() {
			String bodyStr = getBody();
			return (bodyStr == null) ? null : Jsoup.parse(bodyStr, requestUri);
		}

		/**
		 * Parses the response body as XML.
		 * @return the parsed XML document or null if there is no response body
		 * (e.g. HEAD requests)
		 * @throws SAXException if there's a problem parsing the XML
		 */
		public Leaf getBodyAsXml() throws SAXException {
			return (body == null) ? null : Leaf.parse(body);
		}
	}
}
