package oakbot.command.learn;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.util.ChatCommandBuilder;

class LearnCommandTest {
	@Test
	void onMessage() throws Exception {
		var learnedCommands = new LearnedCommandsDao();
		var command = new LearnCommand(Collections.emptyList(), learnedCommands);
		var now = LocalDateTime.now();

		//@formatter:off
		var message = new ChatCommandBuilder(command) 
			.messageId(1)
			.roomId(2)
			.userId(100)
			.username("Username")
			.timestamp(now)
			.content("name <b>command output</b>")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		when(bot.getOriginalMessageContent(1)).thenReturn("/learn name **command output**");

		var response = command.onMessage(message, bot);
		assertMessage(":1 Saved.", response);

		var learned = learnedCommands.get("name");
		assertEquals(Long.valueOf(1), learned.getMessageId());
		assertEquals(Integer.valueOf(2), learned.getRoomId());
		assertEquals(Integer.valueOf(100), learned.getAuthorUserId());
		assertEquals("Username", learned.getAuthorUsername());
		assertEquals(now, learned.getCreated());
		assertEquals("name", learned.name());
		assertEquals("**command output**", learned.getOutput());
		assertTrue(learned.aliases().isEmpty());
		assertMessage("**command output**", learned.onMessage(message, null));
	}

	@Test
	void onMessage_no_command_name() {
		var learnedCommands = new LearnedCommandsDao();
		var command = new LearnCommand(Collections.emptyList(), learnedCommands);
		var message = new ChatCommandBuilder(command).messageId(1).build();

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		var response = command.onMessage(message, bot);
		assertMessage(":1 You haven't specified the command name or its output.", response);
	}

	@Test
	void onMessage_no_command_output() {
		var learnedCommands = new LearnedCommandsDao();
		var command = new LearnCommand(Collections.emptyList(), learnedCommands);

		//@formatter:off
		var message = new ChatCommandBuilder(command) 
			.messageId(1)
			.content("test")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		var response = command.onMessage(message, bot);
		assertMessage(":1 You haven't specified the command output.", response);
	}

	@Test
	void onMessage_bad_command_name() {
		var learnedCommands = new LearnedCommandsDao();
		var command = new LearnCommand(Collections.emptyList(), learnedCommands);
		
		//@formatter:of
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("foo*bar value")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		var response = command.onMessage(message, bot);
		assertMessage(":1 Tricksy hobbitses. Command names can only contain letters (a-z) and numbers.", response);
	}

	@Test
	void onMessage_command_exists() {
		var existing = mock(Command.class);
		when(existing.name()).thenReturn("test");

		var learnedCommands = new LearnedCommandsDao();
		var command = new LearnCommand(List.of(existing), learnedCommands);

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("test value")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		var response = command.onMessage(message, bot);
		assertMessage(":1 A command with that name already exists.", response);
	}

	@Test
	void onMessage_command_exists_check_alias() {
		var existing = mock(Command.class);
		when(existing.name()).thenReturn("name");
		when(existing.aliases()).thenReturn(List.of("test"));

		var learnedCommands = new LearnedCommandsDao();
		var command = new LearnCommand(List.of(existing), learnedCommands);

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("test value")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		var response = command.onMessage(message, bot);
		assertMessage(":1 A command with that name already exists.", response);
	}

	@Test
	void onMessage_learned_command_exists() {
		var learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(new LearnedCommand.Builder().name("test").output("output").build());
		var command = new LearnCommand(Collections.emptyList(), learnedCommands);

		//@formatter:off
		var message = new ChatCommandBuilder(command) 
			.messageId(1)
			.content("test value")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		var response = command.onMessage(message, bot);
		assertMessage(":1 A command with that name already exists.", response);
	}

	@Test
	void onMessage_fixed_width() throws Exception {
		var learnedCommands = new LearnedCommandsDao();
		var command = new LearnCommand(Collections.emptyList(), learnedCommands);

		//@formatter:off
		var message = new ChatCommandBuilder(command) 
			.messageId(1)
			.content("<pre class='partial'>test **one**</pre>")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		when(bot.getOriginalMessageContent(1)).thenReturn("    /learn test **one**");

		var response = command.onMessage(message, bot);
		assertMessage(":1 Saved.", response);

		var learned = learnedCommands.get("test");
		assertMessage("    **one**", learned.onMessage(message, null));
	}

	@Test
	void onMessage_IOException() throws Exception {
		var learnedCommands = new LearnedCommandsDao();
		var command = new LearnCommand(Collections.emptyList(), learnedCommands);

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("test <b>one</b>")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		when(bot.getOriginalMessageContent(1)).thenThrow(new IOException());

		var response = command.onMessage(message, bot);
		assertMessage(":1 Saved.", response);

		var learned = learnedCommands.get("test");
		assertMessage("**one**", learned.onMessage(message, null));
	}

	@Test
	void onMessage_regex_fail() throws Exception {
		var learnedCommands = new LearnedCommandsDao();
		var command = new LearnCommand(Collections.emptyList(), learnedCommands);

		//@formatter:off
		var message = new ChatCommandBuilder(command) 
			.messageId(1)
			.content("test <b>one</b>")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		when(bot.getOriginalMessageContent(1)).thenReturn("plaintext message doesn't match");

		var response = command.onMessage(message, bot);
		assertMessage(":1 Saved.", response);

		var learned = learnedCommands.get("test");
		assertMessage("**one**", learned.onMessage(message, null));
	}
}
