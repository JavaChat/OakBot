package oakbot.chat;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import oakbot.util.Http;
import oakbot.util.Http.Response;

/**
 * Represents a Stack Exchange site.
 * @author Michael Angstadt
 */
public class Site {
	/**
	 * stackoverflow.com
	 */
	public static final Site STACKOVERFLOW = new Site("stackoverflow.com");

	/**
	 * meta.stackexchange.com
	 */
	public static final Site META = new Site("meta.stackexchange.com");

	/**
	 * stackexchange.com
	 */
	public static final Site STACKEXCHANGE = new Site("stackexchange.com");

	private final String domain;

	/**
	 * @param domain the site's domain name (e.g. "stackoverflow.com")
	 */
	public Site(String domain) {
		this.domain = requireNonNull(domain);
	}

	/**
	 * Gets the site's domain name.
	 * @return the domain name (e.g. "stackoverflow.com)
	 */
	public String getDomain() {
		return domain;
	}

	public String getLoginDomain() {
		return (this == STACKEXCHANGE) ? META.getLoginDomain() : domain;
	}

	/**
	 * Gets the domain name of the site's chat service.
	 * @return the chat domain name (e.g. "chat.stackoverflow.com")
	 */
	public String getChatDomain() {
		return "chat." + domain;
	}

	/**
	 * Logs into the site.
	 * @param email the email address
	 * @param password the password
	 * @param http the HTTP client
	 * @return true if the login was successful, false if the credentials were
	 * bad
	 * @throws IOException if an unexpected error occurs
	 */
	public boolean login(String email, String password, Http http) throws IOException {
		//@formatter:off
		String url = new URIBuilder()
			.setScheme("https")
			.setHost(getLoginDomain())
			.setPath("/users/login")
		.toString();
		//@formatter:on

		Response response = http.get(url);
		Document loginPage = response.getBodyAsHtml();

		Elements elements = loginPage.select("input[name=fkey]");
		if (elements.isEmpty()) {
			throw new IOException("\"fkey\" field not found on Stack Exchange login page, cannot login.");
		}
		String fkey = elements.first().attr("value");

		//@formatter:off
		response = http.post(url,
			"email", email,
			"password", password,
			"fkey", fkey
		);
		//@formatter:on

		int statusCode = response.getStatusCode();
		return (statusCode == 302);
	}
}
