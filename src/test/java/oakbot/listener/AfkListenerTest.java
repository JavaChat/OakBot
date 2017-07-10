package oakbot.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.Test;

import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.command.AfkCommand;

/**
 * @author Michael Angstadt
 */
public class AfkListenerTest {
	private final BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), 0);

	@Test
	public void mention_full() {
		AfkCommand command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "brb");
		command.setAway(23, "Fra Niegel", "");
		command.setAway(24, "Alexander", "");

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.messageId(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Frank?")
		.build();
		//@formatter:on

		AfkListener listener = new AfkListener(command);
		ChatResponse response = listener.onMessage(message, context);
		assertEquals(":1 Frank is away", response.getMessage());
	}

	@Test
	public void mention_partial() {
		AfkCommand command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "brb");
		command.setAway(23, "fra Niegel", "");
		command.setAway(24, "Alexander", "");

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.messageId(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Fran?")
		.build();
		//@formatter:on

		AfkListener listener = new AfkListener(command);
		ChatResponse response = listener.onMessage(message, context);
		assertEquals(":1 Frank is away\nFranny is away: brb\nfra Niegel is away", response.getMessage());
	}

	@Test
	public void mention_too_short() {
		AfkCommand command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "brb");
		command.setAway(23, "Fra Niegel", "");
		command.setAway(24, "Alexander", "");

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.messageId(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Fr?")
		.build();
		//@formatter:on

		AfkListener listener = new AfkListener(command);
		ChatResponse response = listener.onMessage(message, context);
		assertNull(response);
	}

	@Test
	public void mention_prevent_spam() {
		AfkCommand command = new AfkCommand();
		command.setAway(21, "Frank", "");
		command.setAway(22, "Franny", "");

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.messageId(1)
			.userId(65)
			.username("Kyle")
			.content("Where are you, @Frank @Frank @Fran @Franny?")
		.build();
		//@formatter:on

		AfkListener listener = new AfkListener(command);
		ChatResponse response = listener.onMessage(message, context);
		assertEquals(":1 Frank is away\nFranny is away", response.getMessage());
		response = listener.onMessage(message, context);
		assertNull(response);
	}

	@Test
	public void back() {
		AfkCommand command = new AfkCommand();
		command.setAway(21, "Frank", "");

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.messageId(1)
			.userId(21)
			.username("Frank")
			.content("I'm back.")
		.build();
		//@formatter:on

		AfkListener listener = new AfkListener(command);
		ChatResponse response = listener.onMessage(message, context);
		assertEquals(":1 Welcome back!", response.getMessage());

		response = listener.onMessage(message, context);
		assertNull(response);
	}

	@Test
	public void already_away_and_typed_afk_command() {
		AfkCommand command = new AfkCommand();
		command.setAway(21, "Frank", "");

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.messageId(1)
			.userId(21)
			.username("Frank")
			.content("/afk")
		.build();
		//@formatter:on

		AfkListener listener = new AfkListener(command);
		ChatResponse response = listener.onMessage(message, context);
		assertNull(response);
	}
}
