package oakbot.command.learn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.command.Command;
import oakbot.util.ChatCommandBuilder;

public class LearnCommandTest {
	private final ChatCommandBuilder ccb = new ChatCommandBuilder(new LearnCommand(null, null).name());

	@Test
	public void onMessage() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);
		LocalDateTime now = LocalDateTime.now();

		//@formatter:off
		ChatCommand cc = new ChatCommand(
			new ChatMessage.Builder()
				.messageId(1)
				.roomId(2)
				.userId(100)
				.username("Username")
				.timestamp(now)
				.content("/learn name <b>command output</b>")
			.build(),
			"learn",
			"name <b>command output</b>"
		);
		//@formatter:on

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");
		when(context.getOriginalMessageContent(1)).thenReturn("/learn name **command output**");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 Saved.", response.getMessage());

		LearnedCommand learned = learnedCommands.get("name");
		assertEquals(new Long(1), learned.getMessageId());
		assertEquals(new Integer(2), learned.getRoomId());
		assertEquals(new Integer(100), learned.getAuthorUserId());
		assertEquals("Username", learned.getAuthorUsername());
		assertEquals(now, learned.getCreated());
		assertEquals("name", learned.name());
		assertEquals("**command output**", learned.getOutput());
		assertTrue(learned.aliases().isEmpty());
		assertEquals("**command output**", learned.onMessage(null, null).getMessage());
	}

	@Test
	public void onMessage_no_command_name() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 You haven't specified the command name or its output.", response.getMessage());
	}

	@Test
	public void onMessage_no_command_output() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "test");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 You haven't specified the command output.", response.getMessage());
	}

	@Test
	public void onMessage_bad_command_name() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
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

		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
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

		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Arrays.asList(existing), learnedCommands);

		ChatCommand cc = ccb.build(1, "test value");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 A command with that name already exists.", response.getMessage());
	}

	@Test
	public void onMessage_learned_command_exists() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(new LearnedCommand.Builder().name("test").output("output").build());
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);

		ChatCommand cc = ccb.build(1, "test value");

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(cc, context);
		assertEquals(":1 A command with that name already exists.", response.getMessage());
	}

	@Test
	public void onMessage_fixed_width() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
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
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
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
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
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
