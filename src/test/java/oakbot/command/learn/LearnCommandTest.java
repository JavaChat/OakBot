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
import oakbot.command.Command;
import oakbot.util.ChatCommandBuilder;

public class LearnCommandTest {
	@Test
	public void onMessage() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);
		LocalDateTime now = LocalDateTime.now();

		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.roomId(2)
			.userId(100)
			.username("Username")
			.timestamp(now)
			.content("name <b>command output</b>")
		.build(); //@formatter:on

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");
		when(context.getOriginalMessageContent(1)).thenReturn("/learn name **command output**");

		ChatResponse response = command.onMessage(message, context);
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
		ChatCommand message = new ChatCommandBuilder(command).messageId(1).build();

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(message, context);
		assertEquals(":1 You haven't specified the command name or its output.", response.getMessage());
	}

	@Test
	public void onMessage_no_command_output() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("test")
		.build(); //@formatter:on

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(message, context);
		assertEquals(":1 You haven't specified the command output.", response.getMessage());
	}

	@Test
	public void onMessage_bad_command_name() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("foo*bar value")
		.build(); //@formatter:on

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(message, context);
		assertEquals(":1 Tricksy hobbitses. Command names can only contain letters (a-z) and numbers.", response.getMessage());
	}

	@Test
	public void onMessage_command_exists() throws Exception {
		Command existing = mock(Command.class);
		when(existing.name()).thenReturn("test");

		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Arrays.asList(existing), learnedCommands);
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("test value")
		.build(); //@formatter:on

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(message, context);
		assertEquals(":1 A command with that name already exists.", response.getMessage());
	}

	@Test
	public void onMessage_command_exists_check_alias() throws Exception {
		Command existing = mock(Command.class);
		when(existing.name()).thenReturn("name");
		when(existing.aliases()).thenReturn(Arrays.asList("test"));

		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Arrays.asList(existing), learnedCommands);
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("test value")
		.build(); //@formatter:on

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(message, context);
		assertEquals(":1 A command with that name already exists.", response.getMessage());
	}

	@Test
	public void onMessage_learned_command_exists() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(new LearnedCommand.Builder().name("test").output("output").build());
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("test value")
		.build(); //@formatter:on

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");

		ChatResponse response = command.onMessage(message, context);
		assertEquals(":1 A command with that name already exists.", response.getMessage());
	}

	@Test
	public void onMessage_fixed_width() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("<pre class='partial'>test **one**</pre>")
		.build(); //@formatter:on

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");
		when(context.getOriginalMessageContent(1)).thenReturn("    /learn test **one**");

		ChatResponse response = command.onMessage(message, context);
		assertEquals(":1 Saved.", response.getMessage());

		LearnedCommand learned = learnedCommands.get("test");
		assertEquals("    **one**", learned.onMessage(null, null).getMessage());
	}

	@Test
	public void onMessage_IOException() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("test <b>one</b>")
		.build(); //@formatter:on

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");
		when(context.getOriginalMessageContent(1)).thenThrow(new IOException());

		ChatResponse response = command.onMessage(message, context);
		assertEquals(":1 Saved.", response.getMessage());

		LearnedCommand learned = learnedCommands.get("test");
		assertEquals("**one**", learned.onMessage(null, null).getMessage());
	}

	@Test
	public void onMessage_regex_fail() throws Exception {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		LearnCommand command = new LearnCommand(Collections.emptyList(), learnedCommands);
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("test <b>one</b>")
		.build(); //@formatter:on

		BotContext context = mock(BotContext.class);
		when(context.getTrigger()).thenReturn("/");
		when(context.getOriginalMessageContent(1)).thenReturn("plaintext message doesn't match");

		ChatResponse response = command.onMessage(message, context);
		assertEquals(":1 Saved.", response.getMessage());

		LearnedCommand learned = learnedCommands.get("test");
		assertEquals("**one**", learned.onMessage(null, null).getMessage());
	}
}
