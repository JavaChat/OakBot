package oakbot.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import oakbot.chat.IChatClient;

/**
 * A mock chat client that reads its chat messages from a text file.
 * @author Michael Angstadt
 */
public class FileChatClient implements IChatClient {
	private final AtomicLong eventIdCounter = new AtomicLong(), messageIdCounter = new AtomicLong();
	private final List<FileChatRoom> rooms = new ArrayList<>();
	private final int botUserId, humanUserId;
	private final String botUsername, humanUsername;

	public FileChatClient(int botUserId, String botUsername, int humanUserId, String humanUsername) {
		this.botUserId = botUserId;
		this.botUsername = botUsername;
		this.humanUserId = humanUserId;
		this.humanUsername = humanUsername;
	}

	@Override
	public void login(String email, String password) {
		//empty
	}

	@Override
	public FileChatRoom joinRoom(int roomId) throws IOException {
		Path inputFile = Paths.get("local.room" + roomId + ".txt");
		Files.deleteIfExists(inputFile);
		Files.createFile(inputFile);

		FileChatRoom room = new FileChatRoom(roomId, humanUserId, humanUsername, botUserId, botUsername, eventIdCounter, messageIdCounter, inputFile, this);
		rooms.add(room);
		return room;
	}

	void leave(FileChatRoom room) {
		rooms.remove(room);
	}

	@Override
	public void close() throws IOException {
		for (FileChatRoom room : rooms) {
			room.close();
		}
		rooms.clear();
	}

	@Override
	public String toString() {
		return rooms.toString();
	}

	@Override
	public List<FileChatRoom> getRooms() {
		return rooms;
	}

	@Override
	public FileChatRoom getRoom(int roomId) {
		for (FileChatRoom room : rooms) {
			if (room.getRoomId() == roomId) {
				return room;
			}
		}
		return null;
	}

	@Override
	public boolean isInRoom(int roomId) {
		return getRoom(roomId) != null;
	}
}
