package oakbot.chat.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.IRoom;
import com.github.mangstadt.sochat4j.PingableUser;
import com.github.mangstadt.sochat4j.RoomInfo;
import com.github.mangstadt.sochat4j.RoomNotFoundException;
import com.github.mangstadt.sochat4j.RoomPermissionException;
import com.github.mangstadt.sochat4j.SplitStrategy;
import com.github.mangstadt.sochat4j.UserInfo;
import com.github.mangstadt.sochat4j.event.Event;
import com.github.mangstadt.sochat4j.event.MessagePostedEvent;

/**
 * A mock connection to a chat room that reads messages from a text file.
 * @author Michael Angstadt
 */
public class FileChatRoom implements IRoom {
	private final int roomId;
	private final int botUserId;
	private final String botUsername;
	private final UserInfo human;
	private final FileChatClient connection;

	private final Thread fileMonitor;
	private final AtomicLong eventId;
	private final AtomicLong messageId;
	private final List<ChatMessage> messages = new ArrayList<>();

	private Consumer<MessagePostedEvent> listener;

	public FileChatRoom(int roomId, UserInfo human, Path inputFile, FileChatClient connection) {
		this.roomId = roomId;
		this.human = human;
		this.botUserId = connection.getUserId();
		this.botUsername = connection.getUsername();
		this.connection = connection;

		this.eventId = connection.getEventIdCounter();
		this.messageId = connection.getMessageIdCounter();

		fileMonitor = new Thread(() -> {
			try (var reader = new ChatRoomFileReader(inputFile)) {
				String line;
				while ((line = reader.readLine()) != null) {
					postMessage(human.getUserId(), human.getUsername(), line);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		fileMonitor.start();
	}

	private class ChatRoomFileReader extends BufferedReader {
		public ChatRoomFileReader(Path file) throws IOException {
			super(Files.newBufferedReader(file));
		}

		@Override
		public String readLine() throws IOException {
			while (true) {
				var line = super.readLine();

				/*
				 * Wait for more content to be added to the file.
				 */
				if (line == null) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						/*
						 * The program has been terminated.
						 */
						Thread.currentThread().interrupt();
						return null;
					}
					continue;
				}

				/*
				 * Most lines are not multiline, so return early for
				 * performance.
				 */
				if (!isMultiline(line)) {
					return line;
				}

				/*
				 * Read the next line, combining multi-line messages into a
				 * single string.
				 * 
				 * Multi-line messages can be specified by ending each line
				 * with a backslash.
				 */
				var lines = new ArrayList<String>();
				do {
					if (isMultiline(line)) {
						lines.add(line.substring(0, line.length() - 1));
					} else {
						lines.add(line);
						break;
					}
				} while ((line = super.readLine()) != null);

				return String.join("\n", lines);
			}
		}

		private boolean isMultiline(String line) {
			return line.endsWith("\\");
		}
	}

	@Override
	public int getRoomId() {
		return roomId;
	}

	@Override
	public String getFkey() {
		return "0123456789abcdef0123456789abcdef";
	}

	@Override
	public boolean canPost() {
		return true;
	}

	@Override
	public void addEventListener(Consumer<Event> listener) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Event> void addEventListener(Class<T> clazz, Consumer<T> listener) {
		if (clazz == MessagePostedEvent.class) {
			this.listener = (Consumer<MessagePostedEvent>) listener;
		}
	}

	@Override
	public List<ChatMessage> getMessages(int count) {
		int start = (messages.size() < count) ? 0 : messages.size() - count;
		synchronized (messages) {
			return new ArrayList<>(messages.subList(start, messages.size()));
		}
	}

	@Override
	public long sendMessage(String message) {
		return sendMessage(message, null).get(0);
	}

	@Override
	public List<Long> sendMessage(String message, SplitStrategy splitStragey) {
		var id = postMessage(botUserId, botUsername, message);
		return List.of(id);
	}

	/**
	 * Posts a message to a chat room.
	 * @param userId the user ID of the message author
	 * @param username the username of the message author
	 * @param content the message content
	 * @return the message ID
	 */
	public long postMessage(int userId, String username, String content) {
		var id = messageId.getAndIncrement();

		//@formatter:off
		var message = new ChatMessage.Builder()
			.roomId(roomId)
			.userId(userId)
			.username(username)
			.content(content)
			.messageId(id)
			.timestamp(LocalDateTime.now())
		.build();
		//@formatter:on

		//indent additional lines for readability
		content = content.replaceAll("\r\n|\r|\n", "$0  ");
		System.out.println(roomId + " > " + username + " > " + content);

		synchronized (messages) {
			messages.add(message);
		}

		if (listener != null) {
			//@formatter:off
			listener.accept(new MessagePostedEvent.Builder()
				.eventId(eventId.getAndIncrement())
				.timestamp(message.getTimestamp())
				.message(message)
			.build());
			//@formatter:on
		}

		return id;
	}

	@Override
	public void deleteMessage(long messageId) throws RoomNotFoundException, RoomPermissionException, IOException {
		System.out.println(roomId + " > " + botUsername + " > (deleted message " + messageId + ")");
	}

	@Override
	public void editMessage(long messageId, String updatedMessage) throws RoomNotFoundException, RoomPermissionException, IOException {
		System.out.println(roomId + " > " + botUsername + " > (edited message " + messageId + "): " + updatedMessage);
	}

	@Override
	public List<UserInfo> getUserInfo(List<Integer> userIds) throws IOException {
		return connection.getUserInfo(roomId, userIds);
	}

	@Override
	public List<PingableUser> getPingableUsers() throws IOException {
		//@formatter:off
		return List.of(
			new PingableUser(roomId, human.getUserId(), human.getUsername(), LocalDateTime.now()),
			new PingableUser(roomId, botUserId, botUsername, LocalDateTime.now())
		);
		//@formatter:on
	}

	@Override
	public RoomInfo getRoomInfo() throws IOException {
		return new RoomInfo(roomId, "name", "description", List.of("one", "two", "three"));
	}

	public List<ChatMessage> getAllMessages() {
		synchronized (messages) {
			return new ArrayList<>(messages);
		}
	}

	@Override
	public void leave() {
		connection.leave(this);
		close();
	}

	@Override
	public void close() {
		fileMonitor.interrupt();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();

		synchronized (messages) {
			sb.append(roomId).append(": ").append(messages.size()).append(" messages\n");
			for (var message : messages) {
				sb.append("  ").append(message.getContent()).append("\n");
			}
		}

		return sb.toString();
	}
}
