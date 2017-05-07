package oakbot.local;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oakbot.chat.ChatConnection;
import oakbot.chat.ChatMessage;
import oakbot.chat.ChatMessageHandler;
import oakbot.chat.PingableUser;
import oakbot.chat.RoomInfo;
import oakbot.chat.RoomNotFoundException;
import oakbot.chat.RoomPermissionException;
import oakbot.chat.SplitStrategy;
import oakbot.chat.UserInfo;

/**
 * A mock chat connection that reads its chat messages from a text file.
 * @author Michael Angstadt
 */
public class FileChatConnection implements ChatConnection {
	private final Path inputFile;
	private final int botUserId, humanUserId;
	private final String botUsername, humanUsername;
	private final int roomId;
	private final Map<Integer, List<ChatMessage>> rooms = new HashMap<>();
	private final Map<Integer, Integer> roomCursor = new HashMap<>();
	private ChatMessageHandler handler;
	private boolean closed = false;
	private long messageIdCounter = 0;

	public FileChatConnection(int botUserId, String botUsername, int humanUserId, String humanUsername, int roomId, Path inputFile) {
		this.botUserId = botUserId;
		this.botUsername = botUsername;
		this.humanUserId = humanUserId;
		this.humanUsername = humanUsername;
		this.roomId = roomId;
		this.inputFile = inputFile;
	}

	@Override
	public void login(String email, String password) {
		//empty
	}

	@Override
	public void joinRoom(int roomId) {
		if (!roomCursor.containsKey(roomId)) {
			roomCursor.put(roomId, 0);
		}
	}

	@Override
	public void leaveRoom(int roomId) {
		roomCursor.remove(roomId);
	}

	@Override
	public List<ChatMessage> getMessages(int room, int count) {
		List<ChatMessage> messages = rooms.get(room);
		if (messages == null) {
			return Collections.emptyList();
		}

		return new ArrayList<>(messages.subList(messages.size() - count, messages.size()));
	}

	@Override
	public void listen(ChatMessageHandler handler) {
		this.handler = handler;

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
				 * Multi-line messages can be specified by ending each line with
				 * a backslash.
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

				//post the message
				postMessage(roomId, humanUserId, humanUsername, sb.toString());

				if (closed) {
					break;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			//ignore
		}
	}

	@Override
	public long sendMessage(int room, String message) {
		return sendMessage(room, message, null).get(0);
	}

	@Override
	public List<Long> sendMessage(int room, String message, SplitStrategy splitStragey) {
		long id = postMessage(room, botUserId, botUsername, message);
		return Arrays.asList(id);
	}

	/**
	 * Posts a message to a chat room.
	 * @param roomId the room ID
	 * @param userId the user ID of the message author
	 * @param username the username of the message author
	 * @param content the message content
	 * @return the message ID
	 */
	public long postMessage(int roomId, int userId, String username, String content) {
		long id = messageIdCounter++;

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

		List<ChatMessage> messages = rooms.get(roomId);
		if (messages == null) {
			messages = new ArrayList<>();
			rooms.put(roomId, messages);
		}

		//indent additional lines for readability
		content = content.replaceAll("\r\n|\r|\n", "$0  ");

		System.out.println(roomId + " > " + username + " > " + content);

		messages.add(message);

		if (handler != null) {
			handler.onMessage(message);
		}

		return id;
	}

	@Override
	public boolean deleteMessage(int roomId, long messageId) throws RoomNotFoundException, RoomPermissionException, IOException {
		System.out.println(roomId + "> " + botUsername + " > (delete message " + messageId);
		return true;
	}

	@Override
	public boolean editMessage(int roomId, long messageId, String updatedMessage) throws RoomNotFoundException, RoomPermissionException, IOException {
		System.out.println(roomId + "> " + botUsername + " > EDIT " + messageId + ": " + updatedMessage);
		return true;
	}

	@Override
	public void flush() throws IOException {
		//empty
	}

	@Override
	public void close() throws IOException {
		closed = true;
	}

	@Override
	public String toString() {
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

	@Override
	public List<UserInfo> getUserInfo(int roomId, List<Integer> userIds) throws IOException {
		//@formatter:off
		return Arrays.asList(
			new UserInfo.Builder()
				.userId(userIds.get(0))
				.roomId(roomId)
				.username(humanUsername)
				.reputation(500)
			.build()
		);
		//@formatter:on
	}

	@Override
	public List<PingableUser> getPingableUsers(int roomId) throws IOException {
		//@formatter:off
		return Arrays.asList(
			new PingableUser(roomId, humanUserId, humanUsername, LocalDateTime.now()),
			new PingableUser(roomId, botUserId, botUsername, LocalDateTime.now())
		);
		//@formatter:on
	}

	@Override
	public RoomInfo getRoomInfo(int roomId) throws IOException {
		return new RoomInfo(roomId, "name", "description", Arrays.asList("one", "two", "three"));
	}
}
