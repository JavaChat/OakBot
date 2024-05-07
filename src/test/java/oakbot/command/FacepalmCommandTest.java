package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static oakbot.bot.ChatActionsUtils.assertPostMessage;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.After;
import org.junit.Test;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatCommandBuilder;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;

/**
 * @author Michael Angstadt
 */
public class FacepalmCommandTest {
	@After
	public void after() {
		HttpFactory.restore();
	}

	@Test
	public void onMessage() throws Exception {
		var tenor = new Gobble(getClass(), "tenor-response.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://api.tenor.com/v1/random?key=key&q=facepalm&media_filter=minimal&safesearch=moderate&limit=1")
			.responseOk(tenor)
		.build());
		//@formatter:on

		var command = new FacepalmCommand("key");

		var message = new ChatCommandBuilder(command).build();
		var response = command.onMessage(message, mock(IBot.class));

		//@formatter:off
		assertPostMessage(
			new PostMessage("https://media.tenor.com/images/7e45bbaa8859d5cf8721f78974f480d4/tenor.gif")
				.condensedMessage("https://media.tenor.com/images/7e45bbaa8859d5cf8721f78974f480d4/tenor.gif (via [Tenor](https://tenor.com))")
				.bypassFilters(true),
			response
		);
		//@formatter:on
	}

	@Test
	public void onMessage_exception() {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://api.tenor.com/v1/random?key=key&q=facepalm&media_filter=minimal&safesearch=moderate&limit=1")
			.response(new IOException())
		.build());
		//@formatter:on

		var command = new FacepalmCommand("key");

		var message = new ChatCommandBuilder(command).messageId(1).build();
		var response = command.onMessage(message, mock(IBot.class));

		assertMessage(":1 Sorry, an error occurred. >.>", response);
	}

	@Test
	public void onMessage_bad_JSON() {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://api.tenor.com/v1/random?key=key&q=facepalm&media_filter=minimal&safesearch=moderate&limit=1")
			.responseOk("{}")
		.build());
		//@formatter:on

		var command = new FacepalmCommand("key");

		var message = new ChatCommandBuilder(command).messageId(1).build();
		var response = command.onMessage(message, mock(IBot.class));

		assertMessage(":1 Sorry, an error occurred. >.>", response);
	}

	@Test
	public void onMessage_JSON_parse_error() {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://api.tenor.com/v1/random?key=key&q=facepalm&media_filter=minimal&safesearch=moderate&limit=1")
			.responseOk("not JSON")
		.build());
		//@formatter:on

		var command = new FacepalmCommand("key");

		var message = new ChatCommandBuilder(command).messageId(1).build();
		var response = command.onMessage(message, mock(IBot.class));

		assertMessage(":1 Sorry, an error occurred. >.>", response);
	}
}
