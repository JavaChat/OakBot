package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;

/**
 * @author Michael Angstadt
 */
public class AfkCommandTest {
	@Test
	public void afk() {
		var afk = new AfkCommand();

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		{
			//@formatter:off
			var message = new ChatMessage.Builder()
				.messageId(1)
				.userId(1)
				.username("Kyle")
				.content("/afk away")
			.build();
			//@formatter:on

			/*
			 * Message that invoke the afk command are ignored when passed into
			 * Listener.onMessage().
			 */
			var actions = afk.onMessage(message, bot);
			assertTrue(actions.isEmpty());

			var cmd = ChatCommand.fromMessage(message, "/");
			actions = afk.onMessage(cmd, bot);
			assertMessage(":1 Cya later", actions);
		}

		{
			//@formatter:off
			var message = new ChatMessage.Builder()
				.messageId(2)
				.userId(2)
				.username("John")
				.content("Are you there, @Kyle?")
			.build();
			//@formatter:on

			var actions = afk.onMessage(message, bot);
			assertMessage(":2 Kyle is away: away", actions);
		}

		{
			//@formatter:off
			var message = new ChatMessage.Builder()
				.messageId(3)
				.userId(1)
				.username("Kyle")
				.content("I'm back now!")
			.build();
			//@formatter:on

			var actions = afk.onMessage(message, bot);
			assertMessage(":3 Welcome back!", actions);
		}
	}

	@Test
	public void mention_full() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "brb");
		command.setAway(23, "Fra Niegel", "");
		command.setAway(24, "Alexander", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.messageId(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Frank?")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage(":1 Frank is away", response);
	}

	@Test
	public void mention_partial() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "brb");
		command.setAway(23, "fra Niegel", "");
		command.setAway(24, "Alexander", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.messageId(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Fran?")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage(":1 Frank is away\nFranny is away: brb\nfra Niegel is away", response);
	}

	@Test
	public void mention_too_short() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "brb");
		command.setAway(23, "Fra Niegel", "");
		command.setAway(24, "Alexander", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.messageId(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Fr?")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertTrue(response.isEmpty());
	}

	@Test
	public void mention_prevent_spam() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.messageId(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Frank @Frank @Fran @Franny?")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage(":1 Frank is away\nFranny is away", response);
		response = command.onMessage(message, bot);
		assertTrue(response.isEmpty());
	}

	@Test
	public void back() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.messageId(1)
			.userId(21)
			.username("Frank")
			.content("I'm back.")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage(":1 Welcome back!", response);

		response = command.onMessage(message, bot);
		assertTrue(response.isEmpty());
	}

	@Test
	public void already_away_and_typed_afk_command() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.messageId(1)
			.userId(21)
			.username("Frank")
			.content("/afk")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		var response = command.onMessage(message, bot);
		assertTrue(response.isEmpty());

		var cmd = ChatCommand.fromMessage(message, bot.getTrigger());
		response = command.onMessage(cmd, bot);
		assertMessage(":1 Cya later", response);
	}
}
