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
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private static final Logger logger = Logger.getLogger(FileChatRoom.class.getName());

	private final int roomId;
	private final int humanUserId, botUserId;
	private final String humanUsername, humanProfilePicture, botUsername;
	private final FileChatClient connection;

	private final Thread fileMonitor;
	private final AtomicLong eventId, messageId;
	private final List<ChatMessage> messages = new ArrayList<>();

	private Consumer<MessagePostedEvent> listener;

	public FileChatRoom(int roomId, int humanUserId, String humanUsername, String humanProfilePicture, int botUserId, String botUsername, AtomicLong eventId, AtomicLong messageId, Path inputFile, FileChatClient connection) throws IOException {
		this.roomId = roomId;
		this.humanUserId = humanUserId;
		this.humanUsername = humanUsername;
		this.humanProfilePicture = humanProfilePicture;
		this.botUserId = botUserId;
		this.botUsername = botUsername;
		this.connection = connection;

		this.eventId = eventId;
		this.messageId = messageId;

		fileMonitor = new Thread(() -> {
			try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
				while (true) {
					String line = reader.readLine();
					if (line == null) {
						//wait for more input
						Thread.sleep(1000);
						continue;
					}

					/*
					 * Read the next line.
					 * 
					 * Multi-line messages can be specified by ending each line
					 * with a backslash.
					 */
					StringBuilder sb = new StringBuilder(line.length());
					while (line != null) {
						boolean multiline = line.endsWith("\\");
						if (!multiline) {
							sb.append(line);
							break;
						}

						sb.append(line, 0, line.length() - 1).append('\n');
						line = reader.readLine();
					}

					postMessage(humanUserId, humanUsername, sb.toString());
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.log(Level.SEVERE, "Thread interrupted while waiting for input from \"" + inputFile.getFileName() + "\".", e);
			}
		});
		fileMonitor.start();
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
		long id = postMessage(botUserId, botUsername, message);
		return List.of(id);
	}

	/**
	 * Posts a message to a chat room.
	 * @param roomId the room ID
	 * @param userId the user ID of the message author
	 * @param username the username of the message author
	 * @param content the message content
	 * @return the message ID
	 */
	public long postMessage(int userId, String username, String content) {
		long id = messageId.getAndIncrement();

		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
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
		//@formatter:off
		return List.of(
			new UserInfo.Builder()
				.userId(userIds.get(0))
				.roomId(roomId)
				.username(humanUsername)
				.profilePicture(humanProfilePicture)
				.reputation(500)
			.build()
		);
		//@formatter:on
	}

	@Override
	public List<PingableUser> getPingableUsers() throws IOException {
		//@formatter:off
		return List.of(
			new PingableUser(roomId, humanUserId, humanUsername, LocalDateTime.now()),
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
		StringBuilder sb = new StringBuilder();

		synchronized (messages) {
			sb.append(roomId).append(": ").append(messages.size()).append(" messages\n");
			for (ChatMessage message : messages) {
				sb.append("  ").append(message.getContent()).append("\n");
			}
		}

		return sb.toString();
	}
}
