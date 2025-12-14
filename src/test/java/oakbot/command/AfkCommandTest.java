package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;

/**
 * @author Michael Angstadt
 */
class AfkCommandTest {
	@Test
	void afk() {
		var afk = new AfkCommand();

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		{
			//@formatter:off
			var message = new ChatMessage.Builder()
				.id(1)
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
			assertMessage("Cya later", 1, actions);
		}

		{
			//@formatter:off
			var message = new ChatMessage.Builder()
				.id(2)
				.userId(2)
				.username("John")
				.content("Are you there, @Kyle?")
			.build();
			//@formatter:on

			var actions = afk.onMessage(message, bot);
			assertMessage("Kyle is away: away", 2, actions);
		}

		{
			//@formatter:off
			var message = new ChatMessage.Builder()
				.id(3)
				.userId(1)
				.username("Kyle")
				.content("I'm back now!")
			.build();
			//@formatter:on

			var actions = afk.onMessage(message, bot);
			assertMessage("Welcome back!", 3, actions);
		}
	}

	@Test
	void mention_full() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "brb");
		command.setAway(23, "Fra Niegel", "");
		command.setAway(24, "Alexander", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.id(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Frank?")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("Frank is away", 1, response);
	}

	@Test
	void mention_partial() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "brb");
		command.setAway(23, "fra Niegel", "");
		command.setAway(24, "Alexander", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.id(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Fran?")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("Frank is away\nFranny is away: brb\nfra Niegel is away", 1, response);
	}

	@Test
	void mention_too_short() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "brb");
		command.setAway(23, "Fra Niegel", "");
		command.setAway(24, "Alexander", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.id(1)
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
	void mention_prevent_spam() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.id(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Frank @Frank @Fran @Franny?")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("Frank is away\nFranny is away", 1, response);
		response = command.onMessage(message, bot);
		assertTrue(response.isEmpty());
	}

	@Test
	void back() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.id(1)
			.userId(21)
			.username("Frank")
			.content("I'm back.")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("Welcome back!", 1, response);

		response = command.onMessage(message, bot);
		assertTrue(response.isEmpty());
	}

	@Test
	void already_away_and_typed_afk_command() {
		var command = new AfkCommand();
		command.setAway(21, "Frank", "");

		//@formatter:off
		var message = new ChatMessage.Builder()
			.id(1)
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
		assertMessage("Cya later", 1, response);
	}
}
