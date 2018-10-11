package oakbot.chat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import oakbot.util.Http;

/**
 * @author Michael Angstadt
 */
public class StackExchangeSiteTest {
	private final String exampleLoginFormUrl = "https://openid.stackexchange.com/affiliate/form?" + //@formatter:off
		"affId=11&" +
		"background=transparent&" +
		"callback=https%3a%2f%2fstackexchange.com%2fusers%2fauthenticate&" +
		"color=black&" +
		"nonce=81O%2fWwAAAAD1mwDW%2fyXSUw%3d%3d&" +
		"openid.sreg.requested=email&" +
		"signupByDefault=false&" +
		"onLoad=signin-loaded&" +
		"authCode=TrX5Gf3aoRTouzFN9PMsnW23ysM7Emx3zPqwdy%2f28hX14t%2bzrCOpA6DGhbyHKB0c1tIBe9SwJulOYd587r9da%2fgu5EPrMmO117qvygwCje8k%2bCao9cw0laAxXhLx1OMJn%2fTbn7OOvOciTAunlqUhdhD%2b20j5RS%2blwvNGOpW4pWs%3d"; //@formatter:on

	@Test
	public void login_form_missing_enclosing_div() throws Exception {
		StackExchangeSite site = new StackExchangeSite();

		Http http = new Http(new MockHttpClientBuilder() //@formatter:off
			.request("GET", "https://stackexchange.com/users/signin")
			.response(200, exampleLoginFormUrl)
			.request("GET", exampleLoginFormUrl)
			.response(200,
			"<html>" +
				"<form method=\"post\" action=\"/affiliate/form/login/submit\">" +
					"<input type=\"text\" name=\"email\" />" +
					"<input type=\"text\" name=\"password\" />" +
					"<input type=\"hidden\" name=\"fkey\" value=\"17382ce7-0148-45ae-83e5-d5416ad4f12a\" />" +
					"<input type=\"hidden\" name=\"affId\" value=\"11\" />" +
				"</form>" +
			"</html>"
			)
		.build()); //@formatter:on

		try {
			site.login("email@example.com", "password", http);
			fail();
		} catch (IOException e) {
			//expected
		}

		verifyNumberOfRequestsSent(http, 2);
	}

	@Test
	public void login_form_missing_action() throws Exception {
		StackExchangeSite site = new StackExchangeSite();

		Http http = new Http(new MockHttpClientBuilder() //@formatter:off
			.request("GET", "https://stackexchange.com/users/signin")
			.response(200, exampleLoginFormUrl)
			.request("GET", exampleLoginFormUrl)
			.response(200,
			"<html>" +
				"<div class=\"login-form\">" +
					"<form method=\"post\">" +
						"<input type=\"text\" name=\"email\" />" +
						"<input type=\"text\" name=\"password\" />" +
						"<input type=\"hidden\" name=\"fkey\" value=\"17382ce7-0148-45ae-83e5-d5416ad4f12a\" />" +
						"<input type=\"hidden\" name=\"affId\" value=\"11\" />" +
					"</form>" +
				"</div>" +
			"</html>"
			)
		.build()); //@formatter:on

		try {
			site.login("email@example.com", "password", http);
			fail();
		} catch (IOException e) {
			//expected
		}

		verifyNumberOfRequestsSent(http, 2);
	}

	@Test
	public void login_form_missing_fkey() throws Exception {
		StackExchangeSite site = new StackExchangeSite();

		Http http = new Http(new MockHttpClientBuilder() //@formatter:off
			.request("GET", "https://stackexchange.com/users/signin")
			.response(200, exampleLoginFormUrl)
			.request("GET", exampleLoginFormUrl)
			.response(200,
			"<html>" +
				"<div class=\"login-form\">" +
					"<form method=\"post\" action=\"/affiliate/form/login/submit\">" +
						"<input type=\"text\" name=\"email\" />" +
						"<input type=\"text\" name=\"password\" />" +
						"<input type=\"hidden\" name=\"affId\" value=\"11\" />" +
					"</form>" +
				"</div>" +
			"</html>"
			)
		.build()); //@formatter:on

		try {
			site.login("email@example.com", "password", http);
			fail();
		} catch (IOException e) {
			//expected
		}

		verifyNumberOfRequestsSent(http, 2);
	}

	@Test
	public void login_form_missing_affId() throws Exception {
		StackExchangeSite site = new StackExchangeSite();

		Http http = new Http(new MockHttpClientBuilder() //@formatter:off
			.request("GET", "https://stackexchange.com/users/signin")
			.response(200, exampleLoginFormUrl)
			.request("GET", exampleLoginFormUrl)
			.response(200,
			"<html>" +
				"<div class=\"login-form\">" +
					"<form method=\"post\" action=\"/affiliate/form/login/submit\">" +
						"<input type=\"text\" name=\"email\" />" +
						"<input type=\"text\" name=\"password\" />" +
						"<input type=\"hidden\" name=\"fkey\" value=\"17382ce7-0148-45ae-83e5-d5416ad4f12a\" />" +
					"</form>" +
				"</div>" +
			"</html>"
			)
		.build()); //@formatter:on

		try {
			site.login("email@example.com", "password", http);
			fail();
		} catch (IOException e) {
			//expected
		}

		verifyNumberOfRequestsSent(http, 2);
	}

	@Test
	public void login_bad_credentials() throws Exception {
		StackExchangeSite site = new StackExchangeSite();

		Http http = new Http(new MockHttpClientBuilder() //@formatter:off
			.request("GET", "https://stackexchange.com/users/signin")
			.response(200, exampleLoginFormUrl)
			.request("GET", exampleLoginFormUrl)
			.response(200, ResponseSamples.stackExchangeLoginForm())
			.request("POST", "https://openid.stackexchange.com/affiliate/form/login/submit",
				"email", "email@example.com",
				"password", "password",
				"fkey", "17382ce7-0148-45ae-83e5-d5416ad4f12a",
				"affId", "11"
			)
			.response(200, "If the response is missing a JavaScript variable called \"target\", then the credentials are bad.")
		.build()); //@formatter:on

		assertFalse(site.login("email@example.com", "password", http));

		verifyNumberOfRequestsSent(http, 3);
	}

	@Test
	public void login_success() throws Exception {
		StackExchangeSite site = new StackExchangeSite();

		Http http = new Http(new MockHttpClientBuilder() //@formatter:off
			.request("GET", "https://stackexchange.com/users/signin")
			.response(200, exampleLoginFormUrl)
			.request("GET", exampleLoginFormUrl)
			.response(200, ResponseSamples.stackExchangeLoginForm())
			.request("POST", "https://openid.stackexchange.com/affiliate/form/login/submit",
				"email", "email@example.com",
				"password", "password",
				"fkey", "17382ce7-0148-45ae-83e5-d5416ad4f12a",
				"affId", "11"
			)
			.response(200, ResponseSamples.stackExchangeLoginRedirectPage())
			.request("GET", "https://stackexchange.com/users/authenticate?openid_identifier=https%3a%2f%2fopenid.stackexchange.com%2fuser%2fe9c67846-dd5b-485d-8be0-30f65898431f")
			.response(200, "Response body ignored")
		.build()); //@formatter:on

		assertTrue(site.login("email@example.com", "password", http));

		verifyNumberOfRequestsSent(http, 4);
	}

	private static void verifyNumberOfRequestsSent(Http http, int requests) throws IOException {
		verify(http.getClient(), times(requests)).execute(any(HttpUriRequest.class));
	}
}
