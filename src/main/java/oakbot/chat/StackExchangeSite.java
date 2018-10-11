package oakbot.chat;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import oakbot.util.Http;
import oakbot.util.Http.Response;

/**
 * Represents the stackexchange.com site.
 * @author Michael Angstadt
 */
public class StackExchangeSite extends Site {
	public StackExchangeSite() {
		super("stackexchange.com");
	}

	@Override
	public boolean login(String email, String password, Http http) throws IOException {
		/*
		 * Get the URL to the login form.
		 * 
		 * Example: https://openid.stackexchange.com/affiliate/form?affId=11&
		 * background=transparent&callback=https%3a%2f%2fstackexchange.com%
		 * 2fusers%2fauthenticate&color=black&nonce=81O%2fWwAAAAD1mwDW%
		 * 2fyXSUw%3d%3d&openid.sreg.requested=email&signupByDefault=false&
		 * onLoad=signin-loaded&authCode=
		 * TrX5Gf3aoRTouzFN9PMsnW23ysM7Emx3zPqwdy%2f28hX14t%
		 * 2bzrCOpA6DGhbyHKB0c1tIBe9SwJulOYd587r9da%2fgu5EPrMmO117qvygwCje8k
		 * %2bCao9cw0laAxXhLx1OMJn%2fTbn7OOvOciTAunlqUhdhD%2b20j5RS%
		 * 2blwvNGOpW4pWs%3d
		 */
		String loginFormUrl = http.get("https://" + getDomain() + "/users/signin").getBody();

		/*
		 * Get the login form.
		 */
		Document loginForm = http.get(loginFormUrl).getBodyAsHtml();

		/*
		 * Extract data from the login form.
		 */
		String action, fkey, affId;
		{
			Element form = loginForm.select("div[class=login-form] form").first();
			if (form == null) {
				throw new IOException("Stack Exchange login form not found. Cannot login.");
			}

			action = form.absUrl("action");
			if (action.isEmpty()) {
				throw new IOException("Stack Exchange login form has no action attribute. Cannot login.");
			}

			Elements elements = form.select("input[name=fkey]");
			if (elements.isEmpty()) {
				throw new IOException("\"fkey\" field not found on Stack Exchange login page. Cannot login.");
			}
			fkey = elements.first().attr("value");

			elements = form.select("input[name=affId]");
			if (elements.isEmpty()) {
				throw new IOException("\"affId\" field not found on Stack Exchange login page. Cannot login.");
			}
			affId = elements.first().attr("value");
		}

		/*
		 * Submit the login form.
		 */
		Response response = http.post(action, //@formatter:off
			"email", email,
			"password", password,
			"fkey", fkey,
			"affId", affId //@formatter:on
		);

		/*
		 * Extract the redirect URL from the response.
		 * 
		 * If this URL can't be found, it probably means the login credentials
		 * were bad. The HTTP status code of the response cannot be used because
		 * it's always 200, no matter if the credentials were good or not.
		 */
		String redirectUrl;
		{
			Pattern p = Pattern.compile("var target = '(.*?)'");
			Matcher m = p.matcher(response.getBody());
			if (!m.find()) {
				return false;
			}
			redirectUrl = m.group(1);
		}

		/*
		 * Go to the redirect URL to complete the login process.
		 */
		http.get(redirectUrl);

		return true;
	}
}
