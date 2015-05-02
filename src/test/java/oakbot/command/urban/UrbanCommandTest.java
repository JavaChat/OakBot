package oakbot.command.urban;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.LogManager;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.CharStreams;

/**
 * @author Michael Angstadt
 */
public class UrbanCommandTest {
	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	@Test
	public void no_word() {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("");

		UrbanCommand urban = new UrbanCommand();
		ChatResponse response = urban.onMessage(message, false);
		assertEquals(":1 You have to type a word to see its definition... -_-", response.getMessage());
	}

	@Test
	public void ioexception() throws Exception {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("cool");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			String get(String url) throws IOException {
				throw new IOException();
			}
		};

		ChatResponse response = urban.onMessage(message, false);
		assertTrue(response.getMessage().startsWith(":1 Sorry"));
	}

	@Test
	public void non_json_response() throws Exception {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("cool");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			String get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return "<html>not JSON</html>";
			}
		};
		ChatResponse response = urban.onMessage(message, false);
		assertTrue(response.getMessage().startsWith(":1 Sorry"));
	}

	@Test
	public void not_found() throws Exception {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("cool");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			String get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return "{}";
			}
		};
		ChatResponse response = urban.onMessage(message, false);
		assertEquals(":1 No definition found.", response.getMessage());
	}

	@Test
	public void no_newlines_in_definition() throws Exception {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("cool");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			String get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("urbandictionary.cool.json"))) {
					return CharStreams.toString(reader);
				}
			}
		};
		ChatResponse response = urban.onMessage(message, false);
		assertEquals(":1 [**`cool`**](http://cool.urbanup.com/120269): The best way to say something is neat-o, [awesome](http://www.urbandictionary.com/define.php?term=awesome), or swell. The phrase \"cool\" is very relaxed, never goes out of style, and people will never laugh at you for using it, very conveniant for people like me who don't care about what's \"in.\"", response.getMessage());
	}

	@Test
	public void newlines_in_definition() throws Exception {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("snafu");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			String get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=snafu", url);
				try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("urbandictionary.snafu.json"))) {
					return CharStreams.toString(reader);
				}
			}
		};
		ChatResponse response = urban.onMessage(message, false);
		//@formatter:off
		assertEquals(
			":1 SNAFU (http://snafu.urbanup.com/449743):\n" +
			"One of a progression of military situational indicators:\r\n" +
			"\r\n" +
			"1. SNAFU - Situation Normal, All Fucked Up - Thing are running normally.\r\n" +
			"2. TARFUN - Things Are Really Fucked Up Now - Houston, we have a problem.\r\n" +
			"3. FUBAR - Fucked Up Beyond All Recognition - Burn it to the ground and start over from scratch; it's totally destroyed."
		, response.getMessage());
		//@formatter:on
	}

	@Test
	public void encode_parameters() throws Exception {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("fucked up");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			String get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=fucked+up", url);
				return "";
			}
		};
		urban.onMessage(message, false);
	}

	@Test
	public void other_definition() throws Exception {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("cool 2");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			String get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("urbandictionary.cool.json"))) {
					return CharStreams.toString(reader);
				}
			}
		};
		ChatResponse response = urban.onMessage(message, false);
		assertEquals(":1 [**`cool`**](http://cool.urbanup.com/1030338): A word to use when you don't know what else to say, or when you are not that interested in the conversation. Sometimes, it can be used when you do not have any knowledge of the subject, yet you want to act as if you know-it-all.", response.getMessage());
	}

	@Test
	public void other_definition_range_low() throws Exception {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("cool -1");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			String get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("urbandictionary.cool.json"))) {
					return CharStreams.toString(reader);
				}
			}
		};
		ChatResponse response = urban.onMessage(message, false);
		assertEquals(":1 [**`cool`**](http://cool.urbanup.com/120269): The best way to say something is neat-o, [awesome](http://www.urbandictionary.com/define.php?term=awesome), or swell. The phrase \"cool\" is very relaxed, never goes out of style, and people will never laugh at you for using it, very conveniant for people like me who don't care about what's \"in.\"", response.getMessage());
	}

	@Test
	public void other_definition_range_high() throws Exception {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("cool 9000");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			String get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("urbandictionary.cool.json"))) {
					return CharStreams.toString(reader);
				}
			}
		};
		ChatResponse response = urban.onMessage(message, false);
		assertEquals(":1 [**`cool`**](http://cool.urbanup.com/1096252): a simplified way of telling someone to shut the fuck up because you don't give a shit.", response.getMessage());
	}

	@Test
	public void word_with_spaces() throws Exception {
		ChatMessage message = new ChatMessage();
		message.setMessageId(1);
		message.setContent("fucked up");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			String get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=fucked+up", url);
				return "{\"list\":[{\"word\":\"fucked up\", \"definition\":\"Definition\", \"permalink\":\"Permalink\"}]}";
			}
		};
		ChatResponse response = urban.onMessage(message, false);
		assertEquals(":1 [**`fucked up`**](Permalink): Definition", response.getMessage());
	}
}
