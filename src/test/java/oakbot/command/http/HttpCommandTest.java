package oakbot.command.http;

import static org.junit.Assert.assertEquals;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class HttpCommandTest {
	private final HttpCommand command = new HttpCommand();

	@Test
	public void no_command() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 Tell me what status code (e.g. 200) or method (e.g. GET) you want to know about.", response.getMessage());
	}

	@Test
	public void status_code_not_found() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("600");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 Status code not recognized.", response.getMessage());
	}

	@Test
	public void status_code_found() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("200");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 [**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc2616#section-10.2.1): The request has succeeded. The information returned with the response is dependent on the method used in the request, for example: (1/2)", response.getMessage());
	}

	@Test
	public void method_not_found() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("FOO");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 Method not recognized.", response.getMessage());
	}

	@Test
	public void method_found() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("GET");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 [**HTTP GET**](http://tools.ietf.org/html/rfc2616#section-9.3): The GET method means retrieve whatever information (in the form of an entity) is identified by the Request-URI. If the Request-URI refers to a data-producing process, it is the produced data which shall be returned as the entity in the response and not the source text of the process, unless that text happens to be the output of the process. (1/5)", response.getMessage());
	}

	@Test
	public void paragraph_less_than_1() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("200 0");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 [**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc2616#section-10.2.1): The request has succeeded. The information returned with the response is dependent on the method used in the request, for example: (1/2)", response.getMessage());
	}

	@Test
	public void paragraph_nan() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("200 foo");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 [**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc2616#section-10.2.1): The request has succeeded. The information returned with the response is dependent on the method used in the request, for example: (1/2)", response.getMessage());
	}

	@Test
	public void paragraph() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("200 2");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 - GET an entity corresponding to the requested resource is sent in the response;\n- HEAD the entity-header fields corresponding to the requested resource are sent in the response without any message-body;\n- POST an entity describing or containing the result of the action;\n- TRACE an entity containing the request message as received by the end server. (2/2)", response.getMessage());
	}

	@Test
	public void paragraph_over_max() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("200 100");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 - GET an entity corresponding to the requested resource is sent in the response;\n- HEAD the entity-header fields corresponding to the requested resource are sent in the response without any message-body;\n- POST an entity describing or containing the result of the action;\n- TRACE an entity containing the request message as received by the end server. (2/2)", response.getMessage());
	}

	@Test
	public void paragraph_just_one() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("306");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 [**HTTP 306 (Unused)**](http://tools.ietf.org/html/rfc2616#section-10.3.7): The 306 status code was used in a previous version of the specification, is no longer used, and the code is reserved.", response.getMessage());
	}
}
