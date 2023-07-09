package oakbot.listener;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Multimap;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.chat.ChatMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommandsDao;

/**
 * @author Michael Angstadt
 */
public class CommandListenerTest {
	@Test
	public void onMessage() {
		Command command = new UpperCommand();
		CommandListener listener = new CommandListener(Arrays.asList(command), new LearnedCommandsDao());

		ChatMessage message = new ChatMessage.Builder().content("/upper test").build();
		
		IBot bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		
		ChatActions response = listener.onMessage(message, bot);

		assertMessage("TEST", response);
	}

	@Test
	public void onMessage_alias() {
		Command command = new UpperCommand();
		CommandListener listener = new CommandListener(Arrays.asList(command), new LearnedCommandsDao());

		ChatMessage message = new ChatMessage.Builder().content("/capital test").build();
		
		IBot bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		
		ChatActions response = listener.onMessage(message, bot);

		assertMessage("TEST", response);
	}

	@Test
	public void onMessage_no_trigger() {
		Command command = new UpperCommand();
		CommandListener listener = new CommandListener(Arrays.asList(command), new LearnedCommandsDao());

		ChatMessage message = new ChatMessage.Builder().content("upper test").build();
		
		IBot bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		
		ChatActions response = listener.onMessage(message, bot);

		assertTrue(response.isEmpty());
	}

	@Test
	public void onMessage_learned() {
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(new LearnedCommand.Builder().name("foo").output("bar").build());
		learnedCommands.add(new LearnedCommand.Builder().name("name").output("reply").build());
		CommandListener listener = new CommandListener(Arrays.asList(), learnedCommands);

		ChatMessage message = new ChatMessage.Builder().content("/foo").build();
		
		IBot bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		ChatActions response = listener.onMessage(message, bot);

		assertMessage("bar", response);
	}

	@Test
	public void onMessage_unrecognized() {
		Command command = new UpperCommand();
		CommandListener listener = new CommandListener(Arrays.asList(command), new LearnedCommandsDao());

		ChatMessage message = new ChatMessage.Builder().content("/foo").build();
		
		IBot bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		
		ChatActions response = listener.onMessage(message, bot);

		assertTrue(response.isEmpty());
	}

	@Test
	public void onMessage_unrecognized_with_handler() {
		Command command = new UpperCommand();
		CommandListener listener = new CommandListener(Arrays.asList(command), new LearnedCommandsDao(), (message, context) -> {
			return ChatActions.post("Unknown command.");
		});

		ChatMessage message = new ChatMessage.Builder().content("/foo").build();
		
		IBot bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		
		ChatActions response = listener.onMessage(message, bot);

		assertMessage("Unknown command.", response);
	}

	@Test
	public void checkForDuplicateNames_no_duplicates() {
		List<Command> commands = Arrays.asList( //@formatter:off
			new SimpleCommand("one"),
			new SimpleCommand("two")
		); //@formatter:on

		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(new LearnedCommand.Builder().name("lone").build());
		learnedCommands.add(new LearnedCommand.Builder().name("ltwo").build());

		CommandListener listener = new CommandListener(commands, learnedCommands);
		assertTrue(listener.checkForDuplicateNames().isEmpty());
	}

	@Test
	public void checkForDuplicateNames_duplicates() {
		Command a = new SimpleCommand("one");
		Command b = new SimpleCommand("two");
		Command c = new SimpleCommand("TWO");
		Command d = new SimpleCommand("three", "ONE");
		List<Command> commands = Arrays.asList(a, b, c, d);

		LearnedCommand e = new LearnedCommand.Builder().name("one").build();
		LearnedCommand f = new LearnedCommand.Builder().name("ltwo").build();
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(e);
		learnedCommands.add(f);

		CommandListener listener = new CommandListener(commands, learnedCommands);
		Multimap<String, Command> duplicates = listener.checkForDuplicateNames();

		Collection<Command> dups = duplicates.get("one");
		assertEquals(3, dups.size());
		assertTrue(dups.contains(a));
		assertTrue(dups.contains(d));
		assertTrue(dups.contains(e));

		dups = duplicates.get("two");
		assertEquals(2, dups.size());
		assertTrue(dups.contains(b));
		assertTrue(dups.contains(c));

		assertEquals(5, duplicates.size());
	}

	private static class SimpleCommand implements Command {
		private final String name;
		private final Collection<String> aliases;

		public SimpleCommand(String name, String... aliases) {
			this.name = name;
			this.aliases = Arrays.asList(aliases);
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public Collection<String> aliases() {
			return aliases;
		}

		@Override
		public HelpDoc help() {
			return null;
		}

		@Override
		public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
			return ChatActions.post("response");
		}
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
		public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
			return ChatActions.post(chatCommand.getContent().toUpperCase());
		}
	}
}
