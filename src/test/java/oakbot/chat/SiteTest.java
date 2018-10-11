package oakbot.chat;

import static org.junit.Assert.assertEquals;
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
public class SiteTest {
	@Test
	public void constructor() throws Exception {
		Site site = new Site("domain.com");
		assertEquals("domain.com", site.getDomain());
		assertEquals("chat.domain.com", site.getChatDomain());
	}

	@Test
	public void login_no_fkey() throws Exception {
		Site site = new Site("domain.com");

		Http http = new Http(new MockHttpClientBuilder() //@formatter:off
			.request("GET", "https://domain.com/users/login")
			.response(200, "garbage data")
		.build()); //@formatter:on

		try {
			site.login("email@example.com", "password", http);
			fail();
		} catch (IOException e) {
			//expected
		}

		verifyNumberOfRequestsSent(http, 1);
	}

	@Test
	public void login_bad_credentials() throws Exception {
		Site site = new Site("domain.com");

		Http http = new Http(new MockHttpClientBuilder() //@formatter:off
			.login("domain.com", "0123456789abcdef0123456789abcdef", "email@example.com", "password", false)
		.build()); //@formatter:on

		assertFalse(site.login("email@example.com", "password", http));

		verifyNumberOfRequestsSent(http, 2);
	}

	@Test
	public void login() throws Exception {
		Site site = new Site("domain.com");

		Http http = new Http(new MockHttpClientBuilder() //@formatter:off
			.login("domain.com", "0123456789abcdef0123456789abcdef", "email@example.com", "password", true)
		.build()); //@formatter:on

		assertTrue(site.login("email@example.com", "password", http));

		verifyNumberOfRequestsSent(http, 2);
	}

	private static void verifyNumberOfRequestsSent(Http http, int requests) throws IOException {
		verify(http.getClient(), times(requests)).execute(any(HttpUriRequest.class));
	}
}
