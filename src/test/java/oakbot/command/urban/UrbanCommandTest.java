package oakbot.command.urban;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static oakbot.bot.ChatActionsUtils.assertMessageStartsWith;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import oakbot.bot.IBot;
import oakbot.util.ChatCommandBuilder;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;

/**
 * @author Michael Angstadt
 */
class UrbanCommandTest {
	@AfterEach
	void after() {
		HttpFactory.restore();
	}

	@Test
	void no_word() {
		var command = new UrbanCommand();

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("You have to type a word to see its definition... -_-", 1, response);
	}

	@Test
	void ioexception() {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.response(new IOException())
		.build());
		//@formatter:on

		var command = new UrbanCommand();

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessageStartsWith("Sorry", 1, response);
	}

	@Test
	void non_json_response() {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk("<html>not JSON</html>")
		.build());
		//@formatter:on

		var command = new UrbanCommand();

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessageStartsWith("Sorry", 1, response);
	}

	@Test
	void not_found() {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk("{}")
		.build());
		//@formatter:on

		var command = new UrbanCommand();

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("No definition found.", 1, response);
	}

	@Test
	void no_newlines_in_definition() throws Exception {
		var cool = new Gobble(getClass(), "urbandictionary.cool.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk(cool)
		.build());
		//@formatter:on

		var command = new UrbanCommand();

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("[**`cool`**](http://cool.urbanup.com/120269): The best way to say something is neat-o, [awesome](http://www.urbandictionary.com/define.php?term=awesome), or swell. The phrase \"cool\" is very relaxed, never goes out of style, and people will never laugh at you for using it, very conveniant for people like me who don't care about what's \"in.\"", 1, response);
	}

	@Test
	void newlines_in_definition() throws Exception {
		var snafu = new Gobble(getClass(), "urbandictionary.snafu.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=snafu")
			.responseOk(snafu)
		.build());
		//@formatter:on

		var command = new UrbanCommand();

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("snafu")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);

		//@formatter:off
		assertMessage("""
		SNAFU (http://snafu.urbanup.com/449743):
		One of a progression of military situational indicators:\r
		\r
		1. SNAFU - Situation Normal, All Fucked Up - Thing are running normally.\r
		2. TARFUN - Things Are Really Fucked Up Now - Houston, we have a problem.\r
		3. FUBAR - Fucked Up Beyond All Recognition - Burn it to the ground and start over from scratch; it's totally destroyed."""
		, 1, response);
		//@formatter:on
	}

	@Test
	void other_definition() throws Exception {
		var cool = new Gobble(getClass(), "urbandictionary.cool.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk(cool)
		.build());
		//@formatter:on

		var command = new UrbanCommand();

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool 2")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("[**`cool`**](http://cool.urbanup.com/1030338): A word to use when you don't know what else to say, or when you are not that interested in the conversation. Sometimes, it can be used when you do not have any knowledge of the subject, yet you want to act as if you know-it-all.", 1, response);
	}

	@Test
	void other_definition_range_low() throws Exception {
		var cool = new Gobble(getClass(), "urbandictionary.cool.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk(cool)
		.build());
		//@formatter:on

		var command = new UrbanCommand();

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool -1")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("[**`cool`**](http://cool.urbanup.com/120269): The best way to say something is neat-o, [awesome](http://www.urbandictionary.com/define.php?term=awesome), or swell. The phrase \"cool\" is very relaxed, never goes out of style, and people will never laugh at you for using it, very conveniant for people like me who don't care about what's \"in.\"", 1, response);
	}

	@Test
	void other_definition_range_high() throws Exception {
		var cool = new Gobble(getClass(), "urbandictionary.cool.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=cool")
			.responseOk(cool)
		.build());
		//@formatter:on

		var command = new UrbanCommand();

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool 9000")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("[**`cool`**](http://cool.urbanup.com/1096252): a simplified way of telling someone to shut the fuck up because you don't give a shit.", 1, response);
	}

	@Test
	void word_with_spaces() {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://api.urbandictionary.com/v0/define?term=fucked+up")
			.responseOk("{\"list\":[{\"word\":\"fucked up\", \"definition\":\"Definition\", \"permalink\":\"Permalink\"}]}")
		.build());
		//@formatter:on

		var command = new UrbanCommand();

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("fucked up")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("[**`fucked up`**](Permalink): Definition", 1, response);
	}
}
