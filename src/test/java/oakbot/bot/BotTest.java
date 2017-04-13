package oakbot.bot;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import oakbot.chat.ChatConnection;
import oakbot.chat.ChatMessage;
import oakbot.chat.ChatMessageHandler;
import oakbot.chat.InvalidCredentialsException;

/**
 * @author Michael Angstadt
 */
public class BotTest {
	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	@Test(expected = IllegalStateException.class)
	public void builder_no_connection() throws Exception {
		new Bot.Builder().build();
	}

	@Test(expected = InvalidCredentialsException.class)
	public void bad_login() throws Exception {
		ChatConnection connection = mock(ChatConnection.class);
		doThrow(new InvalidCredentialsException()).when(connection).login("user", "pass");

		//@formatter:off
		Bot bot = new Bot.Builder()
			.login("user", "pass")
			.connection(connection)
		.build();
		//@formatter:on

		bot.connect(false);
	}

	@Test
	public void unknown_command() throws Exception {
		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.content("=foobar")
			.messageId(1)
			.roomId(1)
			.timestamp(LocalDateTime.now())
			.userId(1)
			.username("User1")
		.build();
		//@formatter:on

		ChatConnection connection = mock(ChatConnection.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) {
				ChatMessageHandler handler = (ChatMessageHandler) invocation.getArguments()[0];
				handler.onMessage(message);
				return null;
			}
		}).when(connection).listen(any(ChatMessageHandler.class));

		ChatResponse response = new ChatResponse("");
		UnknownCommandHandler handler = spy(new UnknownCommandHandler() {
			@Override
			public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
				assertSame(message, chatCommand.getMessage());
				return response;
			}
		});

		//@formatter:off
		Bot bot = new Bot.Builder()
			.connection(connection)
			.trigger("=")
			.rooms(1)
			.user("bot", 2)
			.unknownCommandHandler(handler)
		.build();
		//@formatter:on

		bot.connect(false);

		verify(handler).onMessage(any(ChatCommand.class), any(BotContext.class));
	}
}
