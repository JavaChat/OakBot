package oakbot.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import oakbot.bot.BotContext.JoinRoomCallback;
import oakbot.chat.ChatMessage;
import oakbot.chat.IChatClient;
import oakbot.chat.IRoom;
import oakbot.chat.InvalidCredentialsException;
import oakbot.chat.RoomNotFoundException;
import oakbot.chat.SplitStrategy;
import oakbot.chat.event.MessagePostedEvent;
import oakbot.command.Command;
import oakbot.command.learn.LearnedCommands;
import oakbot.filter.ChatResponseFilter;
import oakbot.listener.Listener;

/**
 * @author Michael Angstadt
 */
public class BotTest {
	private long eventId;
	private long messageId;

	private ChatServerMock chatServer;
	private ChatClientMock chatClient;

	private boolean runAfter;

	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	@Before
	public void before() {
		eventId = 1;
		messageId = 1;

		chatServer = new ChatServerMock();
		chatClient = spy(new ChatClientMock(chatServer));

		runAfter = true;
	}

	@After
	public void after() throws Exception {
		if (!runAfter) {
			return;
		}

		verify(chatClient).close();
	}

	@Test(expected = IllegalStateException.class)
	public void builder_no_connection() throws Exception {
		runAfter = false;

		new Bot.Builder().build();
	}

	@Test(expected = InvalidCredentialsException.class)
	public void bad_login() throws Exception {
		runAfter = false;

		//@formatter:off
		Bot bot = bot()
			.login("user@example.com", "bad")
		.build();
		//@formatter:on

		bot.connect(false);
	}

	@Test
	public void connect_greeting_broadcast() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);
		IRoom room2 = chatServer.createRoom(2);

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1, 2)
			.greeting("Greetings.")
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, false);

		/*
		 * Verify.
		 */
		verify(room1).sendMessage("Greetings.", SplitStrategy.NONE);
		verify(room2).sendMessage("Greetings.", SplitStrategy.NONE);
	}

	@Test
	public void connect_greeting_quiet() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);
		IRoom room2 = chatServer.createRoom(2);

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1, 2)
			.greeting("Greetings.")
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, true);

		/*
		 * Verify.
		 */
		verify(room1, times(0)).sendMessage("Greetings.", SplitStrategy.NONE);
		verify(room2, times(0)).sendMessage("Greetings.", SplitStrategy.NONE);
	}

	@Test
	public void connect_greeting_no_message() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);
		IRoom room2 = chatServer.createRoom(2);

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1, 2)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, false);

		/*
		 * Verify.
		 */
		verify(room1, times(0)).sendMessage("Greetings.", SplitStrategy.NONE);
		verify(room2, times(0)).sendMessage("Greetings.", SplitStrategy.NONE);
	}

	@Test
	public void listener() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("no reply");
		MessagePostedEvent event2 = event("reply");

		/**
		 * Create the listener
		 */
		Listener listener = mock(Listener.class);
		when(listener.onMessage(same(event1.getMessage()), any(BotContext.class))).thenReturn(null);
		when(listener.onMessage(same(event2.getMessage()), any(BotContext.class))).thenReturn(new ChatResponse("reply"));

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1)
			.listeners(listener)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, event1, event2);

		/*
		 * Verify.
		 */
		verify(listener).onMessage(same(event1.getMessage()), any(BotContext.class));
		verify(listener).onMessage(same(event2.getMessage()), any(BotContext.class));
		verify(room1, times(1)).sendMessage(anyString(), any(SplitStrategy.class));
		verify(room1).sendMessage("reply", SplitStrategy.NONE);
	}

	@Test
	public void command() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("Ignore me");
		MessagePostedEvent event2 = event("=name command");
		MessagePostedEvent event3 = event("=alias command");
		MessagePostedEvent event4 = event("=name reply");

		/**
		 * Create the command.
		 */
		Command command = mock(Command.class);
		when(command.name()).thenReturn("name");
		when(command.aliases()).thenReturn(Arrays.asList("alias"));
		ChatCommand expectedChatCommand2 = new ChatCommand(event2.getMessage(), "name", "command");
		when(command.onMessage(eq(expectedChatCommand2), any(BotContext.class))).thenReturn(null);
		ChatCommand expectedChatCommand3 = new ChatCommand(event3.getMessage(), "alias", "command");
		when(command.onMessage(eq(expectedChatCommand3), any(BotContext.class))).thenReturn(null);
		ChatCommand expectedChatCommand4 = new ChatCommand(event4.getMessage(), "name", "reply");
		when(command.onMessage(eq(expectedChatCommand4), any(BotContext.class))).thenReturn(new ChatResponse("reply"));

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1)
			.commands(command)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, event1, event2, event3, event4);

		/*
		 * Verify.
		 */
		verify(command).onMessage(eq(expectedChatCommand2), any(BotContext.class));
		verify(command).onMessage(eq(expectedChatCommand3), any(BotContext.class));
		verify(command).onMessage(eq(expectedChatCommand4), any(BotContext.class));
		verify(room1, times(1)).sendMessage(anyString(), any(SplitStrategy.class));
		verify(room1).sendMessage("reply", SplitStrategy.NONE);
	}

	@Test
	public void learned_command() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("=ignore");
		MessagePostedEvent event2 = event("=foo");

		/**
		 * Create the learned commands.
		 */
		LearnedCommands learnedCommands = new LearnedCommands();
		learnedCommands.add("foo", "bar");

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1)
			.learnedCommands(learnedCommands)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, event1, event2);

		/*
		 * Verify.
		 */
		verify(room1, times(1)).sendMessage(anyString(), any(SplitStrategy.class));
		verify(room1).sendMessage("bar", SplitStrategy.NONE);
	}

	@Test
	public void filter_disabled_by_default() throws Exception {
		filter(0);
	}

	@Test
	public void filter_enabled_on_another_room() throws Exception {
		filter(1);
	}

	@Test
	public void filter_enabled_on_correct_room() throws Exception {
		filter(2);
	}

	@Test
	public void filter_enabled_globally() throws Exception {
		filter(3);
	}

	private void filter(int num) throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("=command");
		MessagePostedEvent event2 = event("=learned");

		/**
		 * Create the command.
		 */
		Command command = mock(Command.class);
		when(command.name()).thenReturn("command");
		when(command.aliases()).thenReturn(Arrays.asList());
		when(command.onMessage(any(ChatCommand.class), any(BotContext.class))).thenReturn(new ChatResponse("reply"));

		/**
		 * Create the learned commands.
		 */
		LearnedCommands learnedCommands = new LearnedCommands();
		learnedCommands.add("learned", "reply");

		/**
		 * Create the listener.
		 */
		Listener listener = mock(Listener.class);
		when(listener.onMessage(eq(event1.getMessage()), any(BotContext.class))).thenReturn(new ChatResponse("reply"));

		/**
		 * Create the filter.
		 */
		ChatResponseFilter filter = new ChatResponseFilter() {
			@Override
			public String filter(String message) {
				return message.toUpperCase();
			}
		};

		boolean expectedEnabled = false;
		switch (num) {
		case 0:
			break;
		case 1:
			filter.setEnabled(2, true);
			break;
		case 2:
			filter.setEnabled(1, true);
			expectedEnabled = true;
			break;
		case 3:
			filter.setGloballyEnabled(true);
			expectedEnabled = true;
			break;
		}

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1)
			.commands(command)
			.listeners(listener)
			.learnedCommands(learnedCommands)
			.greeting("reply")
			.responseFilters(filter)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, false, event1, event2);

		/*
		 * Verify.
		 */
		String expectedMessage = expectedEnabled ? "REPLY" : "reply";
		verify(room1, times(4)).sendMessage(anyString(), any(SplitStrategy.class));
		verify(room1, times(4)).sendMessage(expectedMessage, SplitStrategy.NONE);
	}

	@Test
	public void join_room() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);
		IRoom room2 = chatServer.createRoom(2);
		chatServer.createRoom(4, false);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("=join 2"); //success
		MessagePostedEvent event2 = event("=join 2"); //success
		MessagePostedEvent event3 = event("=join 3"); //404
		MessagePostedEvent event4 = event("=join 3"); //404
		MessagePostedEvent event5 = event("=join 4"); //can't post
		MessagePostedEvent event6 = event("=join 4"); //can't post
		MessagePostedEvent event7 = event("=join 999"); //IOException
		MessagePostedEvent event8 = event("=join 999"); //IOException

		/*
		 * Create the join callback.
		 */
		JoinRoomCallback callback = mock(JoinRoomCallback.class);
		when(callback.success()).thenReturn(null, new ChatResponse("success"));
		when(callback.ifRoomDoesNotExist()).thenReturn(null, new ChatResponse("ifRoomDoesNotExist"));
		when(callback.ifBotDoesNotHavePermission()).thenReturn(null, new ChatResponse("ifBotDoesNotHavePermission"));
		when(callback.ifOther(any(IOException.class))).thenReturn(null, new ChatResponse("ifOther"));

		/*
		 * Create the join command.
		 */
		Command command = mock(Command.class);
		when(command.name()).thenReturn("join");
		when(command.aliases()).thenReturn(Arrays.asList());
		when(command.onMessage(any(ChatCommand.class), any(BotContext.class))).then((invocation) -> {
			ChatCommand chatCommand = (ChatCommand) invocation.getArguments()[0];
			BotContext context = (BotContext) invocation.getArguments()[1];

			int roomId = Integer.parseInt(chatCommand.getContent());
			context.joinRoom(roomId, callback);
			return null;
		});

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.greeting("Greetings.")
			.rooms(1)
			.commands(command)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, event1, event2, event3, event4, event5, event6, event7, event8);

		/*
		 * Verify.
		 */
		verify(chatClient).joinRoom(2);
		verify(chatClient, times(2)).joinRoom(3);
		verify(chatClient, times(2)).joinRoom(4);
		verify(chatClient, times(2)).joinRoom(999);

		verify(callback, times(2)).success();
		verify(callback, times(2)).ifRoomDoesNotExist();
		verify(callback, times(2)).ifBotDoesNotHavePermission();
		verify(callback, times(2)).ifOther(any(IOException.class));

		verify(room1, times(4)).sendMessage(anyString(), any(SplitStrategy.class));
		verify(room1).sendMessage("success", SplitStrategy.NONE);
		verify(room1).sendMessage("ifRoomDoesNotExist", SplitStrategy.NONE);
		verify(room1).sendMessage("ifBotDoesNotHavePermission", SplitStrategy.NONE);
		verify(room1).sendMessage("ifOther", SplitStrategy.NONE);

		verify(room2, times(1)).sendMessage(anyString(), any(SplitStrategy.class));
		verify(room2).sendMessage("Greetings.", SplitStrategy.NONE);
	}

	@Test
	public void leave_room() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);
		IRoom room2 = chatServer.createRoom(2);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("=leave");

		/*
		 * Create the leave command.
		 */
		Command command = mock(Command.class);
		when(command.name()).thenReturn("leave");
		when(command.aliases()).thenReturn(Arrays.asList());
		when(command.onMessage(any(ChatCommand.class), any(BotContext.class))).then((invocation) -> {
			BotContext context = (BotContext) invocation.getArguments()[1];
			context.leaveRoom(2);
			return null;
		});

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1, 2)
			.commands(command)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, event1);

		/*
		 * Verify.
		 */
		verify(room2).leave();
		assertEquals(Arrays.asList(1), bot.getRooms().getRooms());
	}

	@Test
	public void shutdown_broadcast() throws Exception {
		shutdown(true, true);
	}

	@Test
	public void shutdown_message_current_room() throws Exception {
		shutdown(true, false);
	}

	@Test
	public void shutdown_no_message() throws Exception {
		shutdown(false, false);
	}

	private void shutdown(boolean postAMessage, boolean broadcastTheMessage) throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);
		IRoom room2 = chatServer.createRoom(2);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("=shutdown");

		/*
		 * Create the shutdown command.
		 */
		Command command = mock(Command.class);
		when(command.name()).thenReturn("shutdown");
		when(command.aliases()).thenReturn(Arrays.asList());
		when(command.onMessage(any(ChatCommand.class), any(BotContext.class))).then((invocation) -> {
			BotContext context = (BotContext) invocation.getArguments()[1];
			String content = postAMessage ? "Bye." : null;
			context.shutdownBot(content, broadcastTheMessage);
			return null;
		});

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1, 2)
			.commands(command)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		Thread t = bot.connect(true);
		chatServer.pushEvents(event1);
		//bot.stop(); //this method call should not be necessary
		t.join();

		/*
		 * Verify.
		 */
		verify(room1, times(postAMessage ? 1 : 0)).sendMessage("Bye.", SplitStrategy.NONE);
		verify(room2, times(postAMessage && broadcastTheMessage ? 1 : 0)).sendMessage("Bye.", SplitStrategy.NONE);
	}

	@Test
	public void content_null() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event(null);

		/**
		 * Create the listener
		 */
		Listener listener = mock(Listener.class);

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1)
			.listeners(listener)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, event1);

		/*
		 * Verify.
		 */
		verify(listener, times(0)).onMessage(same(event1.getMessage()), any(BotContext.class));
	}

	@Test
	public void admin_user() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("Test", 2);
		MessagePostedEvent event2 = event("Test", 100);

		/**
		 * Create the listener
		 */
		Listener listener = mock(Listener.class);
		when(listener.onMessage(same(event1.getMessage()), any(BotContext.class))).then((invocation) -> {
			BotContext context = (BotContext) invocation.getArguments()[1];
			assertFalse(context.isAuthorAdmin());
			return null;
		});
		when(listener.onMessage(same(event2.getMessage()), any(BotContext.class))).then((invocation) -> {
			BotContext context = (BotContext) invocation.getArguments()[1];
			assertTrue(context.isAuthorAdmin());
			return null;
		});

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1)
			.admins(100)
			.listeners(listener)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, event1, event2);

		/*
		 * Verify.
		 */
		verify(listener).onMessage(same(event1.getMessage()), any(BotContext.class));
		verify(listener).onMessage(same(event2.getMessage()), any(BotContext.class));
	}

	@Test
	public void banned_user() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("Test", 2);
		MessagePostedEvent event2 = event("Test", 100);

		/**
		 * Create the listener
		 */
		Listener listener = mock(Listener.class);

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1)
			.bannedUsers(100)
			.listeners(listener)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, event1, event2);

		/*
		 * Verify.
		 */
		verify(listener).onMessage(same(event1.getMessage()), any(BotContext.class));
		verify(listener, times(0)).onMessage(same(event2.getMessage()), any(BotContext.class));
	}

	@Test
	public void onebox() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);
		when(room1.sendMessage("http://en.wikipedia.org/wiki/Java", SplitStrategy.NONE)).thenReturn(Arrays.asList(100L));

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("trigger the listener");
		//@formatter:off
		LocalDateTime now = LocalDateTime.now();
		MessagePostedEvent event2 = new MessagePostedEvent.Builder()
			.eventId(eventId++)
			.timestamp(now)
			.message(new ChatMessage.Builder()
				.content("<div class=\"onebox ob-wikipedia\"><img class=\"ob-wikipedia-image\" src=\"//upload.wikimedia.org/wikipedia/commons/thumb/4/43/Java_Topography.png/220px-Java_Topography.png\" /><div class=\"ob-wikipedia-title\"><img src=\"//en.wikipedia.org/static/favicon/wikipedia.ico\" width=\"24\" height=\"24\" class=\"ob-wikipedia-favicon\"/> <a rel=\"nofollow noopener noreferrer\" href=\"http://en.wikipedia.org/wiki/Java\">Java</a></div><div class=\"ob-wikipedia-text\">Java (Indonesian: Jawa; Javanese: ꦗꦮ; Sundanese: ᮏᮝ) is an island of Indonesia. With a population of over 141 million (the island itself) or 145 million (the administrative region), Java is home to 56.7 percent of the Indonesian population and is the most populous island on Earth. The Indonesian capital city, Jakarta, is located on western Java. Much of Indonesian history took place on Java. It was the center of powerful Hindu-Buddhist empires, the Islamic sultanates, and the core of the colonial Dutch East Indies. Java was also the center of the Indonesian struggle for independence during the...</div></div>")
				.messageId(100)
				.timestamp(now)
				.userId(1)
				.username("BotUser")
				.roomId(1)
			.build())
		.build();
		//@formatter:on

		/**
		 * Create the listener
		 */
		Listener listener = mock(Listener.class);
		when(listener.onMessage(same(event1.getMessage()), any(BotContext.class))).then((invocation) -> {
			return new ChatResponse("http://en.wikipedia.org/wiki/Java");
		});

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1)
			.hideOneboxesAfter(1000)
			.listeners(listener)
		.build();
		//@formatter:on

		/*
		 * Stop the bot when the bot sends the edit request.
		 */
		doAnswer((invocation) -> {
			bot.stop();
			return null;
		}).when(room1).editMessage(100, "> http://en.wikipedia.org/wiki/Java");

		/**
		 * Run the bot.
		 */
		long start = System.currentTimeMillis();
		Thread t = bot.connect(true);
		chatServer.pushEvents(event1, event2);
		t.join();
		long stop = System.currentTimeMillis();

		/*
		 * Verify.
		 */
		verify(room1).sendMessage("http://en.wikipedia.org/wiki/Java", SplitStrategy.NONE);
		verify(room1).editMessage(100, "> http://en.wikipedia.org/wiki/Java");
		assertTrue(stop - start >= 1000);
	}

	@Test
	public void unknown_command() throws Exception {
		/**
		 * Setup the chat rooms.
		 */
		IRoom room1 = chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		MessagePostedEvent event1 = event("=foobar");
		MessagePostedEvent event2 = event("=foobar");

		/*
		 * Create the handler.
		 */
		UnknownCommandHandler handler = mock(UnknownCommandHandler.class);
		ChatCommand expectedChatCommand1 = new ChatCommand(event1.getMessage(), "foobar", "");
		when(handler.onMessage(eq(expectedChatCommand1), any(BotContext.class))).thenReturn(null);
		ChatCommand expectedChatCommand2 = new ChatCommand(event2.getMessage(), "foobar", "");
		when(handler.onMessage(eq(expectedChatCommand2), any(BotContext.class))).thenReturn(new ChatResponse("reply"));

		/**
		 * Create the bot.
		 */
		//@formatter:off
		Bot bot = bot()
			.rooms(1)
			.unknownCommandHandler(handler)
		.build();
		//@formatter:on

		/**
		 * Run the bot.
		 */
		run(bot, event1, event2);

		/*
		 * Verify.
		 */
		verify(handler, times(2)).onMessage(any(ChatCommand.class), any(BotContext.class));
		verify(room1, times(1)).sendMessage(anyString(), any(SplitStrategy.class));
		verify(room1).sendMessage("reply", SplitStrategy.NONE);
	}

	private MessagePostedEvent event(String content) {
		return event(content, 2);
	}

	private MessagePostedEvent event(String content, int userId) {
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
				.userId(userId)
				.username("User")
			.build())
		.build();
		//@formatter:on
	}

	private Bot.Builder bot() {
		//@formatter:off
		return new Bot.Builder()
			.connection(chatClient)
			.trigger("=")
			.user("BotUser", 1);
		//@formatter:on
	}

	private class ChatServerMock {
		private final Map<Integer, Consumer<MessagePostedEvent>> consumers = new HashMap<>();
		private final Map<Integer, IRoom> rooms = new HashMap<>();

		public void pushEvents(MessagePostedEvent... events) {
			for (MessagePostedEvent event : events) {
				consumers.get(event.getMessage().getRoomId()).accept(event);
			}
		}

		public IRoom createRoom(int roomId) throws IOException {
			return createRoom(roomId, true);
		}

		@SuppressWarnings("unchecked")
		public IRoom createRoom(int roomId, boolean canPost) throws IOException {
			IRoom room = mock(IRoom.class);
			when(room.getRoomId()).thenReturn(roomId);
			when(room.getFkey()).thenReturn("0123456789abcdef0123456789abcdef");
			when(room.canPost()).thenReturn(canPost);
			when(room.sendMessage(anyString(), any(SplitStrategy.class))).thenReturn(Arrays.asList(messageId++));
			doAnswer((invocation) -> {
				chatClient.joined.remove(roomId);
				return null;
			}).when(room).leave();

			doAnswer((invocations) -> {
				Consumer<MessagePostedEvent> consumer = (Consumer<MessagePostedEvent>) invocations.getArguments()[1];
				consumers.put(roomId, consumer);
				return null;
			}).when(room).addEventListener(eq(MessagePostedEvent.class), any(Consumer.class));

			rooms.put(roomId, room);

			return room;
		}
	}

	private class ChatClientMock implements IChatClient {
		private final ChatServerMock server;
		private final Map<Integer, IRoom> joined = new HashMap<>();

		public ChatClientMock(ChatServerMock server) {
			this.server = server;
		}

		@Override
		public void login(String email, String password) throws InvalidCredentialsException, IOException {
			if ("bad".equals(password)) {
				throw new InvalidCredentialsException();
			}
		}

		@Override
		public IRoom joinRoom(int roomId) throws RoomNotFoundException, IOException {
			if (roomId == 999) {
				throw new IOException();
			}

			IRoom room = server.rooms.get(roomId);
			if (room == null) {
				throw new RoomNotFoundException(roomId);
			}

			joined.put(roomId, room);
			return room;
		}

		@Override
		public List<? extends IRoom> getRooms() {
			return new ArrayList<>(joined.values());
		}

		@Override
		public IRoom getRoom(int roomId) {
			return joined.get(roomId);
		}

		@Override
		public boolean isInRoom(int roomId) {
			return joined.containsKey(roomId);
		}

		@Override
		public void close() throws IOException {
			//empty
		}
	}

	private void run(Bot bot, MessagePostedEvent... events) throws Exception {
		run(bot, true, events);
	}

	private void run(Bot bot, boolean quiet, MessagePostedEvent... events) throws Exception {
		Thread t = bot.connect(quiet);
		chatServer.pushEvents(events);
		bot.stop();
		t.join();
	}
}
