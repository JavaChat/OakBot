package oakbot.bot;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Test;

import oakbot.chat.ChatMessage;
import oakbot.chat.IChatClient;
import oakbot.chat.IRoom;
import oakbot.chat.InvalidCredentialsException;
import oakbot.chat.SplitStrategy;
import oakbot.chat.event.MessageEditedEvent;
import oakbot.chat.event.MessagePostedEvent;
import oakbot.command.Command;
import oakbot.command.learn.LearnedCommands;
import oakbot.filter.ChatResponseFilter;
import oakbot.listener.Listener;

/**
 * @author Michael Angstadt
 */
public class BotTest {
	private long eventId = 1;
	private long messageId = 1;

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

		bot.connect(false).join();
	}

	@Test
	public void listener() throws Exception {
		MessagePostedEvent event1 = event("Test1");
		MessagePostedEvent event2 = event("Test2");

		IChatClient connection = mock(IChatClient.class);
		MockRoom room = new MockRoom(1, connection);
		Listener listener = mock(Listener.class);
		when(listener.onMessage(eq(event2.getMessage()), any(BotContext.class))).thenReturn(new ChatResponse("reply"));

		//@formatter:off
		Bot bot = basicBot()
			.connection(connection)
			.trigger("=")
			.listeners(listener)
		.build();
		//@formatter:on

		Thread t = bot.connect(false);
		room.pushEvents(event1, event2);
		bot.stop();
		t.join();

		verify(listener).onMessage(same(event1.getMessage()), any(BotContext.class));
		verify(listener).onMessage(same(event2.getMessage()), any(BotContext.class));
		verify(room.room).sendMessage("reply", SplitStrategy.NONE);
		verify(connection).close();
	}

	@Test
	public void command() throws Exception {
		//ignore this message because it is not a command
		MessagePostedEvent event1 = event("Ignore me");

		MessagePostedEvent event2 = event("=name command");
		ChatCommand expectedChatCommand2 = new ChatCommand(event2.getMessage(), "name", "command");

		MessagePostedEvent event3 = event("=name reply");
		ChatCommand expectedChatCommand3 = new ChatCommand(event3.getMessage(), "name", "reply");

		IChatClient connection = mock(IChatClient.class);
		MockRoom room = new MockRoom(1, connection);
		Command command = mock(Command.class);
		when(command.name()).thenReturn("name");
		when(command.aliases()).thenReturn(Arrays.asList("name"));
		when(command.onMessage(eq(expectedChatCommand3), any(BotContext.class))).thenReturn(new ChatResponse("reply"));

		//@formatter:off
		Bot bot = basicBot()
			.connection(connection)
			.trigger("=")
			.commands(command)
		.build();
		//@formatter:on

		Thread t = bot.connect(false);
		room.pushEvents(event1, event2, event3);
		bot.stop();
		t.join();

		verify(command).onMessage(eq(expectedChatCommand2), any(BotContext.class));
		verify(room.room).sendMessage("reply", SplitStrategy.NONE);
		verify(connection).close();
	}

	@Test
	public void learned_command() throws Exception {
		MessagePostedEvent event1 = event("=ignore");
		MessagePostedEvent event2 = event("=foo");

		IChatClient connection = mock(IChatClient.class);
		MockRoom room = new MockRoom(1, connection);

		LearnedCommands learnedCommands = new LearnedCommands();
		learnedCommands.add("foo", "bar");

		//@formatter:off
		Bot bot = basicBot()
			.connection(connection)
			.trigger("=")
			.learnedCommands(learnedCommands)
		.build();
		//@formatter:on

		Thread t = bot.connect(false);
		room.pushEvents(event1, event2);
		bot.stop();
		t.join();

		verify(room.room).sendMessage("bar", SplitStrategy.NONE);
		verify(connection).close();
	}

	@Test
	public void filter() throws Exception {
		MessagePostedEvent event1 = event("Test");

		Listener listener = mock(Listener.class);
		when(listener.onMessage(eq(event1.getMessage()), any(BotContext.class))).thenReturn(new ChatResponse("reply"));

		ChatResponseFilter filter = new ChatResponseFilter() {
			@Override
			public String filter(String message) {
				return message.toUpperCase();
			}
		};

		{
			IChatClient connection = mock(IChatClient.class);
			MockRoom room = new MockRoom(1, connection);

			//@formatter:off
			Bot bot = basicBot()
				.connection(connection)
				.trigger("=")
				.listeners(listener)
				.responseFilters(filter)
			.build();
			//@formatter:on

			Thread t = bot.connect(false);
			room.pushEvents(event1);
			bot.stop();
			t.join();

			verify(room.room).sendMessage("reply", SplitStrategy.NONE);
		}

		{
			IChatClient connection = mock(IChatClient.class);
			MockRoom room = new MockRoom(1, connection);

			//@formatter:off
			Bot bot = basicBot()
				.connection(connection)
				.trigger("=")
				.listeners(listener)
				.responseFilters(filter)
			.build();
			//@formatter:on

			filter.setEnabled(1, true);

			Thread t = bot.connect(false);
			room.pushEvents(event1);
			bot.stop();
			t.join();

			verify(room.room).sendMessage("REPLY", SplitStrategy.NONE);
		}

		{
			IChatClient connection = mock(IChatClient.class);
			MockRoom room = new MockRoom(1, connection);

			//@formatter:off
			Bot bot = basicBot()
				.connection(connection)
				.trigger("=")
				.listeners(listener)
				.responseFilters(filter)
			.build();
			//@formatter:on

			filter.setEnabled(1, false);

			Thread t = bot.connect(false);
			room.pushEvents(event1);
			bot.stop();
			t.join();

			verify(room.room).sendMessage("reply", SplitStrategy.NONE);
		}

		{
			IChatClient connection = mock(IChatClient.class);
			MockRoom room = new MockRoom(1, connection);

			//@formatter:off
			Bot bot = basicBot()
				.connection(connection)
				.trigger("=")
				.listeners(listener)
				.responseFilters(filter)
			.build();
			//@formatter:on

			filter.setGloballyEnabled(true);

			Thread t = bot.connect(false);
			room.pushEvents(event1);
			bot.stop();
			t.join();

			verify(room.room).sendMessage("REPLY", SplitStrategy.NONE);
		}
	}

	@Test
	public void join_room() {

	}

	@Test
	public void leave_room() {

	}

	@Test
	public void shutdown() {

	}

	@Test
	public void content_null() {

	}

	@Test
	public void admin_user() {

	}

	@Test
	public void banned_user() {

	}

	@Test
	public void onebox() {

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
				.userId(2)
				.username("User")
			.build())
		.build();
		//@formatter:on

		IChatClient connection = mock(IChatClient.class);

		MockRoom room = new MockRoom(1, connection);

		UnknownCommandHandler handler = spy(new UnknownCommandHandler() {
			@Override
			public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
				assertSame(event.getMessage(), chatCommand.getMessage());
				return null;
			}
		});

		//@formatter:off
		Bot bot = basicBot()
			.connection(connection)
			.trigger("=")
			.unknownCommandHandler(handler)
		.build();
		//@formatter:on

		Thread t = bot.connect(false);
		room.pushEvents(event);
		bot.stop();
		t.join();

		verify(handler).onMessage(any(ChatCommand.class), any(BotContext.class));
		verify(connection).close();
	}

	private MessagePostedEvent event(String content) {
		LocalDateTime now = LocalDateTime.now();

		//@formatter:off
		return new MessagePostedEvent.Builder()
			.eventId(eventId++)
			.timestamp(now)
			.message(new ChatMessage.Builder()
				.content(content)
				.messageId(messageId++)
				.roomId(1)
				.timestamp(now)
				.userId(2)
				.username("User")
			.build())
		.build();
		//@formatter:on
	}

	private class MockRoom {
		private final IRoom room;
		private Consumer<MessagePostedEvent> postedHandler;
		private Consumer<MessageEditedEvent> editedHandler;

		@SuppressWarnings("unchecked")
		public MockRoom(int roomId, IChatClient connection) throws Exception {
			room = mock(IRoom.class);
			when(room.getRoomId()).thenReturn(roomId);

			when(connection.joinRoom(roomId)).thenReturn(room);
			when(connection.getRoom(roomId)).thenReturn(room);
			when(connection.isInRoom(roomId)).thenReturn(true);

			doAnswer((invocations) -> {
				postedHandler = (Consumer<MessagePostedEvent>) invocations.getArguments()[1];
				return null;
			}).when(room).addEventListener(eq(MessagePostedEvent.class), any(Consumer.class));

			doAnswer((invocations) -> {
				editedHandler = (Consumer<MessageEditedEvent>) invocations.getArguments()[1];
				return null;
			}).when(room).addEventListener(eq(MessageEditedEvent.class), any(Consumer.class));
		}

		public void pushEvents(MessagePostedEvent... events) {
			for (MessagePostedEvent event : events) {
				postedHandler.accept(event);
			}
		}

		public void pushEvent(MessageEditedEvent event) {
			editedHandler.accept(event);
		}
	}

	private static Bot.Builder basicBot() {
		//@formatter:off
		return new Bot.Builder()
			.trigger("=")
			.rooms(1)
			.user("BotUser", 1);
		//@formatter:on
	}
}
