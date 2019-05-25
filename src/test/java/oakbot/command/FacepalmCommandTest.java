package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static oakbot.bot.ChatActionsUtils.assertPostMessage;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.PostMessage;
import oakbot.util.ChatCommandBuilder;
import oakbot.util.Gobble;

/**
 * @author Michael Angstadt
 */
public class FacepalmCommandTest {
	@Test
	public void onMessage() {
		FacepalmCommand command = new FacepalmCommand("key") {
			@Override
			String get(URI uri) throws IOException {
				assertEquals(URI.create("https://api.tenor.com/v1/random?key=key&q=facepalm&media_filter=minimal&safesearch=moderate&limit=1"), uri);
				try (InputStream in = getClass().getResourceAsStream("tenor-response.json")) {
					return new Gobble(in).asString();
				}
			}
		};

		ChatCommand message = new ChatCommandBuilder(command).build();
		ChatActions response = command.onMessage(message, mock(BotContext.class));

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
		FacepalmCommand command = new FacepalmCommand("key") {
			@Override
			String get(URI uri) throws IOException {
				assertEquals(URI.create("https://api.tenor.com/v1/random?key=key&q=facepalm&media_filter=minimal&safesearch=moderate&limit=1"), uri);
				throw new IOException();
			}
		};

		ChatCommand message = new ChatCommandBuilder(command).messageId(1).build();
		ChatActions response = command.onMessage(message, mock(BotContext.class));

		assertMessage(":1 Sorry, an error occurred. >.>", response);
	}

	@Test
	public void onMessage_bad_JSON() {
		FacepalmCommand command = new FacepalmCommand("key") {
			@Override
			String get(URI uri) throws IOException {
				assertEquals(URI.create("https://api.tenor.com/v1/random?key=key&q=facepalm&media_filter=minimal&safesearch=moderate&limit=1"), uri);
				return "{}";
			}
		};

		ChatCommand message = new ChatCommandBuilder(command).messageId(1).build();
		ChatActions response = command.onMessage(message, mock(BotContext.class));

		assertMessage(":1 Sorry, an error occurred. >.>", response);
	}

	@Test
	public void onMessage_JSON_parse_error() {
		FacepalmCommand command = new FacepalmCommand("key") {
			@Override
			String get(URI uri) throws IOException {
				assertEquals(URI.create("https://api.tenor.com/v1/random?key=key&q=facepalm&media_filter=minimal&safesearch=moderate&limit=1"), uri);
				return "not JSON";
			}
		};

		ChatCommand message = new ChatCommandBuilder(command).messageId(1).build();
		ChatActions response = command.onMessage(message, mock(BotContext.class));

		assertMessage(":1 Sorry, an error occurred. >.>", response);
	}
}
