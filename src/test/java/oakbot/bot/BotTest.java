package oakbot.bot;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.LogManager;

import oakbot.chat.ChatConnection;
import oakbot.chat.ChatMessage;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Michael Angstadt
 */
public class BotTest {
	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	@Test(expected = IllegalArgumentException.class)
	public void builder() throws Exception {
		new Bot.Builder().build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void bad_login() throws Exception {
		ChatConnection connection = mock(ChatConnection.class);
		doThrow(new IllegalArgumentException()).when(connection).login("user", "pass");

		Bot bot = new Bot.Builder().login("user", "pass").connection(connection).build();
		bot.connect(false);
	}

	@Ignore
	@Test
	public void unknown_command() throws Exception {
		ChatConnection connection = mock(ChatConnection.class);
		when(connection.getNewMessages(1)).then(new Answer<List<ChatMessage>>() {
			private int count = 0;

			@Override
			public List<ChatMessage> answer(InvocationOnMock invocation) throws Throwable {
				switch (count++) {
				case 0:
					return Collections.emptyList();
				case 1:
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
					return Arrays.asList(message);
				case 2:
					//@formatter:off
					message = new ChatMessage.Builder()
						.content("=shutdown")
						.messageId(2)
						.roomId(1)
						.timestamp(LocalDateTime.now())
						.userId(1)
						.username("User1")
					.build();
					//@formatter:on
					return Arrays.asList(message);
				}
				fail();
				return null;
			}
		});

		Bot bot = new Bot.Builder().connection(connection).rooms(1).heartbeat(100).build();
		bot.connect(false); //TODO this call is blocking...how to test?
	}

	@Ignore
	@Test
	public void command() {
		//TODO
	}
}
