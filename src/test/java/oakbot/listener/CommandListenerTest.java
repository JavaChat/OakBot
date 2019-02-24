package oakbot.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.bot.UnknownCommandHandler;
import oakbot.chat.ChatMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommandsDao;

/**
 * @author Michael Angstadt
 */
public class CommandListenerTest {
	private final static BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), 0);

	@Test
	public void onMessage() {
		Command command = new UpperCommand();
		CommandListener listener = new CommandListener(Arrays.asList(command), new LearnedCommandsDao());

		ChatMessage message = new ChatMessage.Builder().content("/upper test").build();
		ChatResponse response = listener.onMessage(message, context);

		assertEquals("TEST", response.getMessage());
	}

	@Test
	public void onMessage_alias() {
		Command command = new UpperCommand();
		CommandListener listener = new CommandListener(Arrays.asList(command), new LearnedCommandsDao());

		ChatMessage message = new ChatMessage.Builder().content("/capital test").build();
		ChatResponse response = listener.onMessage(message, context);

		assertEquals("TEST", response.getMessage());
	}

	@Test
	public void onMessage_no_trigger() {
		Command command = new UpperCommand();
		CommandListener listener = new CommandListener(Arrays.asList(command), new LearnedCommandsDao());

		ChatMessage message = new ChatMessage.Builder().content("upper test").build();
		ChatResponse response = listener.onMessage(message, context);

		assertNull(response);
	}

	@Test
	public void onMessage_learned() {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(new LearnedCommand.Builder().name("foo").output("bar").build());
		learnedCommands.add(new LearnedCommand.Builder().name("name").output("reply").build());
		CommandListener listener = new CommandListener(Arrays.asList(), learnedCommands);

		ChatMessage message = new ChatMessage.Builder().content("/foo").build();

		ChatResponse response = listener.onMessage(message, context);

		assertEquals("bar", response.getMessage());
	}

	@Test
	public void onMessage_unrecognized() {
		Command command = new UpperCommand();
		CommandListener listener = new CommandListener(Arrays.asList(command), new LearnedCommandsDao());

		ChatMessage message = new ChatMessage.Builder().content("/foo").build();
		ChatResponse response = listener.onMessage(message, context);

		assertNull(response);
	}

	@Test
	public void onMessage_unrecognized_with_handler() {
		UnknownCommandHandler handler = mock(UnknownCommandHandler.class);
		when(handler.onMessage(any(ChatCommand.class), any(BotContext.class))).thenReturn(new ChatResponse("Unknown command."));
		Command command = new UpperCommand();
		CommandListener listener = new CommandListener(Arrays.asList(command), new LearnedCommandsDao(), handler);

		ChatMessage message = new ChatMessage.Builder().content("/foo").build();
		ChatResponse response = listener.onMessage(message, context);

		assertEquals("Unknown command.", response.getMessage());
	}

	private static class UpperCommand implements Command {
		@Override
		public String name() {
			return "upper";
		}

		@Override
		public Collection<String> aliases() {
			return Arrays.asList("capital");
		}

		@Override
		public HelpDoc help() {
			return null;
		}

		@Override
		public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
			return new ChatResponse(chatCommand.getContent().toUpperCase());
		}
	}
}
