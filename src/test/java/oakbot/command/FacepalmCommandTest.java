package oakbot.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
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

		ChatCommandBuilder ccb = new ChatCommandBuilder(command.name());
		ChatResponse response = command.onMessage(ccb.build(1, ""), mock(BotContext.class));

		assertEquals("https://media.tenor.com/images/7e45bbaa8859d5cf8721f78974f480d4/tenor.gif", response.getMessage());
		assertEquals("https://media.tenor.com/images/7e45bbaa8859d5cf8721f78974f480d4/tenor.gif (via [Tenor](https://tenor.com))", response.getCondensedMessage());
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

		ChatCommandBuilder ccb = new ChatCommandBuilder(command.name());
		ChatResponse response = command.onMessage(ccb.build(1, ""), mock(BotContext.class));

		assertEquals(":1 Sorry, an error occurred. >.>", response.getMessage());
		assertNull(response.getCondensedMessage());
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

		ChatCommandBuilder ccb = new ChatCommandBuilder(command.name());
		ChatResponse response = command.onMessage(ccb.build(1, ""), mock(BotContext.class));

		assertEquals(":1 Sorry, an error occurred. >.>", response.getMessage());
		assertNull(response.getCondensedMessage());
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

		ChatCommandBuilder ccb = new ChatCommandBuilder(command.name());
		ChatResponse response = command.onMessage(ccb.build(1, ""), mock(BotContext.class));

		assertEquals(":1 Sorry, an error occurred. >.>", response.getMessage());
		assertNull(response.getCondensedMessage());
	}
}
