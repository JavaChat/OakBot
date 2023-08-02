package oakbot.chat.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.IChatClient;
import com.github.mangstadt.sochat4j.IRoom;
import com.github.mangstadt.sochat4j.Site;

/**
 * A mock chat client that reads its chat messages from a text file.
 * @author Michael Angstadt
 */
public class FileChatClient implements IChatClient {
	private final AtomicLong eventIdCounter = new AtomicLong(), messageIdCounter = new AtomicLong();
	private final List<FileChatRoom> rooms = new ArrayList<>();
	private final int botUserId, humanUserId;
	private final String botUsername, humanUsername, humanProfilePicture;

	public FileChatClient(int botUserId, String botUsername, int humanUserId, String humanUsername, String humanProfilePicture) {
		this.botUserId = botUserId;
		this.botUsername = botUsername;
		this.humanUserId = humanUserId;
		this.humanUsername = humanUsername;
		this.humanProfilePicture = humanProfilePicture;
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

		FileChatRoom room = new FileChatRoom(roomId, humanUserId, humanUsername, humanProfilePicture, botUserId, botUsername, eventIdCounter, messageIdCounter, inputFile, this);
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
	public List<IRoom> getRooms() {
		return rooms.stream().map(r -> (IRoom) r).collect(Collectors.toList());
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

	@Override
	public String getOriginalMessageContent(long messageId) throws IOException {
		for (FileChatRoom room : rooms) {
			for (ChatMessage message : room.getAllMessages()) {
				if (message.getMessageId() == messageId) {
					return message.getContent().getContent();
				}
			}
		}
		return null;
	}

	@Override
	public Site getSite() {
		return null;
	}
}
