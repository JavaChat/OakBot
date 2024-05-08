package oakbot.listener;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
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
		var command = new UpperCommand();
		var listener = new CommandListener(List.of(command), new LearnedCommandsDao());

		var message = new ChatMessage.Builder().content("/upper test").build();
		
		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		
		var response = listener.onMessage(message, bot);

		assertMessage("TEST", response);
	}

	@Test
	public void onMessage_alias() {
		var command = new UpperCommand();
		var listener = new CommandListener(List.of(command), new LearnedCommandsDao());

		var message = new ChatMessage.Builder().content("/capital test").build();
		
		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		
		var response = listener.onMessage(message, bot);

		assertMessage("TEST", response);
	}

	@Test
	public void onMessage_no_trigger() {
		var command = new UpperCommand();
		var listener = new CommandListener(List.of(command), new LearnedCommandsDao());

		var message = new ChatMessage.Builder().content("upper test").build();
		
		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		
		var response = listener.onMessage(message, bot);

		assertTrue(response.isEmpty());
	}

	@Test
	public void onMessage_learned() {
		var learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(new LearnedCommand.Builder().name("foo").output("bar").build());
		learnedCommands.add(new LearnedCommand.Builder().name("name").output("reply").build());
		var listener = new CommandListener(List.of(), learnedCommands);

		var message = new ChatMessage.Builder().content("/foo").build();
		
		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		var response = listener.onMessage(message, bot);

		assertMessage("bar", response);
	}

	@Test
	public void onMessage_unrecognized() {
		var command = new UpperCommand();
		var listener = new CommandListener(List.of(command), new LearnedCommandsDao());

		var message = new ChatMessage.Builder().content("/foo").build();
		
		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		
		var response = listener.onMessage(message, bot);

		assertTrue(response.isEmpty());
	}

	@Test
	public void onMessage_unrecognized_with_handler() {
		var command = new UpperCommand();
		var listener = new CommandListener(List.of(command), new LearnedCommandsDao(), (message, context) -> {
			return ChatActions.post("Unknown command.");
		});

		var message = new ChatMessage.Builder().content("/foo").build();
		
		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");
		
		var response = listener.onMessage(message, bot);

		assertMessage("Unknown command.", response);
	}

	@Test
	public void checkForDuplicateNames_no_duplicates() {
		//@formatter:off
		var commands = List.<Command>of(
			new SimpleCommand("one"),
			new SimpleCommand("two")
		);
		//@formatter:on

		var learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(new LearnedCommand.Builder().name("lone").build());
		learnedCommands.add(new LearnedCommand.Builder().name("ltwo").build());

		var listener = new CommandListener(commands, learnedCommands);
		assertTrue(listener.checkForDuplicateNames().isEmpty());
	}

	@Test
	public void checkForDuplicateNames_duplicates() {
		var a = new SimpleCommand("one");
		var b = new SimpleCommand("two");
		var c = new SimpleCommand("TWO");
		var d = new SimpleCommand("three", "ONE");
		var commands = List.<Command>of(a, b, c, d);

		LearnedCommand e = new LearnedCommand.Builder().name("one").build();
		LearnedCommand f = new LearnedCommand.Builder().name("ltwo").build();
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(e);
		learnedCommands.add(f);

		var listener = new CommandListener(commands, learnedCommands);
		var duplicates = listener.checkForDuplicateNames();

		var dups = duplicates.get("one");
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
			this.aliases = List.of(aliases);
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
			return List.of("capital");
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
