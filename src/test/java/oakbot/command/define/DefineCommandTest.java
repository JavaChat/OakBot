package oakbot.command.define;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static oakbot.bot.ChatActionsUtils.assertMessageStartsWith;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Test;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.util.ChatCommandBuilder;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;

public class DefineCommandTest {
	@After
	public void after() {
		HttpFactory.restore();
	}

	@Test
	public void no_word() {
		DefineCommand command = new DefineCommand("apiKey");
		ChatCommand message = new ChatCommandBuilder(command).messageId(1).build();

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 Please specify the word you'd like to define.", response);
	}

	@Test
	public void ioexception() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.dictionaryapi.com/api/v1/references/collegiate/xml/cool?key=apiKey")
			.response(new IOException())
		.build());
		//@formatter:on

		DefineCommand command = new DefineCommand("apiKey");

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
	public void non_xml_response() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.dictionaryapi.com/api/v1/references/collegiate/xml/cool?key=apiKey")
			.responseOk("not XML")
		.build());
		//@formatter:on

		DefineCommand command = new DefineCommand("apiKey");

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
			.requestGet("https://www.dictionaryapi.com/api/v1/references/collegiate/xml/cool?key=apiKey")
			.responseOk("<entry_list/>")
		.build());
		//@formatter:on

		DefineCommand command = new DefineCommand("apiKey");

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 No definitions found.", response);
	}

	@Test
	public void not_found_suggestions_one() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.dictionaryapi.com/api/v1/references/collegiate/xml/col?key=apiKey")
			.responseOk("<entry_list><suggestion>cool</suggestion></entry_list>")
		.build());
		//@formatter:on

		DefineCommand command = new DefineCommand("apiKey");

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("col")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 No definitions found. Did you mean cool?", response);
	}

	@Test
	public void not_found_suggestions_two() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.dictionaryapi.com/api/v1/references/collegiate/xml/col?key=apiKey")
			.responseOk("<entry_list><suggestion>cool</suggestion><suggestion>cold</suggestion></entry_list>")
		.build());
		//@formatter:on

		DefineCommand command = new DefineCommand("apiKey");

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("col")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 No definitions found. Did you mean cool or cold?", response);
	}

	@Test
	public void not_found_suggestions_multiple() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.dictionaryapi.com/api/v1/references/collegiate/xml/col?key=apiKey")
			.responseOk("<entry_list><suggestion>cool</suggestion><suggestion>cold</suggestion><suggestion>colt</suggestion></entry_list>")
		.build());
		//@formatter:on

		DefineCommand command = new DefineCommand("apiKey");

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("col")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 No definitions found. Did you mean cool, cold, or colt?", response);
	}

	@Test
	public void multiple_definitions() throws Exception {
		String cool = new Gobble(getClass(), "cool.xml").asString(StandardCharsets.UTF_8);

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.dictionaryapi.com/api/v1/references/collegiate/xml/cool?key=apiKey")
			.responseOk(cool)
		.build());
		//@formatter:on

		DefineCommand command = new DefineCommand("apiKey");

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		//@formatter:off
		assertMessage(
			"cool (adjective):\n" +
			"moderately cold; lacking in warmth\n" +
			"\n" +
			"cool (adjective):\n" +
			"marked by steady dispassionate calmness and self-control (a cool and calculating administrator)\n" +
			"\n" +
			"cool (adjective):\n" +
			"lacking ardor or friendliness (a cool impersonal manner)\n" +
			"\n" +
			"cool (adjective):\n" +
			"marked by restrained emotion and the frequent use of counterpoint\n" +
			"\n" +
			"cool (adjective):\n" +
			"free from tensions or violence (we used to fight, but we're cool now)\n" +
			"\n" +
			"cool (adjective):\n" +
			"used as an intensive\n" +
			"\n" +
			"cool (adjective):\n" +
			"marked by deliberate effrontery or lack of due respect or discretion (a cool reply)\n" +
			"\n" +
			"cool (adjective):\n" +
			"facilitating or suggesting relief from heat (a cool dress)\n" +
			"\n" +
			"cool (adjective):\n" +
			"producing an impression of being cool\n" +
			"\n" +
			"cool (adjective):\n" +
			"of a hue in the range violet through blue to green\n" +
			"\n" +
			"cool (adjective):\n" +
			"relatively lacking in timbre or resonance\n" +
			"\n" +
			"cool (adjective):\n" +
			"very good; excellent\n" +
			"\n" +
			"cool (adjective):\n" +
			"all right\n" +
			"\n" +
			"cool (adjective):\n" +
			"fashionable hip (not happy with the new shoes … because they were not cool)\n" +
			"\n" +
			"cool (verb):\n" +
			"to become cool; lose heat or warmth; sometimes used with off or down (placed the pie in the window to cool)\n" +
			"\n" +
			"cool (verb):\n" +
			"to lose ardor or passion (his anger cooled)\n" +
			"\n" +
			"cool (verb):\n" +
			"to make cool; impart a feeling of coolness to; often used with off or down (cooled the room with a fan)\n" +
			"\n" +
			"cool (verb):\n" +
			"to moderate the heat, excitement, or force of; calm (cooled her growing anger)\n" +
			"\n" +
			"cool (verb):\n" +
			"to slow or lessen the growth or activity of; usually used with off or down\n" +
			"\n" +
			"cool (noun):\n" +
			"a cool time, place, or situation (the cool of the evening)\n" +
			"\n" +
			"cool (noun):\n" +
			"absence of excitement or emotional involvement; detachment (must surrender his fine cool and enter the closed crazy world of suicide)\n" +
			"\n" +
			"cool (noun):\n" +
			"poise composure (press questions … seemed to rattle him and he lost his cool)\n" +
			"\n" +
			"cool (noun):\n" +
			"hipness\n" +
			"\n" +
			"cool (adverb):\n" +
			"in a casual and nonchalant manner (play it cool)"
			, response
		);
		//@formatter:on
	}

	@Test
	public void encode_parameters() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.dictionaryapi.com/api/v1/references/collegiate/xml/grand%20piano?key=apiKey")
			.responseOk("<entry_list/>")
		.build());
		//@formatter:on

		DefineCommand command = new DefineCommand("apiKey");

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("grand piano")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		command.onMessage(message, bot);
	}
}
