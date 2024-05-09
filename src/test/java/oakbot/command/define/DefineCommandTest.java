package oakbot.command.define;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static oakbot.bot.ChatActionsUtils.assertMessageStartsWith;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import oakbot.bot.IBot;
import oakbot.util.ChatCommandBuilder;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;

public class DefineCommandTest {
	@AfterEach
	public void after() {
		HttpFactory.restore();
	}

	@Test
	public void no_word() {
		var command = new DefineCommand("apiKey");
		var message = new ChatCommandBuilder(command).messageId(1).build();

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
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

		var command = new DefineCommand("apiKey");

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
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

		var command = new DefineCommand("apiKey");

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
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

		var command = new DefineCommand("apiKey");

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
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

		var command = new DefineCommand("apiKey");

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("col")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
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

		var command = new DefineCommand("apiKey");

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("col")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
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

		var command = new DefineCommand("apiKey");

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("col")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage(":1 No definitions found. Did you mean cool, cold, or colt?", response);
	}

	@Test
	public void multiple_definitions() throws Exception {
		var cool = new Gobble(getClass(), "cool.xml").asString(StandardCharsets.UTF_8);

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.dictionaryapi.com/api/v1/references/collegiate/xml/cool?key=apiKey")
			.responseOk(cool)
		.build());
		//@formatter:on

		var command = new DefineCommand("apiKey");

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("cool")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("""
			cool (adjective):
			moderately cold; lacking in warmth
			
			cool (adjective):
			marked by steady dispassionate calmness and self-control (a cool and calculating administrator)
			
			cool (adjective):
			lacking ardor or friendliness (a cool impersonal manner)
			
			cool (adjective):
			marked by restrained emotion and the frequent use of counterpoint
			
			cool (adjective):
			free from tensions or violence (we used to fight, but we're cool now)
			
			cool (adjective):
			used as an intensive
			
			cool (adjective):
			marked by deliberate effrontery or lack of due respect or discretion (a cool reply)
			
			cool (adjective):
			facilitating or suggesting relief from heat (a cool dress)
			
			cool (adjective):
			producing an impression of being cool
			
			cool (adjective):
			of a hue in the range violet through blue to green
			
			cool (adjective):
			relatively lacking in timbre or resonance
			
			cool (adjective):
			very good; excellent
			
			cool (adjective):
			all right
			
			cool (adjective):
			fashionable hip (not happy with the new shoes … because they were not cool)
			
			cool (verb):
			to become cool; lose heat or warmth; sometimes used with off or down (placed the pie in the window to cool)
			
			cool (verb):
			to lose ardor or passion (his anger cooled)
			
			cool (verb):
			to make cool; impart a feeling of coolness to; often used with off or down (cooled the room with a fan)
			
			cool (verb):
			to moderate the heat, excitement, or force of; calm (cooled her growing anger)
			
			cool (verb):
			to slow or lessen the growth or activity of; usually used with off or down
			
			cool (noun):
			a cool time, place, or situation (the cool of the evening)
			
			cool (noun):
			absence of excitement or emotional involvement; detachment (must surrender his fine cool and enter the closed crazy world of suicide)
			
			cool (noun):
			poise composure (press questions … seemed to rattle him and he lost his cool)
			
			cool (noun):
			hipness
			
			cool (adverb):
			in a casual and nonchalant manner (play it cool)""", response);
	}

	@Test
	public void encode_parameters() throws Exception {
		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://www.dictionaryapi.com/api/v1/references/collegiate/xml/grand%20piano?key=apiKey")
			.responseOk("<entry_list/>")
		.build());
		//@formatter:on

		var command = new DefineCommand("apiKey");

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("grand piano")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		command.onMessage(message, bot);
	}
}
