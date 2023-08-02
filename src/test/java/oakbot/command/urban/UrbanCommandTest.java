package oakbot.command.urban;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static oakbot.bot.ChatActionsUtils.assertMessageStartsWith;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.After;
import org.junit.Test;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.util.ChatCommandBuilder;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;

/**
 * @author Michael Angstadt
 */
public class UrbanCommandTest {
	@After
	public void after() {
		HttpFactory.restore();
	}

	@Test
	public void no_word() {
		UrbanCommand command = new UrbanCommand();

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 You have to type a word to see its definition... -_-", response);
	}

	@Test
	public void ioexception() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.response(new IOException())
		.build());
		//@formatter:on

		UrbanCommand command = new UrbanCommand();

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessageStartsWith(":1 Sorry", response);
	}

	@Test
	public void non_json_response() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk("<html>not JSON</html>")
		.build());
		//@formatter:on

		UrbanCommand command = new UrbanCommand();

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessageStartsWith(":1 Sorry", response);
	}

	@Test
	public void not_found() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk("{}")
		.build());
		//@formatter:on

		UrbanCommand command = new UrbanCommand();

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 No definition found.", response);
	}

	@Test
	public void no_newlines_in_definition() throws Exception {
		String cool = new Gobble(getClass(), "urbandictionary.cool.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk(cool)
		.build());
		//@formatter:on

		UrbanCommand command = new UrbanCommand();

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 [**`cool`**](http://cool.urbanup.com/120269): The best way to say something is neat-o, [awesome](http://www.urbandictionary.com/define.php?term=awesome), or swell. The phrase \"cool\" is very relaxed, never goes out of style, and people will never laugh at you for using it, very conveniant for people like me who don't care about what's \"in.\"", response);
	}

	@Test
	public void newlines_in_definition() throws Exception {
		String snafu = new Gobble(getClass(), "urbandictionary.snafu.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=snafu")
			.responseOk(snafu)
		.build());
		//@formatter:on

		UrbanCommand command = new UrbanCommand();

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("snafu")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);

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
	public void other_definition() throws Exception {
		String cool = new Gobble(getClass(), "urbandictionary.cool.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk(cool)
		.build());
		//@formatter:on

		UrbanCommand command = new UrbanCommand();

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool 2")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 [**`cool`**](http://cool.urbanup.com/1030338): A word to use when you don't know what else to say, or when you are not that interested in the conversation. Sometimes, it can be used when you do not have any knowledge of the subject, yet you want to act as if you know-it-all.", response);
	}

	@Test
	public void other_definition_range_low() throws Exception {
		String cool = new Gobble(getClass(), "urbandictionary.cool.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk(cool)
		.build());
		//@formatter:on

		UrbanCommand command = new UrbanCommand();

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool -1")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 [**`cool`**](http://cool.urbanup.com/120269): The best way to say something is neat-o, [awesome](http://www.urbandictionary.com/define.php?term=awesome), or swell. The phrase \"cool\" is very relaxed, never goes out of style, and people will never laugh at you for using it, very conveniant for people like me who don't care about what's \"in.\"", response);
	}

	@Test
	public void other_definition_range_high() throws Exception {
		String cool = new Gobble(getClass(), "urbandictionary.cool.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk(cool)
		.build());
		//@formatter:on

		UrbanCommand command = new UrbanCommand();

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool 9000")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 [**`cool`**](http://cool.urbanup.com/1096252): a simplified way of telling someone to shut the fuck up because you don't give a shit.", response);
	}

	@Test
	public void word_with_spaces() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=fucked+up")
			.responseOk("{\"list\":[{\"word\":\"fucked up\", \"definition\":\"Definition\", \"permalink\":\"Permalink\"}]}")
		.build());
		//@formatter:on

		UrbanCommand command = new UrbanCommand();

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("fucked up")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 [**`fucked up`**](Permalink): Definition", response);
	}
}
