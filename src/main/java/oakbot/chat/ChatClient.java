package oakbot.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.WebSocketContainer;

import org.apache.http.impl.client.CloseableHttpClient;

import oakbot.util.ChatUtils;
import oakbot.util.Http;
import oakbot.util.Http.Response;

/**
 * A connection to Stack Overflow Chat that uses web sockets to retrieve new
 * messages. This class is thread-safe.
 * @author Michael Angstadt
 * @see <a href="https://chat.stackoverflow.com">chat.stackoverflow.com</a>
 * @see <a href=
 * "https://github.com/Zirak/SO-ChatBot/blob/master/source/adapter.js">Good
 * explanation of how SO Chat works</a>
 */
public class ChatClient implements IChatClient {
	private static final Logger logger = Logger.getLogger(ChatClient.class.getName());

	private final Http http;
	private final WebSocketContainer webSocketClient;
	private final String domain, chatDomain;
	private final Map<Integer, Room> rooms = new LinkedHashMap<>();

	/**
	 * Creates a web socket connection to Stack Overflow Chat.
	 * @param httpClient the HTTP client (used to interact to the chat room)
	 * @param webSocketClient the web socket client (used to listen for new
	 * messages)
	 */
	public ChatClient(CloseableHttpClient httpClient, WebSocketContainer webSocketClient) {
		this(httpClient, webSocketClient, "stackoverflow.com");
	}

	/**
	 * Creates a web socket connection to a Stack Exchange chat network.
	 * @param httpClient the HTTP client (used to interact to the chat room)
	 * @param webSocketClient the web socket client (used to listen for new
	 * messages)
	 * @param domain the Stack Exchange website to connect to (e.g.
	 * "stackoverflow.com")
	 */
	public ChatClient(CloseableHttpClient httpClient, WebSocketContainer webSocketClient, String domain) {
		this.http = new Http(httpClient);
		this.webSocketClient = webSocketClient;
		this.domain = domain;
		chatDomain = "https://chat." + domain;
	}

	@Override
	public void login(String email, String password) throws InvalidCredentialsException, IOException {
		Response response = http.get("https://" + domain + "/users/login");
		String fkey = ChatUtils.parseFkey(response.getBody());
		if (fkey == null) {
			throw new IOException("\"fkey\" field not found on login page, cannot login.");
		}

		//@formatter:off
		response = http.post("https://" + domain + "/users/login",
			"email", email,
			"password", password,
			"fkey", fkey
		);
		//@formatter:on

		int statusCode = response.getStatusCode();
		if (statusCode != 302) {
			throw new InvalidCredentialsException();
		}

		/*
		 * Note: The authenticated session info is stored in the HttpClient's
		 * cookie store.
		 */
	}

	@Override
	public Room joinRoom(int roomId) throws RoomNotFoundException, IOException {
		synchronized (rooms) {
			Room room = rooms.get(roomId);
			if (room != null) {
				return room;
			}

			room = new Room(roomId, domain, http, webSocketClient, this);
			rooms.put(roomId, room);

			return room;
		}
	}

	@Override
	public List<Room> getRooms() {
		synchronized (rooms) {
			return new ArrayList<Room>(rooms.values());
		}
	}

	@Override
	public Room getRoom(int roomId) {
		synchronized (rooms) {
			return rooms.get(roomId);
		}
	}

	@Override
	public boolean isInRoom(int roomId) {
		synchronized (rooms) {
			return rooms.containsKey(roomId);
		}
	}

	/**
	 * Removes a room from the list of connected rooms. For internal use only
	 * (invoked by {@link Room#leave}).
	 * @param room the room to remove from the list of connected rooms
	 */
	void removeRoom(Room room) {
		synchronized (rooms) {
			rooms.remove(room.getRoomId());
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (rooms) {
			//leave all rooms
			if (!rooms.isEmpty()) {
				Room anyRoom = rooms.values().iterator().next();
				String fkey = anyRoom.getFkey();

				try {
					//@formatter:off
					http.post(chatDomain + "/chats/leave/all",
						"quiet", "true", //setting this parameter to "false" results in an error
						"fkey", fkey
					);
					//@formatter:on
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Problem leaving all rooms.", e);
				}
			}

			for (Room room : rooms.values()) {
				try {
					room.close();
				} catch (IOException e) {
					logger.log(Level.WARNING, "Problem closing websocket connection for room " + room.getRoomId() + ".", e);
				}
			}
			rooms.clear();
		}

		try {
			http.close();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Problem closing HTTP connection.", e);
		}
	}
}
