package oakbot.bot;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Test;

import oakbot.chat.ChatMessage;
import oakbot.chat.IChatClient;
import oakbot.chat.IRoom;
import oakbot.chat.InvalidCredentialsException;
import oakbot.chat.event.Event;
import oakbot.chat.event.MessageEditedEvent;
import oakbot.chat.event.MessagePostedEvent;

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
		IChatClient connection = mock(IChatClient.class);
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
		MessagePostedEvent event = new MessagePostedEvent.Builder()
			.eventId(1)
			.timestamp(LocalDateTime.now())
			.message(new ChatMessage.Builder()
				.content("=foobar")
				.messageId(1)
				.roomId(1)
				.timestamp(LocalDateTime.now())
				.userId(1)
				.username("User1")
			.build())
		.build();
		//@formatter:on

		IChatClient connection = mock(IChatClient.class);

		MockRoom room = new MockRoom(1, event);
		when(connection.joinRoom(1)).thenReturn(room.room);
		//when(connection.getRooms()).thenReturn(Arrays.asList(room.room));
		when(connection.getRoom(1)).thenReturn(room.room);
		when(connection.isInRoom(1)).thenReturn(true);

		UnknownCommandHandler handler = spy(new UnknownCommandHandler() {
			@Override
			public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
				assertSame(event.getMessage(), chatCommand.getMessage());
				return null;
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

		Thread t = null;
		try {
			t = new Thread(() -> {
				try {
					bot.connect(false);
				} catch (Exception e) {
					//ignore
				}
			});
			t.start();

			Thread.sleep(500);

			verify(handler).onMessage(any(ChatCommand.class), any(BotContext.class));
		} finally {
			t.interrupt();
		}
	}

	private class MockRoom {
		private final IRoom room;
		private Consumer<MessagePostedEvent> postedHandler;
		private Consumer<MessageEditedEvent> editedHandler;

		@SuppressWarnings("unchecked")
		public MockRoom(int roomId, Event... events) {
			room = mock(IRoom.class);
			when(room.getRoomId()).thenReturn(roomId);

			doAnswer((invocations) -> {
				postedHandler = (Consumer<MessagePostedEvent>) invocations.getArguments()[1];
				return null;
			}).when(room).addEventListener(eq(MessagePostedEvent.class), any(Consumer.class));

			doAnswer((invocations) -> {
				editedHandler = (Consumer<MessageEditedEvent>) invocations.getArguments()[1];

				/*
				 * We know the edit handler is assigned last, so push the events
				 * as soon as we have it.
				 */
				for (Event event : events) {
					if (event instanceof MessagePostedEvent) {
						postedHandler.accept((MessagePostedEvent) event);
					} else if (event instanceof MessageEditedEvent) {
						editedHandler.accept((MessageEditedEvent) event);
					} else {
						fail("The bot does not handle these events: " + event.getClass().getSimpleName());
					}
				}

				return null;
			}).when(room).addEventListener(eq(MessageEditedEvent.class), any(Consumer.class));
		}
	}
}
