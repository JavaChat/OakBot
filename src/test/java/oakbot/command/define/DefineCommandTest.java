package oakbot.command.define;

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

public class DefineCommandTest {
	private final ChatCommandBuilder chatCommandBuilder = new ChatCommandBuilder(new DefineCommand("").name());

	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	@Test
	public void no_word() {
		ChatCommand message = chatCommandBuilder.build(1, "");
		DefineCommand urban = new DefineCommand("theKey");
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 Please specify the word you'd like to define.", response.getMessage());
	}

	@Test
	public void ioexception() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool");
		DefineCommand urban = new DefineCommand("theKey") {
			@Override
			InputStream get(String url) throws IOException {
				throw new IOException();
			}
		};

		ChatResponse response = urban.onMessage(message, null);
		assertTrue(response.getMessage().startsWith(":1 Sorry"));
	}

	@Test
	public void non_xml_response() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool");
		DefineCommand urban = new DefineCommand("theKey") {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://www.dictionaryapi.com/api/v1/references/collegiate/xml/cool?key=theKey", url);
				return new ByteArrayInputStream("not XML".getBytes());
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertTrue(response.getMessage().startsWith(":1 Sorry"));
	}

	@Test
	public void not_found() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool");
		DefineCommand urban = new DefineCommand("theKey") {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://www.dictionaryapi.com/api/v1/references/collegiate/xml/cool?key=theKey", url);
				return new ByteArrayInputStream("<entry_list/>".getBytes());
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 No definitions found.", response.getMessage());
	}

	@Test
	public void not_found_suggestions_one() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "col");
		DefineCommand urban = new DefineCommand("theKey") {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://www.dictionaryapi.com/api/v1/references/collegiate/xml/col?key=theKey", url);
				return new ByteArrayInputStream("<entry_list><suggestion>cool</suggestion></entry_list>".getBytes());
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 No definitions found. Did you mean cool?", response.getMessage());
	}

	@Test
	public void not_found_suggestions_two() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "col");
		DefineCommand urban = new DefineCommand("theKey") {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://www.dictionaryapi.com/api/v1/references/collegiate/xml/col?key=theKey", url);
				return new ByteArrayInputStream("<entry_list><suggestion>cool</suggestion><suggestion>cold</suggestion></entry_list>".getBytes());
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 No definitions found. Did you mean cool or cold?", response.getMessage());
	}

	@Test
	public void not_found_suggestions_multiple() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "col");
		DefineCommand urban = new DefineCommand("theKey") {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://www.dictionaryapi.com/api/v1/references/collegiate/xml/col?key=theKey", url);
				return new ByteArrayInputStream("<entry_list><suggestion>cool</suggestion><suggestion>cold</suggestion><suggestion>colt</suggestion></entry_list>".getBytes());
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		assertEquals(":1 No definitions found. Did you mean cool, cold, or colt?", response.getMessage());
	}

	@Test
	public void multiple_definitions() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "cool");
		DefineCommand urban = new DefineCommand("theKey") {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://www.dictionaryapi.com/api/v1/references/collegiate/xml/cool?key=theKey", url);
				return getClass().getResourceAsStream("cool.xml");
			}
		};
		ChatResponse response = urban.onMessage(message, null);
		//@formatter:off
		assertEquals(
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
			, response.getMessage()
		);
		//@formatter:on
	}

	@Test
	public void encode_parameters() throws Exception {
		ChatCommand message = chatCommandBuilder.build(1, "grand piano");
		DefineCommand urban = new DefineCommand("theKey") {
			@Override
			InputStream get(String url) throws IOException {
				assertEquals("http://www.dictionaryapi.com/api/v1/references/collegiate/xml/grand%20piano?key=theKey", url);
				return new ByteArrayInputStream("<entry_list/>".getBytes());
			}
		};
		urban.onMessage(message, null);
	}
}
