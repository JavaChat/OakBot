package oakbot.chat;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.WebSocketContainer;

import org.apache.http.impl.client.CloseableHttpClient;

import oakbot.util.Http;

/**
 * A connection to a Stack Exchange chat site that uses web sockets to retrieve
 * new messages. This class is thread-safe.
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
	private final Site site;
	private final Map<Integer, Room> rooms = new LinkedHashMap<>();
	private boolean loggedIn = false;

	/**
	 * Creates a connection to Stack Overflow Chat. Note that the connection is
	 * not established until {@link #login} is called.
	 * @param httpClient the HTTP client
	 * @param webSocketClient the web socket client
	 */
	public ChatClient(CloseableHttpClient httpClient, WebSocketContainer webSocketClient) {
		this(httpClient, webSocketClient, Site.STACKOVERFLOW);
	}

	/**
	 * Creates a connection to a Stack Exchange chat site. Note that the
	 * connection is not established until {@link #login} is called.
	 * @param httpClient the HTTP client
	 * @param webSocketClient the web socket client
	 * @param site the Stack Exchange site to connect to
	 */
	public ChatClient(CloseableHttpClient httpClient, WebSocketContainer webSocketClient, Site site) {
		this.http = new Http(httpClient);
		this.webSocketClient = requireNonNull(webSocketClient);
		this.site = requireNonNull(site);
	}

	@Override
	public void login(String email, String password) throws InvalidCredentialsException, IOException {
		boolean success = site.login(email, password, http);
		if (!success) {
			throw new InvalidCredentialsException();
		}

		/*
		 * Note: The authenticated session info is stored in the HttpClient's
		 * cookie store.
		 */
		loggedIn = true;
	}

	@Override
	public Room joinRoom(int roomId) throws RoomNotFoundException, IOException {
		if (!loggedIn) {
			throw new IllegalStateException("Client is not authenticated. Call the \"login\" method first.");
		}

		synchronized (rooms) {
			Room room = rooms.get(roomId);
			if (room != null) {
				return room;
			}

			room = new Room(roomId, site, http, webSocketClient, this);
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
	public String getOriginalMessageContent(long messageId) throws IOException {
		Http.Response response = http.get("https://" + site.getChatDomain() + "/message/" + messageId + "?plain=true");
		int statusCode = response.getStatusCode();
		if (statusCode != 200) {
			throw new IOException("HTTP " + statusCode + " response returned: " + response.getBody());
		}

		return response.getBody();
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
					http.post("https://" + site.getChatDomain() + "/chats/leave/all",
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
