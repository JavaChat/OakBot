package oakbot.local;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import oakbot.chat.ChatConnection;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;

/**
 * A mock chat connection.
 * @author Michael Angstadt
 */
public class MockChatConnection implements ChatConnection {
	private final int botUserId;
	private final String botName;
	private final AtomicInteger messageIdCounter = new AtomicInteger();
	private final Map<Integer, List<ChatMessage>> rooms = new HashMap<>();
	private final Map<Integer, Integer> roomCursor = new HashMap<>();

	public MockChatConnection(int botUserId, String botName) {
		this.botUserId = botUserId;
		this.botName = botName;
	}

	@Override
	public void login(String email, String password) {
		//empty
	}

	@Override
	public synchronized void joinRoom(int roomId) {
		if (!roomCursor.containsKey(roomId)) {
			roomCursor.put(roomId, 0);
		}
	}

	@Override
	public synchronized void leaveRoom(int roomId) {
		roomCursor.remove(roomId);
	}

	@Override
	public synchronized List<ChatMessage> getMessages(int room, int count) {
		List<ChatMessage> messages = rooms.get(room);
		if (messages == null) {
			return Collections.emptyList();
		}

		return new ArrayList<>(messages.subList(messages.size() - count, messages.size()));
	}

	@Override
	public synchronized List<ChatMessage> getNewMessages(int room) {
		List<ChatMessage> messages = rooms.get(room);
		if (messages == null) {
			return Collections.emptyList();
		}

		Integer cursor = roomCursor.get(room);
		if (cursor == null) {
			roomCursor.put(room, 0);
			cursor = 0;
		}

		if (cursor == messages.size()) {
			return Collections.emptyList();
		}

		roomCursor.put(room, messages.size());
		return new ArrayList<>(messages.subList(cursor, messages.size()));
	}

	@Override
	public void sendMessage(int room, String message) {
		sendMessage(room, message, null);
	}

	@Override
	public void sendMessage(int room, String message, SplitStrategy splitStragey) {
		//System.out.println(room + " > " + botName + " > " + message);
		postMessage(room, botUserId, botName, message);
	}

	/**
	 * Posts a message to a chat room.
	 * @param roomId the room ID
	 * @param userId the user ID of the message author
	 * @param username the username of the message author
	 * @param content the message content
	 */
	public void postMessage(int roomId, int userId, String username, String content) {
		ChatMessage message = new ChatMessage();
		message.setRoomId(roomId);
		message.setUserId(userId);
		message.setUsername(username);
		message.setContent(content);
		message.setMessageId(messageIdCounter.incrementAndGet());
		message.setEdits(0);
		message.setTimestamp(LocalDateTime.now());

		synchronized (this) {
			List<ChatMessage> messages = rooms.get(roomId);
			if (messages == null) {
				messages = new ArrayList<>();
				rooms.put(roomId, messages);
			}

			content = content.replaceAll("\r\n|\r|\n", "$0  ");
			System.out.println(roomId + " > " + username + " > " + content);

			messages.add(message);
		}
	}

	@Override
	public void flush() throws IOException {
		//empty
	}

	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, List<ChatMessage>> entry : rooms.entrySet()) {
			Integer roomId = entry.getKey();
			List<ChatMessage> messages = entry.getValue();

			sb.append(roomId).append(": ").append(messages.size()).append(" messages\n");
			for (ChatMessage message : messages) {
				sb.append("  ").append(message.getContent()).append("\n");
			}
		}

		return sb.toString();
	}
}
