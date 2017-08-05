package oakbot.chat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.websocket.WebSocketContainer;

import org.apache.http.impl.client.CloseableHttpClient;

import oakbot.chat.event.MessageEditedEvent;
import oakbot.chat.event.MessagePostedEvent;

/**
 * A connection to Stack Overflow Chat that uses an event based system for
 * responding to web socket events. This class is thread-safe.
 * @author Michael Angstadt
 * @see <a href="http://chat.stackoverflow.com">chat.stackoverflow.com</a>
 * @see <a href=
 * "https://github.com/Zirak/SO-ChatBot/blob/master/source/adapter.js">Good
 * explanation of how SO Chat works</a>
 */
public class StackoverflowChatWSEvents implements ChatConnection {
	private final IChatClient chatClient;
	private final BlockingQueue<ChatMessage> newMessages = new LinkedBlockingQueue<>();
	private final ChatMessage CLOSE_MESSAGE = new ChatMessage.Builder().build();

	/**
	 * Creates an event based, web socket connection to Stack Overflow Chat.
	 * @param httpClient the HTTP client (used to interact to the chat room)
	 * @param webSocketClient the web socket client (used to listen for events)
	 */
	public StackoverflowChatWSEvents(CloseableHttpClient httpClient, WebSocketContainer webSocketClient) {
		chatClient = new ChatClient(httpClient, webSocketClient);
	}

	@Override
	public void login(String email, String password) throws InvalidCredentialsException, IOException {
		chatClient.login(email, password);
	}

	@Override
	public void joinRoom(int roomId) throws RoomNotFoundException, RoomPermissionException, IOException {
		if (chatClient.isInRoom(roomId)) {
			return;
		}

		IRoom room = chatClient.joinRoom(roomId);
		room.addEventListener(MessagePostedEvent.class, (event) -> {
			newMessages.add(event.getMessage());
		});
		room.addEventListener(MessageEditedEvent.class, (event) -> {
			newMessages.add(event.getMessage());
		});
	}

	@Override
	public void leaveRoom(int roomId) {
		IRoom room = chatClient.getRoom(roomId);
		if (room == null) {
			return;
		}

		room.leave();
	}

	@Override
	public long sendMessage(int roomId, String message) throws IOException {
		return sendMessage(roomId, message, SplitStrategy.NONE).get(0);
	}

	@Override
	public List<Long> sendMessage(int roomId, String message, SplitStrategy splitStrategy) throws IOException {
		IRoom room = chatClient.getRoom(roomId);
		if (room == null) {
			throw new IllegalStateException("Room must be joined first.");
		}

		return room.sendMessage(message, splitStrategy);
	}

	@Override
	public List<ChatMessage> getMessages(int roomId, int count) throws IOException {
		IRoom room = chatClient.getRoom(roomId);
		if (room == null) {
			throw new IllegalStateException("Room must be joined first.");
		}

		return room.getMessages(count);
	}

	@Override
	public boolean deleteMessage(int roomId, long messageId) throws IOException {
		IRoom room = chatClient.getRoom(roomId);
		if (room == null) {
			throw new IllegalStateException("Room must be joined first.");
		}

		try {
			room.deleteMessage(messageId);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean editMessage(int roomId, long messageId, String updatedMessage) throws IOException {
		IRoom room = chatClient.getRoom(roomId);
		if (room == null) {
			throw new IllegalStateException("Room must be joined first.");
		}

		try {
			room.editMessage(messageId, updatedMessage);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public List<UserInfo> getUserInfo(int roomId, List<Integer> userIds) throws IOException {
		IRoom room = chatClient.getRoom(roomId);
		if (room == null) {
			throw new IllegalStateException("Room must be joined first.");
		}

		return room.getUserInfo(userIds);
	}

	@Override
	public List<PingableUser> getPingableUsers(int roomId) throws IOException {
		IRoom room = chatClient.getRoom(roomId);
		if (room == null) {
			throw new IllegalStateException("Room must be joined first.");
		}

		return room.getPingableUsers();
	}

	@Override
	public RoomInfo getRoomInfo(int roomId) throws IOException {
		IRoom room = chatClient.getRoom(roomId);
		if (room == null) {
			throw new IllegalStateException("Room must be joined first.");
		}

		return room.getRoomInfo();
	}

	@Override
	public void listen(ChatMessageHandler handler) {
		while (true) {
			ChatMessage message;
			try {
				message = newMessages.take();
			} catch (InterruptedException e) {
				break;
			}

			if (message == CLOSE_MESSAGE) {
				break;
			}

			if (message.getEdits() == 0) {
				handler.onMessage(message);
			} else {
				handler.onMessageEdited(message);
			}
		}
	}

	@Override
	public synchronized void close() throws IOException {
		flush();
		newMessages.clear();
		chatClient.close();

		try {
			newMessages.put(CLOSE_MESSAGE);
		} catch (InterruptedException e) {
			//ignore
		}
	}

	@Override
	public void flush() {
		//empty
	}
}
