package oakbot.util;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.github.mangstadt.sochat4j.util.Http;

/**
 * Use this class when you want to send normal HTTP requests in production, but
 * want to inject a mock HTTP client during unit testing.
 * @author Michael Angstadt
 */
public class HttpFactory {
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
		CloseableHttpClient client = (mock == null) ? HttpClients.createDefault() : mock;
		return new Http(client);
	}

	/**
	 * Creates an HTTP client or returns a mock client injected using
	 * {@link #inject}.
	 * @param cookieStore the cookies to use
	 * @return the HTTP client
	 */
	public static Http connect(CookieStore cookieStore) {
		CloseableHttpClient client = (mock == null) ? HttpClients.custom().setDefaultCookieStore(cookieStore).build() : mock;
		return new Http(client);
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
