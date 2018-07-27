package oakbot.command.learn;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.command.Command;
import oakbot.util.ChatCommandBuilder;

public class LearnCommandTest {
	private final ChatCommandBuilder ccb = new ChatCommandBuilder(new LearnCommand(null, null).name());

	@Test
	public void onMessage() throws Exception {
		LearnedCommands learnedCommands = new LearnedCommands();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "test <b>one</b>");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");
		when(context.getOriginalMessageContent(1)).thenReturn("/learn test **one**");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 Saved.", response.getMessage());

		LearnedCommand learned = learnedCommands.get("test");
		assertEquals("**one**", learned.onMessage(null, null).getMessage());
	}

	@Test
	public void onMessage_no_command_name() throws Exception {
		LearnedCommands learnedCommands = new LearnedCommands();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 Syntax: `/learn NAME OUTPUT`", response.getMessage());
	}

	@Test
	public void onMessage_no_command_output() throws Exception {
		LearnedCommands learnedCommands = new LearnedCommands();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "test");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 Syntax: `/learn NAME OUTPUT`", response.getMessage());
	}

	@Test
	public void onMessage_bad_command_name() throws Exception {
		LearnedCommands learnedCommands = new LearnedCommands();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "foo*bar value");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 Tricksy hobbitses. Command names can only contain letters (a-z) and numbers.", response.getMessage());
	}

	@Test
	public void onMessage_command_exists() throws Exception {
		Command existing = mock(Command.class);
		when(existing.name()).thenReturn("test");

		LearnedCommands learnedCommands = new LearnedCommands();
		LearnCommand command = new LearnCommand(Arrays.asList(existing), learnedCommands);

		ChatCommand cc = ccb.build(1, "test value");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 A command with that name already exists.", response.getMessage());
	}

	@Test
	public void onMessage_command_exists_check_alias() throws Exception {
		Command existing = mock(Command.class);
		when(existing.name()).thenReturn("name");
		when(existing.aliases()).thenReturn(Arrays.asList("test"));

		LearnedCommands learnedCommands = new LearnedCommands();
		LearnCommand command = new LearnCommand(Arrays.asList(existing), learnedCommands);

		ChatCommand cc = ccb.build(1, "test value");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 A command with that name already exists.", response.getMessage());
	}

	@Test
	public void onMessage_learned_command_exists() throws Exception {
		LearnedCommands learnedCommands = new LearnedCommands();
		learnedCommands.add("test", "output");
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "test value");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 A command with that name already exists.", response.getMessage());
	}

	@Test
	public void onMessage_fixed_width() throws Exception {
		LearnedCommands learnedCommands = new LearnedCommands();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "<pre class='partial'>test **one**</pre>");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");
		when(context.getOriginalMessageContent(1)).thenReturn("    /learn test **one**");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 Saved.", response.getMessage());

		LearnedCommand learned = learnedCommands.get("test");
		assertEquals("    **one**", learned.onMessage(null, null).getMessage());
	}

	@Test
	public void onMessage_IOException() throws Exception {
		LearnedCommands learnedCommands = new LearnedCommands();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "test <b>one</b>");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");
		when(context.getOriginalMessageContent(1)).thenThrow(new IOException());

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 Saved.", response.getMessage());

		LearnedCommand learned = learnedCommands.get("test");
		assertEquals("**one**", learned.onMessage(null, null).getMessage());
	}

	@Test
	public void onMessage_regex_fail() throws Exception {
		LearnedCommands learnedCommands = new LearnedCommands();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "test <b>one</b>");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");
		when(context.getOriginalMessageContent(1)).thenReturn("plaintext message doesn't match");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 Saved.", response.getMessage());

		LearnedCommand learned = learnedCommands.get("test");
		assertEquals("**one**", learned.onMessage(null, null).getMessage());
	}
}
