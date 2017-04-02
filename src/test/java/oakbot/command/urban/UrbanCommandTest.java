package oakbot.command.urban;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Test;

import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class UrbanCommandTest {
	private final ChatCommandBuilder chatCommandBuilder = new ChatCommandBuilder(new UrbanCommand().name());

	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	@Test
	public void no_word() {
		ChatCommand message = chatCommandBuilder.build(1, "");

		UrbanCommand urban = new UrbanCommand();
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 You have to type a word to see its definition... -_-", response.getMessage());
	}

	@Test
	public void ioexception() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				throw new IOException();
			}
		};

		ChatResponse response = urban.onMessage(message, null);
		assertTrue(response.getMessage().startsWith(":1 Sorry"));
	}

	@Test
	public void non_json_response() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return in("<html>not JSON</html>");
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertTrue(response.getMessage().startsWith(":1 Sorry"));
	}

	@Test
	public void not_found() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return in("{}");
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 No definition found.", response.getMessage());
	}

	@Test
	public void no_newlines_in_definition() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return getClass().getResourceAsStream("urbandictionary.cool.json");
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 [**`cool`**](http://cool.urbanup.com/120269): The best way to say something is neat-o, [awesome](http://www.urbandictionary.com/define.php?term=awesome), or swell. The phrase \"cool\" is very relaxed, never goes out of style, and people will never laugh at you for using it, very conveniant for people like me who don't care about what's \"in.\"", response.getMessage());
	}

	@Test
	public void newlines_in_definition() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "snafu");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=snafu", url);
				return getClass().getResourceAsStream("urbandictionary.snafu.json");
			}
		};
		ChatResponse response = urban.onMessage(message, null);
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
		ChatCommand message = chatCommandBuilder.build(1, "fucked up");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=fucked+up", url);
				return in("");
			}
		};
		urban.onMessage(message, null);
	}

	@Test
	public void other_definition() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool 2");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return getClass().getResourceAsStream("urbandictionary.cool.json");
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 [**`cool`**](http://cool.urbanup.com/1030338): A word to use when you don't know what else to say, or when you are not that interested in the conversation. Sometimes, it can be used when you do not have any knowledge of the subject, yet you want to act as if you know-it-all.", response.getMessage());
	}

	@Test
	public void other_definition_range_low() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool -1");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return getClass().getResourceAsStream("urbandictionary.cool.json");
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 [**`cool`**](http://cool.urbanup.com/120269): The best way to say something is neat-o, [awesome](http://www.urbandictionary.com/define.php?term=awesome), or swell. The phrase \"cool\" is very relaxed, never goes out of style, and people will never laugh at you for using it, very conveniant for people like me who don't care about what's \"in.\"", response.getMessage());
	}

	@Test
	public void other_definition_range_high() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool 9000");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return getClass().getResourceAsStream("urbandictionary.cool.json");
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 [**`cool`**](http://cool.urbanup.com/1096252): a simplified way of telling someone to shut the fuck up because you don't give a shit.", response.getMessage());
	}

	@Test
	public void word_with_spaces() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "fucked up");

		UrbanCommand urban = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=fucked+up", url);
				return in("{\"list\":[{\"word\":\"fucked up\", \"definition\":\"Definition\", \"permalink\":\"Permalink\"}]}");
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 [**`fucked up`**](Permalink): Definition", response.getMessage());
	}

	private static InputStream in(String string) {
		return new ByteArrayInputStream(string.getBytes());
	}
}
