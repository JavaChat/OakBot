package oakbot.command.http;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.util.Leaf;

import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
class HttpCommandTest {
	private final HttpCommand command = new HttpCommand();

	@Test
	void xml() throws Exception {
		Leaf document;
		try (var in = getClass().getResourceAsStream("http.xml")) {
			document = Leaf.parse(in);
		}

		var http = document.selectFirst("/http");
		assertFalse(http.attribute("rfc").isEmpty());

		var statusCodes = document.select("/http/statusCode");
		assertFalse(statusCodes.isEmpty());

		for (var leaf : statusCodes) {
			assertFalse(leaf.attribute("code").isEmpty());
			assertFalse(leaf.attribute("name").isEmpty());
			assertFalse(leaf.attribute("section").isEmpty());
		}

		var methods = document.select("/http/method");
		assertFalse(methods.isEmpty());

		for (var leaf : methods) {
			assertFalse(leaf.attribute("name").isEmpty());
			assertFalse(leaf.attribute("section").isEmpty());
		}
	}

	@Test
	void no_command() {
		var message = new ChatCommandBuilder(command).messageId(1).build();

		var response = command.onMessage(message, null);
		assertMessage("Tell me what status code (e.g. 200) or method (e.g. GET) you want to know about.", 1, response);
	}

	@Test
	void status_code_not_found() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("600")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("Status code not recognized.", 1, response);
	}

	@Test
	void status_code_found() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("200")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("[**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc7231#section-6.3.1): The 200 (OK) status code indicates that the request has succeeded. The payload sent in a 200 response depends on the request method. For the methods defined by this specification, the intended meaning of the payload can be summarized as: (1/4)", 1, response);
	}

	@Test
	void method_not_found() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("FOO")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("Method not recognized.", 1, response);
	}

	@Test
	void method_found() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("GET")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("[**HTTP GET**](http://tools.ietf.org/html/rfc7231#section-4.3.1): The GET method requests transfer of a current selected representation for the target resource.  GET is the primary mechanism of information retrieval and the focus of almost all performance optimizations. Hence, when people speak of retrieving some identifiable information via HTTP, they are generally referring to making a GET request. (1/5)", 1, response);
	}

	@Test
	void paragraph_less_than_1() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("200 0")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("[**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc7231#section-6.3.1): The 200 (OK) status code indicates that the request has succeeded. The payload sent in a 200 response depends on the request method. For the methods defined by this specification, the intended meaning of the payload can be summarized as: (1/4)", 1, response);
	}

	@Test
	void paragraph_nan() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("200 foo")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("[**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc7231#section-6.3.1): The 200 (OK) status code indicates that the request has succeeded. The payload sent in a 200 response depends on the request method. For the methods defined by this specification, the intended meaning of the payload can be summarized as: (1/4)", 1, response);
	}

	@Test
	void paragraph() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("200 2")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("- GET: a representation of the target resource;\n- HEAD: the same representation as GET, but without the representation data;\n- POST: a representation of the status of, or results obtained from, the action;\n- PUT, DELETE: a representation of the status of the action;\n- OPTIONS: a representation of the communications options;\n- TRACE: a representation of the request message as received by the end server. (2/4)", 1, response);
	}

	@Test
	void paragraph_over_max() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("200 100")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("A 200 response is cacheable by default; i.e., unless otherwise indicated by the method definition or explicit cache controls (see [Section 4.2.2 of RFC7234](http://tools.ietf.org/html/rfc7234#section-4.2.2))). (4/4)", 1, response);
	}

	@Test
	void paragraph_just_one() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("306")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("[**HTTP 306 (Unused)**](http://tools.ietf.org/html/rfc7231#section-6.4.6): The 306 status code was used in a previous version of the specification, is no longer used, and the code is reserved.", 1, response);
	}

	@Test
	void section_without_rfc() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("100 2")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("When the request contains an Expect header field that includes a 100-continue expectation, the 100 response indicates that the server wishes to receive the request payload body, as described in [Section 5.1.1](http://tools.ietf.org/html/rfc7231#section-5.1.1).  The client ought to continue sending the request and discard the 100 response. (2/3)", 1, response);
	}

	@Test
	void section_without_rfc_with_statusCode_specific_rfc() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("401")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("[**HTTP 401 (Unauthorized)**](http://tools.ietf.org/html/rfc7235#section-3.1): The 401 (Unauthorized) status code indicates that the request has not been applied because it lacks valid authentication credentials for the target resource.  The server generating a 401 response MUST send a WWW-Authenticate header field ([Section 4.1](http://tools.ietf.org/html/rfc7235#section-4.1)) containing at least one challenge applicable to the target resource. (1/2)", 1, response);
	}

	@Test
	void section_with_rfc() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("101")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("[**HTTP 101 (Switching Protocols)**](http://tools.ietf.org/html/rfc7231#section-6.2.2): The 101 (Switching Protocols) status code indicates that the server understands and is willing to comply with the client's request, via the Upgrade header field ([Section 6.7 of RFC7230](http://tools.ietf.org/html/rfc7230#section-6.7)), for a change in the application protocol being used on this connection.  The server MUST generate an Upgrade header field in the response that indicates which protocol(s) will be switched to immediately after the empty line that terminates the 101 response. (1/2)", 1, response);
	}

	@Test
	void rfc() {
		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("300 5")
		.build();
		//@formatter:on

		var response = command.onMessage(message, null);
		assertMessage("Note: The original proposal for the 300 status code defined the URI header field as providing a list of alternative representations, such that it would be usable for 200, 300, and 406 responses and be transferred in responses to the HEAD method. However, lack of deployment and disagreement over syntax led to both URI and Alternates (a subsequent proposal) being dropped from this specification.  It is possible to communicate the list using a set of Link header fields ([RFC5988](http://tools.ietf.org/html/rfc5988)), each with a relationship of \"alternate\", though deployment is a chicken-and-egg problem. (5/5)", 1, response);
	}
}
