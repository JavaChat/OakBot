package oakbot.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.IChatClient;
import com.github.mangstadt.sochat4j.IRoom;
import com.github.mangstadt.sochat4j.InvalidCredentialsException;
import com.github.mangstadt.sochat4j.RoomNotFoundException;
import com.github.mangstadt.sochat4j.Site;
import com.github.mangstadt.sochat4j.SplitStrategy;
import com.github.mangstadt.sochat4j.UserInfo;
import com.github.mangstadt.sochat4j.event.MessagePostedEvent;
import com.github.mangstadt.sochat4j.util.Sleeper;

import oakbot.Database;
import oakbot.command.Command;
import oakbot.command.learn.LearnedCommand;
import oakbot.command.learn.LearnedCommandsDao;
import oakbot.filter.ChatResponseFilter;
import oakbot.listener.CommandListener;
import oakbot.listener.Listener;

/**
 * @author Michael Angstadt
 */
class BotTest {
	private long eventId;
	private long messageId;

	private ChatServerMock chatServer;
	private ChatClientMock chatClient;

	private boolean runAfter;

	@BeforeEach
	void before() {
		eventId = 1;
		messageId = 1;

		chatServer = new ChatServerMock();
		chatClient = spy(new ChatClientMock(chatServer));

		runAfter = true;

		Sleeper.startUnitTest();
	}

	@AfterEach
	void after() throws Exception {
		Sleeper.endUnitTest();

		if (!runAfter) {
			return;
		}

		verify(chatClient).close();
	}

	@Test
	void builder_no_connection() {
		runAfter = false;
		assertThrows(IllegalStateException.class, () -> new Bot.Builder().build());
	}

	@Test
	void connect_greeting_broadcast() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		var room1 = chatServer.createRoom(1);
		var room2 = chatServer.createRoom(2);

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1, 2)
			.greeting("Greetings.")
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runNonQuiet(bot);

		/*
		 * Verify.
		 */
		verify(room1).sendMessage("Greetings.", 0, SplitStrategy.NONE);
		verify(room2).sendMessage("Greetings.", 0, SplitStrategy.NONE);
	}

	@Test
	void connect_greeting_quiet() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		var room1 = chatServer.createRoom(1);
		var room2 = chatServer.createRoom(2);

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1, 2)
			.greeting("Greetings.")
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot);

		/*
		 * Verify.
		 */
		verify(room1, times(0)).sendMessage("Greetings.", 0, SplitStrategy.NONE);
		verify(room2, times(0)).sendMessage("Greetings.", 0, SplitStrategy.NONE);
	}

	@Test
	void connect_greeting_no_message() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		var room1 = chatServer.createRoom(1);
		var room2 = chatServer.createRoom(2);

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1, 2)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runNonQuiet(bot);

		/*
		 * Verify.
		 */
		verify(room1, times(0)).sendMessage("Greetings.", 0, SplitStrategy.NONE);
		verify(room2, times(0)).sendMessage("Greetings.", 0, SplitStrategy.NONE);
	}

	@Test
	void listener() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		var room1 = chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("no reply");
		var event2 = event("reply");

		/*
		 * Create the listener
		 */
		var listener = mock(Listener.class);
		when(listener.onMessage(same(event1.getMessage()), any(IBot.class))).thenReturn(ChatActions.doNothing());
		when(listener.onMessage(same(event2.getMessage()), any(IBot.class))).thenReturn(ChatActions.post("reply"));

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.listeners(listener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1, event2);

		/*
		 * Verify.
		 */
		verify(listener).onMessage(same(event1.getMessage()), same(bot));
		verify(listener).onMessage(same(event2.getMessage()), same(bot));
		verify(room1, times(1)).sendMessage(anyString(), anyLong(), any(SplitStrategy.class));
		verify(room1).sendMessage("reply", 0, SplitStrategy.NONE);
	}

	@Test
	void command() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		var room1 = chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("Ignore me");
		var event2 = event("=name command");
		var event3 = event("=alias command");
		var event4 = event("=name reply");

		/*
		 * Create the command.
		 */
		var command = mock(Command.class);
		when(command.name()).thenReturn("name");
		when(command.aliases()).thenReturn(List.of("alias"));
		var expectedChatCommand2 = new ChatCommand(event2.getMessage(), "name", "command");
		when(command.onMessage(eq(expectedChatCommand2), any(IBot.class))).thenReturn(ChatActions.doNothing());
		var expectedChatCommand3 = new ChatCommand(event3.getMessage(), "alias", "command");
		when(command.onMessage(eq(expectedChatCommand3), any(IBot.class))).thenReturn(ChatActions.doNothing());
		var expectedChatCommand4 = new ChatCommand(event4.getMessage(), "name", "reply");
		when(command.onMessage(eq(expectedChatCommand4), any(IBot.class))).thenReturn(ChatActions.post("reply"));
		var commandListener = new CommandListener(List.of(command), new LearnedCommandsDao());

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.listeners(commandListener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1, event2, event3, event4);

		/*
		 * Verify.
		 */
		verify(command).onMessage(eq(expectedChatCommand2), same(bot));
		verify(command).onMessage(eq(expectedChatCommand3), same(bot));
		verify(command).onMessage(eq(expectedChatCommand4), same(bot));
		verify(room1, times(1)).sendMessage(anyString(), anyLong(), any(SplitStrategy.class));
		verify(room1).sendMessage("reply", 0, SplitStrategy.NONE);
	}

	@Test
	void learned_command() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		var room1 = chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("=ignore");
		var event2 = event("=foo");

		/*
		 * Create the learned commands.
		 */
		var learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(new LearnedCommand.Builder().name("foo").output("bar").build());
		var commandListener = new CommandListener(Collections.<Command> emptyList(), learnedCommands);

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.listeners(commandListener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1, event2);

		/*
		 * Verify.
		 */
		verify(room1, times(1)).sendMessage(anyString(), anyLong(), any(SplitStrategy.class));
		verify(room1).sendMessage("bar", 0, SplitStrategy.NONE);
	}

	@Test
	void botler_relay_message() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("Direct message from Botler user, treat normally.", Bot.BOTLER_ID);
		var event2 = event("[<b><a href=\"https://discord.gg/PNMq3pBSUe\" rel=\"nofollow noopener noreferrer\">realmichael</a></b>] /fish", Bot.BOTLER_ID);
		var event3 = event("[<b><a href=\"https://discord.gg/PNMq3pBSUe\" rel=\"nofollow noopener noreferrer\">realmichael</a></b>]", Bot.BOTLER_ID);

		/*
		 * Define the expected ChatMessage objects that will be passed down the
		 * line.
		 */
		var message1 = event1.getMessage();
		var message2 = new ChatMessage.Builder(event2.getMessage()).username("realmichael").content("/fish").build();
		var message3 = new ChatMessage.Builder(event3.getMessage()).username("realmichael").content("").build();

		/*
		 * Create the listener.
		 */
		var listener = mock(Listener.class);
		when(listener.onMessage(any(ChatMessage.class), any(IBot.class))).thenReturn(ChatActions.doNothing());

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.listeners(listener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1, event2, event3);

		/*
		 * Verify.
		 */
		verify(listener).onMessage(same(message1), same(bot));
		verify(listener).onMessage(eq(message2), same(bot));
		verify(listener).onMessage(eq(message3), same(bot));
	}

	@Test
	void filter_disabled_by_default() throws Exception {
		filter(0);
	}

	@Test
	void filter_enabled_on_another_room() throws Exception {
		filter(1);
	}

	@Test
	void filter_enabled_on_correct_room() throws Exception {
		filter(2);
	}

	@Test
	void filter_enabled_globally() throws Exception {
		filter(3);
	}

	private void filter(int num) throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		var room1 = chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("=command");
		var event2 = event("=learned");

		/*
		 * Create the command.
		 */
		var command = mock(Command.class);
		when(command.name()).thenReturn("command");
		when(command.aliases()).thenReturn(List.of());
		when(command.onMessage(any(ChatCommand.class), any(IBot.class))).thenReturn(ChatActions.post("reply"));

		/*
		 * Create the learned commands.
		 */
		var learnedCommands = new LearnedCommandsDao();
		learnedCommands.add(new LearnedCommand.Builder().name("learned").output("reply").build());

		var commandListener = new CommandListener(List.of(command), learnedCommands);

		/*
		 * Create the listener.
		 */
		var listener = mock(Listener.class);
		when(listener.onMessage(eq(event1.getMessage()), any(IBot.class))).thenReturn(ChatActions.post("reply"));

		/*
		 * Create the filter.
		 */
		var filter = new ChatResponseFilter() {
			@Override
			public String filter(String message) {
				return message.toUpperCase();
			}
		};

		var expectedEnabled = false;
		switch (num) {
		case 0 -> {
			//do nothing
		}
		case 1 -> filter.setEnabled(2, true);
		case 2 -> {
			filter.setEnabled(1, true);
			expectedEnabled = true;
		}
		case 3 -> {
			filter.setGloballyEnabled(true);
			expectedEnabled = true;
		}
		default -> {
			fail("Undefined test case number.");
		}
		}

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.listeners(commandListener, listener)
			.greeting("reply")
			.responseFilters(filter)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runNonQuiet(bot, event1, event2);

		/*
		 * Verify.
		 */
		var expectedMessage = expectedEnabled ? "REPLY" : "reply";
		verify(room1, times(4)).sendMessage(anyString(), anyLong(), any(SplitStrategy.class));
		verify(room1, times(4)).sendMessage(expectedMessage, 0, SplitStrategy.NONE);
	}

	@Test
	void join_room() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		var room1 = chatServer.createRoom(1);
		var room2 = chatServer.createRoom(2);
		chatServer.createRoom(4, false);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("=join 2"); //success
		var event2 = event("=join 2"); //success
		var event3 = event("=join 3"); //404
		var event4 = event("=join 3"); //404
		var event5 = event("=join 4"); //can't post
		var event6 = event("=join 4"); //can't post
		var event7 = event("=join 999"); //IOException
		var event8 = event("=join 999"); //IOException

		/*
		 * Create the join command.
		 */
		var joinCommand = mock(Command.class);
		when(joinCommand.name()).thenReturn("join");
		when(joinCommand.aliases()).thenReturn(List.of());
		when(joinCommand.onMessage(any(ChatCommand.class), any(IBot.class))).then(invocation -> {
			var chatCommand = (ChatCommand) invocation.getArguments()[0];
			var roomId = Integer.parseInt(chatCommand.getContent());

			//@formatter:off
			return ChatActions.create(
				new JoinRoom(roomId)
				.onSuccess(() -> ChatActions.post("success"))
				.ifRoomDoesNotExist(() -> ChatActions.post("ifRoomDoesNotExist"))
				.ifLackingPermissionToPost(() -> ChatActions.post("ifLackingPermissionToPost"))
				.onError(e -> ChatActions.post("onError"))
			);
			//@formatter:on
		});

		var commandListener = new CommandListener(List.of(joinCommand), new LearnedCommandsDao());

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.greeting("Greetings.")
			.roomsHome(1)
			.listeners(commandListener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1, event2, event3, event4, event5, event6, event7, event8);

		/*
		 * Verify.
		 */
		verify(chatClient).joinRoom(2);
		verify(chatClient, times(2)).joinRoom(3);
		verify(chatClient, times(2)).joinRoom(4);
		verify(chatClient, times(2)).joinRoom(999);

		verify(room1, times(2)).sendMessage("success", 0, SplitStrategy.NONE);
		verify(room1, times(2)).sendMessage("ifRoomDoesNotExist", 0, SplitStrategy.NONE);
		verify(room1, times(2)).sendMessage("ifLackingPermissionToPost", 0, SplitStrategy.NONE);
		verify(room1, times(2)).sendMessage("onError", 0, SplitStrategy.NONE);

		verify(room2, times(1)).sendMessage(anyString(), anyLong(), any(SplitStrategy.class));
		verify(room2).sendMessage("Greetings.", 0, SplitStrategy.NONE);
	}

	@Test
	void leave_room() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);
		var room2 = chatServer.createRoom(2);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("=leave");

		/*
		 * Create the leave command.
		 */
		var command = mock(Command.class);
		when(command.name()).thenReturn("leave");
		when(command.aliases()).thenReturn(List.of());

		when(command.onMessage(any(ChatCommand.class), any(IBot.class))).thenReturn(ChatActions.create(new LeaveRoom(2)));

		var commandListener = new CommandListener(List.of(command), new LearnedCommandsDao());

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1, 2)
			.listeners(commandListener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1);

		/*
		 * Verify.
		 */
		verify(room2).leave();
		assertEquals(List.of(1), bot.getRooms());
	}

	@Test
	void shutdown() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("=shutdown");

		/*
		 * Create the shutdown command.
		 */
		var command = mock(Command.class);
		when(command.name()).thenReturn("shutdown");
		when(command.aliases()).thenReturn(List.of());
		when(command.onMessage(any(ChatCommand.class), any(IBot.class))).thenReturn(ChatActions.create(new Shutdown()));

		var commandListener = new CommandListener(List.of(command), new LearnedCommandsDao());

		var db = mock(Database.class);

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1, 2)
			.listeners(commandListener)
			.database(db)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		var t = bot.connect(true);
		chatServer.pushEvents(event1);

		/*
		 * The bot should terminate, so this method should only block until it
		 * processes the shutdown command.
		 */
		t.join(5000);

		/*
		 * Verify.
		 */
		assertFalse(t.isAlive());
		verify(db, atLeastOnce()).commit();
	}

	@Test
	void content_null() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event(null);

		/*
		 * Create the listener
		 */
		var listener = mock(Listener.class);

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.listeners(listener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1);

		/*
		 * Verify.
		 */
		verify(listener, times(0)).onMessage(same(event1.getMessage()), same(bot));
	}

	@Test
	void banned_user() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("Test", 2);
		var event2 = event("Test", 100);

		/*
		 * Create the listener
		 */
		var listener = mock(Listener.class);

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.bannedUsers(100)
			.listeners(listener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1, event2);

		/*
		 * Verify.
		 */
		verify(listener).onMessage(same(event1.getMessage()), same(bot));
		verify(listener, times(0)).onMessage(same(event2.getMessage()), same(bot));
	}

	@Test
	void allowed_user() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("Test2", 2);
		var event2 = event("Test100", 100);

		/*
		 * Create the listener
		 */
		var listener = mock(Listener.class);

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.allowedUsers(2)
			.listeners(listener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1, event2);

		/*
		 * Verify.
		 */
		verify(listener).onMessage(same(event1.getMessage()), same(bot));
		verify(listener, times(0)).onMessage(same(event2.getMessage()), same(bot));
	}

	@Test
	void ignoreMessageSuffix() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("Ignore this ~");

		/*
		 * Create the listener
		 */
		var listener = mock(Listener.class);

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.ignoreMessageSuffix("~")
			.listeners(listener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1);

		/*
		 * Verify.
		 */
		verify(listener, times(0)).onMessage(same(event1.getMessage()), same(bot));
	}

	@Test
	void onebox() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		var room1 = chatServer.createRoom(1);
		when(room1.sendMessage("http://en.wikipedia.org/wiki/Java", 0, SplitStrategy.NONE)).thenReturn(List.of(100L));

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("trigger the listener");

		var now = LocalDateTime.now();
		//@formatter:off
		var event2 = new MessagePostedEvent.Builder()
			.eventId(eventId++)
			.timestamp(now)
			.message(new ChatMessage.Builder()
				.content("<div class=\"onebox ob-wikipedia\"><img class=\"ob-wikipedia-image\" src=\"//upload.wikimedia.org/wikipedia/commons/thumb/4/43/Java_Topography.png/220px-Java_Topography.png\" /><div class=\"ob-wikipedia-title\"><img src=\"//en.wikipedia.org/static/favicon/wikipedia.ico\" width=\"24\" height=\"24\" class=\"ob-wikipedia-favicon\"/> <a rel=\"nofollow noopener noreferrer\" href=\"http://en.wikipedia.org/wiki/Java\">Java</a></div><div class=\"ob-wikipedia-text\">Java is an island of Indonesia. With a population of over 141 million (the island itself) or 145 million (the administrative region), Java is home to 56.7 percent of the Indonesian population and is the most populous island on Earth. The Indonesian capital city, Jakarta, is located on western Java. Much of Indonesian history took place on Java. It was the center of powerful Hindu-Buddhist empires, the Islamic sultanates, and the core of the colonial Dutch East Indies. Java was also the center of the Indonesian struggle for independence during the...</div></div>")
				.id(100)
				.timestamp(now)
				.userId(1)
				.username("BotUser")
				.roomId(1)
			.build())
		.build();
		//@formatter:on

		/*
		 * Create the listener
		 */
		var listener = mock(Listener.class);
		when(listener.onMessage(same(event1.getMessage()), any(IBot.class))).thenReturn(ChatActions.post("http://en.wikipedia.org/wiki/Java"));

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.hideOneboxesAfter(Duration.ofSeconds(1))
			.listeners(listener)
		.build();
		//@formatter:on

		/*
		 * Stop the bot when the bot sends the edit request.
		 */
		doAnswer(invocation -> {
			bot.stop();
			return null;
		}).when(room1).editMessage(100, 0, "> http://en.wikipedia.org/wiki/Java");

		/*
		 * Run the bot.
		 */
		var start = System.currentTimeMillis();
		var t = bot.connect(true);
		chatServer.pushEvents(event1, event2);
		t.join();
		var stop = System.currentTimeMillis();

		/*
		 * Verify.
		 */
		verify(room1).sendMessage("http://en.wikipedia.org/wiki/Java", 0, SplitStrategy.NONE);
		verify(room1).editMessage(100, 0, "> http://en.wikipedia.org/wiki/Java");
		assertTrue(stop - start >= 1000);
	}

	@Test
	void unknown_command() throws Exception {
		/*
		 * Setup the chat rooms.
		 */
		var room1 = chatServer.createRoom(1);

		/*
		 * Define the chat room events to push.
		 */
		var event1 = event("=foobar");
		var event2 = event("=foobar");

		/*
		 * Create the handler.
		 */
		@SuppressWarnings("unchecked")
		BiFunction<ChatCommand, IBot, ChatActions> handler = mock(BiFunction.class);
		var expectedChatCommand1 = new ChatCommand(event1.getMessage(), "foobar", "");
		when(handler.apply(eq(expectedChatCommand1), any(IBot.class))).thenReturn(null);
		var expectedChatCommand2 = new ChatCommand(event2.getMessage(), "foobar", "");
		when(handler.apply(eq(expectedChatCommand2), any(IBot.class))).thenReturn(ChatActions.post("reply"));

		var commandListener = new CommandListener(Collections.emptyList(), new LearnedCommandsDao(), handler);

		/*
		 * Create the bot.
		 */
		//@formatter:off
		var bot = bot()
			.roomsHome(1)
			.listeners(commandListener)
		.build();
		//@formatter:on

		/*
		 * Run the bot.
		 */
		runQuiet(bot, event1, event2);

		/*
		 * Verify.
		 */
		verify(handler, times(2)).apply(any(ChatCommand.class), same(bot));
		verify(room1, times(1)).sendMessage(anyString(), anyLong(), any(SplitStrategy.class));
		verify(room1).sendMessage("reply", 0, SplitStrategy.NONE);
	}

	private MessagePostedEvent event(String content) {
		return event(content, 2);
	}

	private MessagePostedEvent event(String content, int userId) {
		var now = LocalDateTime.now();

		//@formatter:off
		return new MessagePostedEvent.Builder()
			.eventId(eventId++)
			.timestamp(now)
			.message(new ChatMessage.Builder()
				.content(content)
				.id(messageId++)
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
			for (var event : events) {
				var roomId = event.getMessage().roomId();
				consumers.get(roomId).accept(event);
			}
		}

		public IRoom createRoom(int roomId) throws IOException {
			return createRoom(roomId, true);
		}

		@SuppressWarnings("unchecked")
		public IRoom createRoom(int roomId, boolean canPost) throws IOException {
			var room = mock(IRoom.class);
			when(room.getRoomId()).thenReturn(roomId);
			when(room.getFkey()).thenReturn("0123456789abcdef0123456789abcdef");
			when(room.canPost()).thenReturn(canPost);
			when(room.sendMessage(anyString(), anyLong(), any(SplitStrategy.class))).thenReturn(List.of(messageId++));
			doAnswer(invocation -> {
				chatClient.joined.remove(roomId);
				return null;
			}).when(room).leave();

			doAnswer(invocations -> {
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
		public String getUsername() {
			return null;
		}

		@Override
		public Integer getUserId() {
			return null;
		}

		@Override
		public IRoom joinRoom(int roomId) throws RoomNotFoundException, IOException {
			if (roomId == 999) {
				throw new IOException();
			}

			var room = server.rooms.get(roomId);
			if (room == null) {
				throw new RoomNotFoundException(roomId);
			}

			joined.put(roomId, room);
			return room;
		}

		@Override
		public List<IRoom> getRooms() {
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

		@Override
		public String getMessageContent(long messageId) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getOriginalMessageContent(long messageId) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Site getSite() {
			return null;
		}

		@Override
		public String uploadImage(String url) throws IOException {
			throw new IOException();
		}

		@Override
		public String uploadImage(byte[] data) throws IOException {
			throw new IOException();
		}

		@Override
		public List<UserInfo> getUserInfo(int roomId, List<Integer> userIds) throws IOException {
			return List.of();
		}
	}

	private void runQuiet(Bot bot, MessagePostedEvent... events) throws Exception {
		run(bot, true, events);
	}

	private void runNonQuiet(Bot bot, MessagePostedEvent... events) throws Exception {
		run(bot, false, events);
	}

	private void run(Bot bot, boolean quiet, MessagePostedEvent... events) throws Exception {
		var t = bot.connect(quiet);
		chatServer.pushEvents(events);
		bot.finish();
		t.join();
	}
}
