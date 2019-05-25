package oakbot.command.http;

import static oakbot.bot.ChatActionsUtils.assertMessage;

import org.junit.Test;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class HttpCommandTest {
	private final HttpCommand command = new HttpCommand();

	@Test
	public void no_command() {
		ChatCommand message = new ChatCommandBuilder(command).messageId(1).build();

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 Tell me what status code (e.g. 200) or method (e.g. GET) you want to know about.", response);
	}

	@Test
	public void status_code_not_found() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("600")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 Status code not recognized.", response);
	}

	@Test
	public void status_code_found() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("200")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc7231#section-6.3.1): The 200 (OK) status code indicates that the request has succeeded. The payload sent in a 200 response depends on the request method. For the methods defined by this specification, the intended meaning of the payload can be summarized as: (1/4)", response);
	}

	@Test
	public void method_not_found() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("FOO")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 Method not recognized.", response);
	}

	@Test
	public void method_found() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("GET")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**HTTP GET**](http://tools.ietf.org/html/rfc7231#section-4.3.1): The GET method requests transfer of a current selected representation for the target resource.  GET is the primary mechanism of information retrieval and the focus of almost all performance optimizations. Hence, when people speak of retrieving some identifiable information via HTTP, they are generally referring to making a GET request. (1/5)", response);
	}

	@Test
	public void paragraph_less_than_1() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("200 0")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc7231#section-6.3.1): The 200 (OK) status code indicates that the request has succeeded. The payload sent in a 200 response depends on the request method. For the methods defined by this specification, the intended meaning of the payload can be summarized as: (1/4)", response);
	}

	@Test
	public void paragraph_nan() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("200 foo")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc7231#section-6.3.1): The 200 (OK) status code indicates that the request has succeeded. The payload sent in a 200 response depends on the request method. For the methods defined by this specification, the intended meaning of the payload can be summarized as: (1/4)", response);
	}

	@Test
	public void paragraph() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("200 2")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 - GET: a representation of the target resource;\n- HEAD: the same representation as GET, but without the representation data;\n- POST: a representation of the status of, or results obtained from, the action;\n- PUT, DELETE: a representation of the status of the action;\n- OPTIONS: a representation of the communications options;\n- TRACE: a representation of the request message as received by the end server. (2/4)", response);
	}

	@Test
	public void paragraph_over_max() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("200 100")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 A 200 response is cacheable by default; i.e., unless otherwise indicated by the method definition or explicit cache controls (see [Section 4.2.2 of RFC7234](http://tools.ietf.org/html/rfc7234#section-4.2.2))). (4/4)", response);
	}

	@Test
	public void paragraph_just_one() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("306")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**HTTP 306 (Unused)**](http://tools.ietf.org/html/rfc7231#section-6.4.6): The 306 status code was used in a previous version of the specification, is no longer used, and the code is reserved.", response);
	}

	@Test
	public void section_without_rfc() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("100 2")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 When the request contains an Expect header field that includes a 100-continue expectation, the 100 response indicates that the server wishes to receive the request payload body, as described in [Section 5.1.1](http://tools.ietf.org/html/rfc7231#section-5.1.1).  The client ought to continue sending the request and discard the 100 response. (2/3)", response);
	}

	@Test
	public void section_without_rfc_with_statusCode_specific_rfc() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("401")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**HTTP 401 (Unauthorized)**](http://tools.ietf.org/html/rfc7235#section-3.1): The 401 (Unauthorized) status code indicates that the request has not been applied because it lacks valid authentication credentials for the target resource.  The server generating a 401 response MUST send a WWW-Authenticate header field ([Section 4.1](http://tools.ietf.org/html/rfc7235#section-4.1)) containing at least one challenge applicable to the target resource. (1/2)", response);
	}

	@Test
	public void section_with_rfc() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("101")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**HTTP 101 (Switching Protocols)**](http://tools.ietf.org/html/rfc7231#section-6.2.2): The 101 (Switching Protocols) status code indicates that the server understands and is willing to comply with the client's request, via the Upgrade header field ([Section 6.7 of RFC7230](http://tools.ietf.org/html/rfc7230#section-6.7)), for a change in the application protocol being used on this connection.  The server MUST generate an Upgrade header field in the response that indicates which protocol(s) will be switched to immediately after the empty line that terminates the 101 response. (1/2)", response);
	}

	@Test
	public void rfc() {
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("300 5")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 Note: The original proposal for the 300 status code defined the URI header field as providing a list of alternative representations, such that it would be usable for 200, 300, and 406 responses and be transferred in responses to the HEAD method. However, lack of deployment and disagreement over syntax led to both URI and Alternates (a subsequent proposal) being dropped from this specification.  It is possible to communicate the list using a set of Link header fields ([RFC5988](http://tools.ietf.org/html/rfc5988)), each with a relationship of \"alternate\", though deployment is a chicken-and-egg problem. (5/5)", response);
	}
}
