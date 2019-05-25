package oakbot.command.urban;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static oakbot.bot.ChatActionsUtils.assertMessageStartsWith;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class UrbanCommandTest {
	@Test
	public void no_word() {
		UrbanCommand command = new UrbanCommand();
		ChatCommand message = new ChatCommandBuilder(command).messageId(1).build();

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 You have to type a word to see its definition... -_-", response);
	}

	@Test
	public void ioexception() throws Exception {
		UrbanCommand command = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				throw new IOException();
			}
		};
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("cool")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessageStartsWith(":1 Sorry", response);
	}

	@Test
	public void non_json_response() throws Exception {
		UrbanCommand command = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return in("<html>not JSON</html>");
			}
		};
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("cool")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessageStartsWith(":1 Sorry", response);
	}

	@Test
	public void not_found() throws Exception {
		UrbanCommand command = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return in("{}");
			}
		};
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("cool")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 No definition found.", response);
	}

	@Test
	public void no_newlines_in_definition() throws Exception {
		UrbanCommand command = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return getClass().getResourceAsStream("urbandictionary.cool.json");
			}
		};
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("cool")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**`cool`**](http://cool.urbanup.com/120269): The best way to say something is neat-o, [awesome](http://www.urbandictionary.com/define.php?term=awesome), or swell. The phrase \"cool\" is very relaxed, never goes out of style, and people will never laugh at you for using it, very conveniant for people like me who don't care about what's \"in.\"", response);
	}

	@Test
	public void newlines_in_definition() throws Exception {
		UrbanCommand command = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=snafu", url);
				return getClass().getResourceAsStream("urbandictionary.snafu.json");
			}
		};
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("snafu")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		//@formatter:off
		assertMessage(
			":1 SNAFU (http://snafu.urbanup.com/449743):\n" +
			"One of a progression of military situational indicators:\r\n" +
			"\r\n" +
			"1. SNAFU - Situation Normal, All Fucked Up - Thing are running normally.\r\n" +
			"2. TARFUN - Things Are Really Fucked Up Now - Houston, we have a problem.\r\n" +
			"3. FUBAR - Fucked Up Beyond All Recognition - Burn it to the ground and start over from scratch; it's totally destroyed."
		, response);
		//@formatter:on
	}

	@Test
	public void encode_parameters() throws Exception {
		UrbanCommand command = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=fucked+up", url);
				return in("");
			}
		};
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("fucked up")
		.build(); //@formatter:on

		command.onMessage(message, null);
	}

	@Test
	public void other_definition() throws Exception {
		UrbanCommand command = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return getClass().getResourceAsStream("urbandictionary.cool.json");
			}
		};
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("cool 2")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**`cool`**](http://cool.urbanup.com/1030338): A word to use when you don't know what else to say, or when you are not that interested in the conversation. Sometimes, it can be used when you do not have any knowledge of the subject, yet you want to act as if you know-it-all.", response);
	}

	@Test
	public void other_definition_range_low() throws Exception {
		UrbanCommand command = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return getClass().getResourceAsStream("urbandictionary.cool.json");
			}
		};
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("cool -1")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**`cool`**](http://cool.urbanup.com/120269): The best way to say something is neat-o, [awesome](http://www.urbandictionary.com/define.php?term=awesome), or swell. The phrase \"cool\" is very relaxed, never goes out of style, and people will never laugh at you for using it, very conveniant for people like me who don't care about what's \"in.\"", response);
	}

	@Test
	public void other_definition_range_high() throws Exception {
		UrbanCommand command = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=cool", url);
				return getClass().getResourceAsStream("urbandictionary.cool.json");
			}
		};
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("cool 9000")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**`cool`**](http://cool.urbanup.com/1096252): a simplified way of telling someone to shut the fuck up because you don't give a shit.", response);
	}

	@Test
	public void word_with_spaces() throws Exception {
		UrbanCommand command = new UrbanCommand() {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://api.urbandictionary.com/v0/define?term=fucked+up", url);
				return in("{\"list\":[{\"word\":\"fucked up\", \"definition\":\"Definition\", \"permalink\":\"Permalink\"}]}");
			}
		};
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("fucked up")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, null);
		assertMessage(":1 [**`fucked up`**](Permalink): Definition", response);
	}

	private static InputStream in(String string) {
		return new ByteArrayInputStream(string.getBytes());
	}
}
