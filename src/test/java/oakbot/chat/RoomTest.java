package oakbot.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.BeforeClass;
import org.junit.Test;

import oakbot.chat.event.Event;
import oakbot.chat.event.MessageDeletedEvent;
import oakbot.chat.event.MessageEditedEvent;
import oakbot.chat.event.MessagePostedEvent;
import oakbot.chat.event.MessageStarredEvent;
import oakbot.chat.event.MessagesMovedEvent;
import oakbot.chat.event.UserEnteredEvent;
import oakbot.chat.event.UserLeftEvent;

/**
 * @author Michael Angstadt
 */
@SuppressWarnings("resource")
public class RoomTest {
	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	/**
	 * Anything that is not JSON should be silently ignored.
	 */
	@Test
	public void webSocket_ignore_bad_data() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		chatClient.joinRoom(1);

		wsRoom1.send("not JSON");
		wsRoom1.send("{\"r20\": {} }");
		wsRoom1.send("{\"r1\": {} }");
		wsRoom1.send("{\"r1\": { \"e\": {} } }");
		wsRoom1.send("{\"r1\": { \"e\": [] } }");
		wsRoom1.send("{\"r1\": { \"e\": [ {} ] } }");
		wsRoom1.send("{\"r1\": { \"e\": [ {\"event_type\": \"invalid\"} ] } }");
		wsRoom1.send("{\"r1\": { \"e\": [ {\"event_type\": 9001} ] } }");

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_MessagePostedEvent() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(MessagePostedEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.newMessage().id(1).timestamp(1417041460).content("one").user(50, "User").messageId(20157245).done()
		.build());
		//@formatter:on

		assertEquals(1, events.size());

		MessagePostedEvent event = (MessagePostedEvent) events.get(0);
		assertEquals(1, event.getEventId());
		assertEquals(timestamp(1417041460), event.getTimestamp());
		assertEquals("one", event.getMessage().getContent().getContent());
		assertEquals(0, event.getMessage().getEdits());
		assertEquals(0, event.getMessage().getMentionedUserId());
		assertEquals(20157245, event.getMessage().getMessageId());
		assertEquals(0, event.getMessage().getParentMessageId());
		assertEquals(1, event.getMessage().getRoomId());
		assertEquals("Sandbox", event.getMessage().getRoomName());
		assertEquals(0, event.getMessage().getStars());
		assertEquals(timestamp(1417041460), event.getMessage().getTimestamp());
		assertEquals(50, event.getMessage().getUserId());
		assertEquals("User", event.getMessage().getUsername());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_MessagePostedEvent_reply() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(MessagePostedEvent.class, (event) -> {
			events.add(event);
		});
		room.addEventListener(MessageEditedEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.reply().id(1).timestamp(1417041460).content("@Bob Howdy.").user(50, "User").targetUser(100).messageId(20157245).parentId(20157230).done()
			.newMessage().id(2).timestamp(1417041460).content("@Bob Howdy.").user(50, "User").messageId(20157245).parentId(20157230).done()
			.reply().id(3).timestamp(1417041470).edits(1).content("@Greg Sup.").user(50, "User").targetUser(150).messageId(20157240).parentId(20157220).done()
			.messageEdited().id(4).timestamp(1417041470).edits(1).content("@Greg Sup.").user(50, "User").messageId(20157240).parentId(20157220).done()
		.build());
		//@formatter:on

		assertEquals(2, events.size());

		MessagePostedEvent postedEvent = (MessagePostedEvent) events.get(0);
		assertEquals(1, postedEvent.getEventId());
		assertEquals(timestamp(1417041460), postedEvent.getTimestamp());
		assertEquals("@Bob Howdy.", postedEvent.getMessage().getContent().getContent());
		assertEquals(0, postedEvent.getMessage().getEdits());
		assertEquals(100, postedEvent.getMessage().getMentionedUserId());
		assertEquals(20157245, postedEvent.getMessage().getMessageId());
		assertEquals(20157230, postedEvent.getMessage().getParentMessageId());
		assertEquals(1, postedEvent.getMessage().getRoomId());
		assertEquals("Sandbox", postedEvent.getMessage().getRoomName());
		assertEquals(0, postedEvent.getMessage().getStars());
		assertEquals(timestamp(1417041460), postedEvent.getMessage().getTimestamp());
		assertEquals(50, postedEvent.getMessage().getUserId());
		assertEquals("User", postedEvent.getMessage().getUsername());

		MessageEditedEvent editedEvent = (MessageEditedEvent) events.get(1);
		assertEquals(3, editedEvent.getEventId());
		assertEquals(timestamp(1417041470), editedEvent.getTimestamp());
		assertEquals("@Greg Sup.", editedEvent.getMessage().getContent().getContent());
		assertEquals(1, editedEvent.getMessage().getEdits());
		assertEquals(150, editedEvent.getMessage().getMentionedUserId());
		assertEquals(20157240, editedEvent.getMessage().getMessageId());
		assertEquals(20157220, editedEvent.getMessage().getParentMessageId());
		assertEquals(1, editedEvent.getMessage().getRoomId());
		assertEquals("Sandbox", editedEvent.getMessage().getRoomName());
		assertEquals(0, editedEvent.getMessage().getStars());
		assertEquals(timestamp(1417041470), editedEvent.getMessage().getTimestamp());
		assertEquals(50, editedEvent.getMessage().getUserId());
		assertEquals("User", editedEvent.getMessage().getUsername());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_MessagePostedEvent_mention() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(MessagePostedEvent.class, (event) -> {
			events.add(event);
		});
		room.addEventListener(MessageEditedEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.mention().id(1).timestamp(1417041460).content("@Bob Howdy.").user(50, "User").targetUser(100).messageId(20157245).done()
			.newMessage().id(2).timestamp(1417041460).content("@Bob Howdy.").user(50, "User").messageId(20157245).done()
			.mention().id(3).timestamp(1417041470).edits(1).content("@Greg Sup.").user(50, "User").targetUser(150).messageId(20157240).done()
			.messageEdited().id(4).timestamp(1417041470).edits(1).content("@Greg Sup.").user(50, "User").messageId(20157240).done()
		.build());
		//@formatter:on

		assertEquals(2, events.size());

		MessagePostedEvent postedEvent = (MessagePostedEvent) events.get(0);
		assertEquals(1, postedEvent.getEventId());
		assertEquals(timestamp(1417041460), postedEvent.getTimestamp());
		assertEquals("@Bob Howdy.", postedEvent.getMessage().getContent().getContent());
		assertEquals(0, postedEvent.getMessage().getEdits());
		assertEquals(100, postedEvent.getMessage().getMentionedUserId());
		assertEquals(20157245, postedEvent.getMessage().getMessageId());
		assertEquals(0, postedEvent.getMessage().getParentMessageId());
		assertEquals(1, postedEvent.getMessage().getRoomId());
		assertEquals("Sandbox", postedEvent.getMessage().getRoomName());
		assertEquals(0, postedEvent.getMessage().getStars());
		assertEquals(timestamp(1417041460), postedEvent.getMessage().getTimestamp());
		assertEquals(50, postedEvent.getMessage().getUserId());
		assertEquals("User", postedEvent.getMessage().getUsername());

		MessageEditedEvent editedEvent = (MessageEditedEvent) events.get(1);
		assertEquals(3, editedEvent.getEventId());
		assertEquals(timestamp(1417041470), editedEvent.getTimestamp());
		assertEquals("@Greg Sup.", editedEvent.getMessage().getContent().getContent());
		assertEquals(1, editedEvent.getMessage().getEdits());
		assertEquals(150, editedEvent.getMessage().getMentionedUserId());
		assertEquals(20157240, editedEvent.getMessage().getMessageId());
		assertEquals(0, editedEvent.getMessage().getParentMessageId());
		assertEquals(1, editedEvent.getMessage().getRoomId());
		assertEquals("Sandbox", editedEvent.getMessage().getRoomName());
		assertEquals(0, editedEvent.getMessage().getStars());
		assertEquals(timestamp(1417041470), editedEvent.getMessage().getTimestamp());
		assertEquals(50, editedEvent.getMessage().getUserId());
		assertEquals("User", editedEvent.getMessage().getUsername());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_MessageEditedEvent() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(MessageEditedEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.messageEdited().id(1).timestamp(1417041460).content("one").user(50, "User").messageId(20157245).edits(1).done()
		.build());
		//@formatter:on

		assertEquals(1, events.size());

		MessageEditedEvent event = (MessageEditedEvent) events.get(0);
		assertEquals(1, event.getEventId());
		assertEquals(timestamp(1417041460), event.getTimestamp());
		assertEquals("one", event.getMessage().getContent().getContent());
		assertEquals(1, event.getMessage().getEdits());
		assertEquals(0, event.getMessage().getMentionedUserId());
		assertEquals(20157245, event.getMessage().getMessageId());
		assertEquals(0, event.getMessage().getParentMessageId());
		assertEquals(1, event.getMessage().getRoomId());
		assertEquals("Sandbox", event.getMessage().getRoomName());
		assertEquals(0, event.getMessage().getStars());
		assertEquals(timestamp(1417041460), event.getMessage().getTimestamp());
		assertEquals(50, event.getMessage().getUserId());
		assertEquals("User", event.getMessage().getUsername());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_MessageStarredEvent() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(MessageStarredEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.messageStarred().id(1).timestamp(1417041460).content("one").messageId(20157245).stars(1).done()
		.build());
		//@formatter:on

		assertEquals(1, events.size());

		MessageStarredEvent event = (MessageStarredEvent) events.get(0);
		assertEquals(1, event.getEventId());
		assertEquals(timestamp(1417041460), event.getTimestamp());
		assertEquals("one", event.getMessage().getContent().getContent());
		assertEquals(0, event.getMessage().getEdits());
		assertEquals(0, event.getMessage().getMentionedUserId());
		assertEquals(20157245, event.getMessage().getMessageId());
		assertEquals(0, event.getMessage().getParentMessageId());
		assertEquals(1, event.getMessage().getRoomId());
		assertEquals("Sandbox", event.getMessage().getRoomName());
		assertEquals(1, event.getMessage().getStars());
		assertEquals(timestamp(1417041460), event.getMessage().getTimestamp());
		assertEquals(0, event.getMessage().getUserId());
		assertNull(event.getMessage().getUsername());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_MessageDeletedEvent() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(MessageDeletedEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.messageDeleted().id(1).timestamp(1417041460).user(50, "User").messageId(20157245).done()
		.build());
		//@formatter:on

		assertEquals(1, events.size());

		MessageDeletedEvent event = (MessageDeletedEvent) events.get(0);
		assertEquals(1, event.getEventId());
		assertEquals(timestamp(1417041460), event.getTimestamp());
		assertNull(event.getMessage().getContent());
		assertEquals(0, event.getMessage().getEdits());
		assertEquals(0, event.getMessage().getMentionedUserId());
		assertEquals(20157245, event.getMessage().getMessageId());
		assertEquals(0, event.getMessage().getParentMessageId());
		assertEquals(1, event.getMessage().getRoomId());
		assertEquals("Sandbox", event.getMessage().getRoomName());
		assertEquals(0, event.getMessage().getStars());
		assertEquals(timestamp(1417041460), event.getMessage().getTimestamp());
		assertEquals(50, event.getMessage().getUserId());
		assertEquals("User", event.getMessage().getUsername());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_MessagesMovedEvent_out() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(MessagesMovedEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.movedOut().id(1).timestamp(1417041460).content("one").user(50, "User").messageId(20157245).moved().done()
			.movedOut().id(2).timestamp(1417041470).content("two").user(50, "User").messageId(20157246).moved().done()
			.movedOut().id(3).timestamp(1417041480).content("three").user(50, "User").messageId(20157247).moved().done()
			.newMessage().id(4).timestamp(1417041490).content("&rarr; <i><a href=\"http://chat.stackoverflow.com/transcript/message/38258010#38258010\">3 messages</a> moved to <a href=\"http://chat.stackoverflow.com/rooms/48058/trash\">Trash</a></i>").user(100, "RoomOwner").messageId(20157248).done()
		.build());
		//@formatter:on

		assertEquals(1, events.size());

		MessagesMovedEvent event = (MessagesMovedEvent) events.get(0);
		assertEquals(4, event.getEventId());
		assertEquals(timestamp(1417041490), event.getTimestamp());
		assertEquals(1, event.getSourceRoomId());
		assertEquals("Sandbox", event.getSourceRoomName());
		assertEquals(48058, event.getDestRoomId());
		assertEquals("Trash", event.getDestRoomName());
		assertEquals(100, event.getMoverUserId());
		assertEquals("RoomOwner", event.getMoverUsername());

		Iterator<ChatMessage> it = event.getMessages().iterator();

		ChatMessage message = it.next();
		assertEquals("one", message.getContent().getContent());
		assertEquals(0, message.getEdits());
		assertEquals(0, message.getMentionedUserId());
		assertEquals(20157245, message.getMessageId());
		assertEquals(0, message.getParentMessageId());
		assertEquals(1, message.getRoomId());
		assertEquals("Sandbox", message.getRoomName());
		assertEquals(0, message.getStars());
		assertEquals(timestamp(1417041460), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User", message.getUsername());

		message = it.next();
		assertEquals("two", message.getContent().getContent());
		assertEquals(0, message.getEdits());
		assertEquals(0, message.getMentionedUserId());
		assertEquals(20157246, message.getMessageId());
		assertEquals(0, message.getParentMessageId());
		assertEquals(1, message.getRoomId());
		assertEquals("Sandbox", message.getRoomName());
		assertEquals(0, message.getStars());
		assertEquals(timestamp(1417041470), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User", message.getUsername());

		message = it.next();
		assertEquals("three", message.getContent().getContent());
		assertEquals(0, message.getEdits());
		assertEquals(0, message.getMentionedUserId());
		assertEquals(20157247, message.getMessageId());
		assertEquals(0, message.getParentMessageId());
		assertEquals(1, message.getRoomId());
		assertEquals("Sandbox", message.getRoomName());
		assertEquals(0, message.getStars());
		assertEquals(timestamp(1417041480), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User", message.getUsername());

		assertFalse(it.hasNext());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_MessagesMovedEvent_in() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(MessagesMovedEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.movedIn().id(1).timestamp(1417041460).content("one").user(50, "User").messageId(20157245).moved().done()
			.movedIn().id(2).timestamp(1417041470).content("two").user(50, "User").messageId(20157246).moved().done()
			.movedIn().id(3).timestamp(1417041480).content("three").user(50, "User").messageId(20157247).moved().done()
			.newMessage().id(4).timestamp(1417041490).content("&larr; <i>3 messages moved from <a href=\"http://chat.stackoverflow.com/rooms/139/jaba\">Jaba</a></i>").user(100, "RoomOwner").messageId(20157248).done()
		.build());
		//@formatter:on

		assertEquals(1, events.size());

		MessagesMovedEvent event = (MessagesMovedEvent) events.get(0);
		assertEquals(4, event.getEventId());
		assertEquals(timestamp(1417041490), event.getTimestamp());
		assertEquals(139, event.getSourceRoomId());
		assertEquals("Jaba", event.getSourceRoomName());
		assertEquals(1, event.getDestRoomId());
		assertEquals("Sandbox", event.getDestRoomName());
		assertEquals(100, event.getMoverUserId());
		assertEquals("RoomOwner", event.getMoverUsername());

		Iterator<ChatMessage> it = event.getMessages().iterator();

		ChatMessage message = it.next();
		assertEquals("one", message.getContent().getContent());
		assertEquals(0, message.getEdits());
		assertEquals(0, message.getMentionedUserId());
		assertEquals(20157245, message.getMessageId());
		assertEquals(0, message.getParentMessageId());
		assertEquals(1, message.getRoomId());
		assertEquals("Sandbox", message.getRoomName());
		assertEquals(0, message.getStars());
		assertEquals(timestamp(1417041460), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User", message.getUsername());

		message = it.next();
		assertEquals("two", message.getContent().getContent());
		assertEquals(0, message.getEdits());
		assertEquals(0, message.getMentionedUserId());
		assertEquals(20157246, message.getMessageId());
		assertEquals(0, message.getParentMessageId());
		assertEquals(1, message.getRoomId());
		assertEquals("Sandbox", message.getRoomName());
		assertEquals(0, message.getStars());
		assertEquals(timestamp(1417041470), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User", message.getUsername());

		message = it.next();
		assertEquals("three", message.getContent().getContent());
		assertEquals(0, message.getEdits());
		assertEquals(0, message.getMentionedUserId());
		assertEquals(20157247, message.getMessageId());
		assertEquals(0, message.getParentMessageId());
		assertEquals(1, message.getRoomId());
		assertEquals("Sandbox", message.getRoomName());
		assertEquals(0, message.getStars());
		assertEquals(timestamp(1417041480), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User", message.getUsername());

		assertFalse(it.hasNext());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_UserEnteredEvent() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(UserEnteredEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.userEntered().id(1).timestamp(1417041460).user(50, "User").done()
		.build());
		//@formatter:on

		assertEquals(1, events.size());

		UserEnteredEvent event = (UserEnteredEvent) events.get(0);
		assertEquals(1, event.getEventId());
		assertEquals(1, event.getRoomId());
		assertEquals("Sandbox", event.getRoomName());
		assertEquals(timestamp(1417041460), event.getTimestamp());
		assertEquals(50, event.getUserId());
		assertEquals("User", event.getUsername());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_UserLeftEvent() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(UserLeftEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.userLeft().id(1).timestamp(1417041460).user(50, "User").done()
		.build());
		//@formatter:on

		assertEquals(1, events.size());

		UserLeftEvent event = (UserLeftEvent) events.get(0);
		assertEquals(1, event.getEventId());
		assertEquals(1, event.getRoomId());
		assertEquals("Sandbox", event.getRoomName());
		assertEquals(timestamp(1417041460), event.getTimestamp());
		assertEquals(50, event.getUserId());
		assertEquals("User", event.getUsername());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_ignore_events_from_other_rooms() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener(MessagePostedEvent.class, (event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.newMessage().id(1).timestamp(1417041460).content("one").user(50, "User").messageId(20157245).done()
		.room(139, "Jaba")
			.newMessage().id(2).timestamp(1417041460).content("two").user(50, "User").messageId(20157245).done()
		.build());
		//@formatter:on

		assertEquals(1, events.size());

		MessagePostedEvent event = (MessagePostedEvent) events.get(0);
		assertEquals(1, event.getEventId());
		assertEquals(timestamp(1417041460), event.getTimestamp());
		assertEquals("one", event.getMessage().getContent().getContent());
		assertEquals(0, event.getMessage().getEdits());
		assertEquals(0, event.getMessage().getMentionedUserId());
		assertEquals(20157245, event.getMessage().getMessageId());
		assertEquals(0, event.getMessage().getParentMessageId());
		assertEquals(1, event.getMessage().getRoomId());
		assertEquals("Sandbox", event.getMessage().getRoomName());
		assertEquals(0, event.getMessage().getStars());
		assertEquals(timestamp(1417041460), event.getMessage().getTimestamp());
		assertEquals(50, event.getMessage().getUserId());
		assertEquals("User", event.getMessage().getUsername());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void webSocket_listen_for_all_events() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
		.build();
		//@formatter:on

		WebSocketContainer container = mock(WebSocketContainer.class);
		MockWebSocketServer wsRoom1 = new MockWebSocketServer(container, "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417005460");

		ChatClient chatClient = new ChatClient(httpClient, container);
		Room room = chatClient.joinRoom(1);

		List<Event> events = new ArrayList<>();
		room.addEventListener((event) -> {
			events.add(event);
		});

		//@formatter:off
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.userEntered().id(1).timestamp(1417041460).user(50, "User").done()
			.newMessage().id(2).timestamp(1417041470).content("<i>meow</i>").user(50, "User").messageId(20157245).done()
		.build());
		
		wsRoom1.send(ResponseSamples.webSocket()
		.room(1, "Sandbox")
			.userLeft().id(3).timestamp(1417041480).user(50, "User").done()
		.build());
		//@formatter:on

		assertEquals(3, events.size());
		Iterator<Event> it = events.iterator();
		{
			UserEnteredEvent event = (UserEnteredEvent) it.next();
			assertEquals(1, event.getEventId());
			assertEquals(1, event.getRoomId());
			assertEquals("Sandbox", event.getRoomName());
			assertEquals(timestamp(1417041460), event.getTimestamp());
			assertEquals(50, event.getUserId());
			assertEquals("User", event.getUsername());
		}

		{
			MessagePostedEvent event = (MessagePostedEvent) it.next();
			assertEquals(2, event.getEventId());
			assertEquals(timestamp(1417041470), event.getTimestamp());
			assertEquals("<i>meow</i>", event.getMessage().getContent().getContent());
			assertEquals(0, event.getMessage().getEdits());
			assertEquals(0, event.getMessage().getMentionedUserId());
			assertEquals(20157245, event.getMessage().getMessageId());
			assertEquals(0, event.getMessage().getParentMessageId());
			assertEquals(1, event.getMessage().getRoomId());
			assertEquals("Sandbox", event.getMessage().getRoomName());
			assertEquals(0, event.getMessage().getStars());
			assertEquals(timestamp(1417041470), event.getMessage().getTimestamp());
			assertEquals(50, event.getMessage().getUserId());
			assertEquals("User", event.getMessage().getUsername());
		}

		{
			UserLeftEvent event = (UserLeftEvent) it.next();
			assertEquals(3, event.getEventId());
			assertEquals(1, event.getRoomId());
			assertEquals("Sandbox", event.getRoomName());
			assertEquals(timestamp(1417041480), event.getTimestamp());
			assertEquals(50, event.getUserId());
			assertEquals("User", event.getUsername());
		}

		assertFalse(it.hasNext());

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void getMessages() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)

			.request("POST", "https://chat.stackoverflow.com/chats/1/events",
				"mode", "messages",
				"msgCount", "3",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.events()
				.event(1417041460, "one", 50, "User", 1, 20157245)
				.event(1417041470, "two", 50, "User", 1, 20157246)
				.event(1417041480, "three", 50, "User", 1, 20157247)
			.build())
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);
		List<ChatMessage> messages = room.getMessages(3);

		Iterator<ChatMessage> it = messages.iterator();

		ChatMessage message = it.next();
		assertEquals("one", message.getContent().getContent());
		assertEquals(0, message.getEdits());
		assertEquals(0, message.getMentionedUserId());
		assertEquals(20157245, message.getMessageId());
		assertEquals(0, message.getParentMessageId());
		assertEquals(1, message.getRoomId());
		assertNull(message.getRoomName());
		assertEquals(0, message.getStars());
		assertEquals(timestamp(1417041460), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User", message.getUsername());

		message = it.next();
		assertEquals("two", message.getContent().getContent());
		assertEquals(0, message.getEdits());
		assertEquals(0, message.getMentionedUserId());
		assertEquals(20157246, message.getMessageId());
		assertEquals(0, message.getParentMessageId());
		assertEquals(1, message.getRoomId());
		assertNull(message.getRoomName());
		assertEquals(0, message.getStars());
		assertEquals(timestamp(1417041470), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User", message.getUsername());

		message = it.next();
		assertEquals("three", message.getContent().getContent());
		assertEquals(0, message.getEdits());
		assertEquals(0, message.getMentionedUserId());
		assertEquals(20157247, message.getMessageId());
		assertEquals(0, message.getParentMessageId());
		assertEquals(1, message.getRoomId());
		assertNull(message.getRoomName());
		assertEquals(0, message.getStars());
		assertEquals(timestamp(1417041480), message.getTimestamp());
		assertEquals(50, message.getUserId());
		assertEquals("User", message.getUsername());

		assertFalse(it.hasNext());

		verifyHttpClient(httpClient, 4);
	}

	@Test
	public void getMessages_bad_responses() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)

			.request("POST", "https://chat.stackoverflow.com/chats/1/events",
				"mode", "messages",
				"msgCount", "3",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(404, "")
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/events",
				"mode", "messages",
				"msgCount", "3",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "not JSON")
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/events",
				"mode", "messages",
				"msgCount", "3",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "{}")
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/events",
				"mode", "messages",
				"msgCount", "3",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "{\"events\":{}}")
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/events",
				"mode", "messages",
				"msgCount", "3",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "{\"events\":[]}")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		try {
			room.getMessages(3);
			fail();
		} catch (IOException e) {
			//expected
		}

		try {
			room.getMessages(3);
			fail();
		} catch (IOException e) {
			//expected
		}

		assertEquals(Arrays.asList(), room.getMessages(3));
		assertEquals(Arrays.asList(), room.getMessages(3));
		assertEquals(Arrays.asList(), room.getMessages(3));

		verifyHttpClient(httpClient, 8);
	}

	@Test
	public void sendMessage() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)

			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "one",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.newMessage(1))
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);
		assertEquals(1, room.sendMessage("one"));

		verifyHttpClient(httpClient, 4);
	}

	@Test
	public void sendMessage_split_strategy() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "Java is an island of Indonesia. With a population of over 141 million (the island itself) or 145 million (the administrative region), Java is home to 56.7 percent of the Indonesian population and is the most populous island on Earth. The Indonesian capital city, Jakarta, is located on western Java. Much of Indonesian history took place on Java. It was the center of powerful Hindu-Buddhist empires, the Islamic sultanates, and the core of the colonial Dutch East Indies. Java was also the ...",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.newMessage(1))
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "center of the Indonesian struggle for independence during the 1930s and 1940s. Java dominates Indonesia politically, economically and culturally.",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.newMessage(2))
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "Java is an island of Indonesia. With a population of over 141 million (the island itself) or 145 million (the administrative region), Java is home to 56.7 percent of the Indonesian population and is the most populous island on Earth.\nThe Indonesian capital city, Jakarta, is located on western Java. Much of Indonesian history took place on Java. It was the center of powerful Hindu-Buddhist empires, the Islamic sultanates, and the core of the colonial Dutch East Indies. Java was also the center of the Indonesian struggle for independence during the 1930s and 1940s. Java dominates Indonesia politically, economically and culturally.",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.newMessage(3))
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);
		Session session = mock(Session.class);
		doReturn(session).when(ws).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417023460")));

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		//split up into 2 posts
		assertEquals(Arrays.asList(1L, 2L), room.sendMessage("Java is an island of Indonesia. With a population of over 141 million (the island itself) or 145 million (the administrative region), Java is home to 56.7 percent of the Indonesian population and is the most populous island on Earth. The Indonesian capital city, Jakarta, is located on western Java. Much of Indonesian history took place on Java. It was the center of powerful Hindu-Buddhist empires, the Islamic sultanates, and the core of the colonial Dutch East Indies. Java was also the center of the Indonesian struggle for independence during the 1930s and 1940s. Java dominates Indonesia politically, economically and culturally.", SplitStrategy.WORD));

		//do not split because it has a newline
		assertEquals(Arrays.asList(3L), room.sendMessage("Java is an island of Indonesia. With a population of over 141 million (the island itself) or 145 million (the administrative region), Java is home to 56.7 percent of the Indonesian population and is the most populous island on Earth.\nThe Indonesian capital city, Jakarta, is located on western Java. Much of Indonesian history took place on Java. It was the center of powerful Hindu-Buddhist empires, the Islamic sultanates, and the core of the colonial Dutch East Indies. Java was also the center of the Indonesian struggle for independence during the 1930s and 1940s. Java dominates Indonesia politically, economically and culturally.", SplitStrategy.WORD));

		verifyHttpClient(httpClient, 6);
	}

	@Test
	public void sendMessage_posting_too_fast() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "one",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(409, "You can perform this action again in 2 seconds")
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "one",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.newMessage(1))
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);
		Session session = mock(Session.class);
		doReturn(session).when(ws).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417023460")));

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room1 = chatClient.joinRoom(1);

		long start = System.currentTimeMillis();
		assertEquals(1, room1.sendMessage("one"));
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed >= 2000);

		verifyHttpClient(httpClient, 5);
	}

	@Test
	public void sendMessage_permission_problem() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.request("GET", "https://chat.stackoverflow.com/rooms/1")
			.response(200, ResponseSamples.protectedChatRoom("0123456789abcdef0123456789abcdef")) //room is protected
			
			.request("POST", "https://chat.stackoverflow.com/ws-auth",
				"roomid", "1",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.wsAuth("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247"))
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/events",
				"mode", "messages",
				"msgCount", "1",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, ResponseSamples.events()
				.event(1417041460, "message 1", 50, "User1", 1, 20157245)
			.build())
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);
		Session session = mock(Session.class);
		doReturn(session).when(ws).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI("wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247?l=1417023460")));

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);
		try {
			room.sendMessage("one");
			fail();
		} catch (RoomPermissionException e) {
			//expected
		}

		verifyHttpClient(httpClient, 3);
	}

	@Test
	public void sendMessage_bad_responses() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)

			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "one",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(404, "The room does not exist, or you do not have permission")
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "one",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "not JSON")
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "one",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "{}")
			
			.request("POST", "https://chat.stackoverflow.com/chats/1/messages/new",
				"text", "one",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "{\"id\": \"value\"}")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		try {
			room.sendMessage("one");
			fail();
		} catch (IOException e) {
			//expected
		}

		try {
			room.sendMessage("one");
			fail();
		} catch (IOException e) {
			//expected
		}

		assertEquals(0, room.sendMessage("one"));
		assertEquals(0, room.sendMessage("one"));

		verifyHttpClient(httpClient, 7);
	}

	@Test
	public void editMessage() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)

			.request("POST", "https://chat.stackoverflow.com/messages/20157247",
				"text", "edited",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "\"ok\"")
			
			.request("POST", "https://chat.stackoverflow.com/messages/20157247",
				"text", "edited",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(302, "")
			
			.request("POST", "https://chat.stackoverflow.com/messages/20157247",
				"text", "edited",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "\"This message has already been deleted and cannot be edited\"")
				
			.request("POST", "https://chat.stackoverflow.com/messages/20157247",
				"text", "edited",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "\"It is too late to edit this message.\"")
					
			.request("POST", "https://chat.stackoverflow.com/messages/20157247",
				"text", "edited",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "\"You can only edit your own messages\"")
			
			.request("POST", "https://chat.stackoverflow.com/messages/20157247",
				"text", "edited",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "unexpected response")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		room.editMessage(20157247, "edited");

		try {
			room.editMessage(20157247, "edited");
			fail();
		} catch (IOException e) {
			//expected
		}
		try {
			room.editMessage(20157247, "edited");
			fail();
		} catch (IOException e) {
			//expected
		}
		try {
			room.editMessage(20157247, "edited");
			fail();
		} catch (IOException e) {
			//expected
		}
		try {
			room.editMessage(20157247, "edited");
			fail();
		} catch (IOException e) {
			//expected
		}

		room.editMessage(20157247, "edited");

		verifyHttpClient(httpClient, 9);
	}

	@Test
	public void deleteMessage() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)

			.request("POST", "https://chat.stackoverflow.com/messages/20157247/delete",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "\"ok\"")
			
			.request("POST", "https://chat.stackoverflow.com/messages/20157247/delete",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(302, "")
			
			.request("POST", "https://chat.stackoverflow.com/messages/20157247/delete",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "\"This message has already been deleted.\"")
				
			.request("POST", "https://chat.stackoverflow.com/messages/20157247/delete",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "\"It is too late to delete this message\"")
					
			.request("POST", "https://chat.stackoverflow.com/messages/20157247/delete",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "\"You can only delete your own messages\"")
			
			.request("POST", "https://chat.stackoverflow.com/messages/20157247/delete",
				"fkey", "0123456789abcdef0123456789abcdef"
			)
			.response(200, "unexpected response")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		room.deleteMessage(20157247);

		try {
			room.deleteMessage(20157247);
			fail();
		} catch (IOException e) {
			//expected
		}

		room.deleteMessage(20157247);

		try {
			room.deleteMessage(20157247);
			fail();
		} catch (IOException e) {
			//expected
		}
		try {
			room.deleteMessage(20157247);
			fail();
		} catch (IOException e) {
			//expected
		}

		room.deleteMessage(20157247);

		verifyHttpClient(httpClient, 9);
	}

	@Test
	public void getPingableUsers() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)

			.request("GET", "https://chat.stackoverflow.com/rooms/pingable/1"	)
			.response(200, ResponseSamples.pingableUsers()
				.user(13379, "Michael", 1501806926, 1501769526)
				.user(4258326, "OakBot", 1501806934, 1501802068)
			.build())
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		List<PingableUser> users = room.getPingableUsers();

		Iterator<PingableUser> it = users.iterator();

		PingableUser user = it.next();
		assertEquals(timestamp(1501769526), user.getLastPost());
		assertEquals(1, user.getRoomId());
		assertEquals(13379, user.getUserId());
		assertEquals("Michael", user.getUsername());

		user = it.next();
		assertEquals(timestamp(1501802068), user.getLastPost());
		assertEquals(1, user.getRoomId());
		assertEquals(4258326, user.getUserId());
		assertEquals("OakBot", user.getUsername());

		assertFalse(it.hasNext());

		verifyHttpClient(httpClient, 4);
	}

	@Test
	public void getPingableUsers_bad_responses() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
			
			.request("GET", "https://chat.stackoverflow.com/rooms/pingable/1"	)
			.response(404, "")

			.request("GET", "https://chat.stackoverflow.com/rooms/pingable/1"	)
			.response(200, "not JSON")
			
			.request("GET", "https://chat.stackoverflow.com/rooms/pingable/1"	)
			.response(200, "{}")
			
			.request("GET", "https://chat.stackoverflow.com/rooms/pingable/1"	)
			.response(200, "[]")
			
			.request("GET", "https://chat.stackoverflow.com/rooms/pingable/1"	)
			.response(200, "[ {}, [1, \"User\"], [13379, \"Michael\", 1501806926, 1501769526] ]")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		try {
			room.getPingableUsers();
			fail();
		} catch (IOException e) {
			//expected
		}

		try {
			room.getPingableUsers();
			fail();
		} catch (IOException e) {
			//expected
		}

		assertEquals(0, room.getPingableUsers().size());
		assertEquals(0, room.getPingableUsers().size());
		assertEquals(1, room.getPingableUsers().size());

		verifyHttpClient(httpClient, 8);
	}

	@Test
	public void getUserInfo() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)

			.request("POST", "https://chat.stackoverflow.com/user/info",
				"ids", "13379,4258326",
				"roomId", "1"
			)
			.response(200, ResponseSamples.userInfo()
				.user(13379, "Michael", "!https://i.stack.imgur.com/awces.jpg?s=128\\u0026g=1", 23145, false, true, 1501724997, 1501770855)
				.user(4258326, "OakBot", "f5166c4602a6deaf2accdc98c89e9b82", 408, false, false, 1501769545, 1501771253)
			.build())
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		List<UserInfo> users = room.getUserInfo(Arrays.asList(13379, 4258326));

		Iterator<UserInfo> it = users.iterator();

		UserInfo user = it.next();
		assertEquals(13379, user.getUserId());
		assertEquals("Michael", user.getUsername());
		assertEquals("https://i.stack.imgur.com/awces.jpg?s=128&g=1", user.getProfilePicture());
		assertEquals(23145, user.getReputation());
		assertFalse(user.isModerator());
		assertTrue(user.isOwner());
		assertEquals(timestamp(1501724997), user.getLastPost());
		assertEquals(timestamp(1501770855), user.getLastSeen());

		user = it.next();
		assertEquals(4258326, user.getUserId());
		assertEquals("OakBot", user.getUsername());
		assertEquals("https://www.gravatar.com/avatar/f5166c4602a6deaf2accdc98c89e9b82?d=identicon&s=128", user.getProfilePicture());
		assertEquals(408, user.getReputation());
		assertFalse(user.isModerator());
		assertFalse(user.isOwner());
		assertEquals(timestamp(1501769545), user.getLastPost());
		assertEquals(timestamp(1501771253), user.getLastSeen());

		assertFalse(it.hasNext());

		verifyHttpClient(httpClient, 4);
	}

	@Test
	public void getUserInfo_bad_responses() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)

			.request("POST", "https://chat.stackoverflow.com/user/info",
				"ids", "13379",
				"roomId", "1"
			)
			.response(200, "not JSON")
			
			.request("POST", "https://chat.stackoverflow.com/user/info",
				"ids", "13379",
				"roomId", "1"
			)
			.response(200, "{}")
			
			.request("POST", "https://chat.stackoverflow.com/user/info",
				"ids", "13379",
				"roomId", "1"
			)
			.response(200, "{ \"users\":{} }")
			
			.request("POST", "https://chat.stackoverflow.com/user/info",
				"ids", "13379",
				"roomId", "1"
			)
			.response(200, "{ \"users\":[ {} ] }")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		try {
			room.getUserInfo(Arrays.asList(13379));
			fail();
		} catch (IOException e) {
			//expected
		}

		assertEquals(0, room.getUserInfo(Arrays.asList(13379)).size());
		assertEquals(0, room.getUserInfo(Arrays.asList(13379)).size());

		List<UserInfo> users = room.getUserInfo(Arrays.asList(13379));
		assertEquals(1, users.size());
		UserInfo user = users.get(0);
		assertEquals(0, user.getUserId());
		assertNull(user.getUsername());
		assertNull(user.getProfilePicture());
		assertEquals(0, user.getReputation());
		assertFalse(user.isModerator());
		assertFalse(user.isOwner());
		assertNull(user.getLastPost());
		assertNull(user.getLastSeen());

		verifyHttpClient(httpClient, 7);
	}

	@Test
	public void getRoomInfo() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)

			.request("GET", "https://chat.stackoverflow.com/rooms/thumbs/1")
			.response(200, ResponseSamples.roomInfo(
				1,
				"Sandbox",
				"Where you can play with regular chat features (except flagging) without upsetting anyone",
				true,
				Arrays.asList("one", "two")
			))
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		RoomInfo info = room.getRoomInfo();
		assertEquals("Where you can play with regular chat features (except flagging) without upsetting anyone", info.getDescription());
		assertEquals(1, info.getId());
		assertEquals("Sandbox", info.getName());
		assertEquals(Arrays.asList("one", "two"), info.getTags());

		verifyHttpClient(httpClient, 4);
	}

	@Test
	public void getRoomInfo_bad_responses() throws Exception {
		//@formatter:off
		CloseableHttpClient httpClient = new MockHttpClientBuilder()
			.joinRoom(1, "0123456789abcdef0123456789abcdef", "wss://chat.sockets.stackexchange.com/events/1/37516a6eb3464228bf48a33088b3c247", 1417023460)
			
			.request("GET", "https://chat.stackoverflow.com/rooms/thumbs/1")
			.response(404, "")

			.request("GET", "https://chat.stackoverflow.com/rooms/thumbs/1")
			.response(200, "not JSON")
			
			.request("GET", "https://chat.stackoverflow.com/rooms/thumbs/1")
			.response(200, "[]")
			
			.request("GET", "https://chat.stackoverflow.com/rooms/thumbs/1")
			.response(200, "{}")
			
			.request("GET", "https://chat.stackoverflow.com/rooms/thumbs/1")
			.response(200, "{\"tags\":\"garbage\"}")
		.build();
		//@formatter:on

		WebSocketContainer ws = mock(WebSocketContainer.class);

		ChatClient chatClient = new ChatClient(httpClient, ws);
		Room room = chatClient.joinRoom(1);

		try {
			room.getRoomInfo();
			fail();
		} catch (IOException e) {
			//expected
		}

		try {
			room.getRoomInfo();
			fail();
		} catch (IOException e) {
			//expected
		}

		RoomInfo info = room.getRoomInfo();
		assertNull(info.getDescription());
		assertEquals(0, info.getId());
		assertNull(info.getName());
		assertEquals(Arrays.asList(), info.getTags());

		info = room.getRoomInfo();
		assertNull(info.getDescription());
		assertEquals(0, info.getId());
		assertNull(info.getName());
		assertEquals(Arrays.asList(), info.getTags());

		info = room.getRoomInfo();
		assertNull(info.getDescription());
		assertEquals(0, info.getId());
		assertNull(info.getName());
		assertEquals(Arrays.asList(), info.getTags());

		verifyHttpClient(httpClient, 8);
	}

	/**
	 * @see ChatClientTest#leave_room
	 */
	@Test
	public void leave() throws Exception {
		//empty
	}

	private static void verifyHttpClient(CloseableHttpClient httpClient, int requests) throws IOException {
		verify(httpClient, times(requests)).execute(any(HttpUriRequest.class));
	}

	/**
	 * Represents a mock web socket server.
	 * @author Michael Angstadt
	 */
	private static class MockWebSocketServer {
		private Whole<String> messageHandler;

		/**
		 * @param container the mock container. This object is shared amongst
		 * all the {@link MockWebSocketServer} instances (each instance is for a
		 * single chat room).
		 * @param url the expected URL that the room will use to connect to the
		 * web socket
		 */
		@SuppressWarnings("unchecked")
		public MockWebSocketServer(WebSocketContainer container, String url) throws Exception {
			Session session = mock(Session.class);
			doAnswer((invocation) -> {
				messageHandler = (Whole<String>) invocation.getArguments()[1];
				return null;
			}).when(session).addMessageHandler(eq(String.class), any(Whole.class));

			when(container.connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), eq(new URI(url)))).then((invocation) -> {
				Endpoint endpoint = (Endpoint) invocation.getArguments()[0];
				endpoint.onOpen(session, mock(EndpointConfig.class));
				return session;
			});
		}

		/**
		 * Sends a web socket message.
		 * @param message the message to send
		 */
		public void send(String message) {
			messageHandler.onMessage(message);
		}
	}

	/**
	 * Converts a timestamp to a {@link LocalDateTime} instance.
	 * @param ts the timestamp (seconds since epoch)
	 * @return the {@link LocalDateTime} instance
	 */
	private static LocalDateTime timestamp(long ts) {
		Instant instant = Instant.ofEpochSecond(ts);
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
	}
}
