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
	public void not_found() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("600");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 Status code not recognized.", response.getMessage());
	}

	@Test
	public void found() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("200");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 [**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc2616#section-10.2.1): The request has succeeded. The information returned with the response is dependent on the method used in the request, for example:", response.getMessage());
	}

	@Test
	public void paragraph_less_than_1() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("200 0");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 [**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc2616#section-10.2.1): The request has succeeded. The information returned with the response is dependent on the method used in the request, for example:", response.getMessage());
	}

	@Test
	public void paragraph_nan() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("200 foo");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 [**HTTP 200 (OK)**](http://tools.ietf.org/html/rfc2616#section-10.2.1): The request has succeeded. The information returned with the response is dependent on the method used in the request, for example:", response.getMessage());
	}

	@Test
	public void paragraph() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("200 2");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 GET an entity corresponding to the requested resource is sent in the response;", response.getMessage());
	}

	@Test
	public void paragraph_over_max() {
		ChatMessage msg = new ChatMessage();
		msg.setMessageId(1);
		msg.setContent("200 100");
		ChatResponse response = command.onMessage(msg, false);

		assertEquals(":1 TRACE an entity containing the request message as received by the end server.", response.getMessage());
	}
}
