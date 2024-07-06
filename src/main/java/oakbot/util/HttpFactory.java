package oakbot.util;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import com.github.mangstadt.sochat4j.util.Http;

import okhttp3.OkHttpClient;

/**
 * Use this class when you want to send normal HTTP requests in production, but
 * want to inject a mock HTTP client during unit testing.
 * @author Michael Angstadt
 */
public class HttpFactory {
	private static final OkHttpClient okHttpClient = new OkHttpClient();

	private static CloseableHttpClient mock;

	/**
	 * Injects a mock HTTP client for unit testing.
	 * @param mock the mock client
	 */
	public static void inject(CloseableHttpClient mock) {
		HttpFactory.mock = mock;
	}

	/**
	 * Creates an HTTP client or returns a mock client injected using
	 * {@link #inject}.
	 * @return the HTTP client
	 */
	public static Http connect() {
		var client = (mock == null) ? HttpClients.createDefault() : mock;
		return new Http(client);
	}

	/**
	 * Creates an HTTP client or returns a mock client injected using
	 * {@link #inject}.
	 * @param builder the client builder object
	 * @return the HTTP client
	 */
	public static Http connect(HttpClientBuilder builder) {
		var client = (mock == null) ? builder.build() : mock;
		return new Http(client);
	}

	/**
	 * Creates an HTTP client or returns a mock client injected using
	 * {@link #inject}.
	 * @param cookieStore the cookies to use
	 * @return the HTTP client
	 */
	public static Http connect(CookieStore cookieStore) {
		var client = (mock == null) ? HttpClients.custom().setDefaultCookieStore(cookieStore).build() : mock;
		return new Http(client);
	}

	/**
	 * Returns the shared OkHttpClient instance. Only a single instance should
	 * be created for performance reasons.
	 * @return the client instance
	 */
	public static OkHttpClient okHttp() {
		return okHttpClient;
	}

	/**
	 * Removes the mock HTTP client, if present.
	 */
	public static void restore() {
		mock = null;
	}

	private HttpFactory() {
		//hide constructor
	}
}
