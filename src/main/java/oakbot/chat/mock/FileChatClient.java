package oakbot.chat.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.github.mangstadt.sochat4j.IChatClient;
import com.github.mangstadt.sochat4j.IRoom;
import com.github.mangstadt.sochat4j.Site;
import com.github.mangstadt.sochat4j.UserInfo;

/**
 * A mock chat client that reads its chat messages from a text file.
 * @author Michael Angstadt
 */
public class FileChatClient implements IChatClient {
	private final AtomicLong eventIdCounter = new AtomicLong();
	private final AtomicLong messageIdCounter = new AtomicLong();
	private final List<FileChatRoom> rooms = new ArrayList<>();
	private final Integer botUserId;
	private final Integer humanUserId;
	private final String botUsername;
	private final String humanUsername;
	private final String humanProfilePicture;

	public FileChatClient(Integer botUserId, String botUsername, Integer humanUserId, String humanUsername, String humanProfilePicture) {
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
	public String getUsername() {
		return botUsername;
	}

	@Override
	public Integer getUserId() {
		return botUserId;
	}

	@Override
	public FileChatRoom joinRoom(int roomId) throws IOException {
		var inputFile = Paths.get("local.room" + roomId + ".txt");
		Files.deleteIfExists(inputFile);
		Files.createFile(inputFile);

		//@formatter:off
		var human = new UserInfo.Builder()
			.roomId(roomId)
			.userId(humanUserId)
			.username(humanUsername)
			.profilePicture(humanProfilePicture)
		.build();
		//@formatter:on

		var room = new FileChatRoom(roomId, human, inputFile, this);
		rooms.add(room);
		return room;
	}

	void leave(FileChatRoom room) {
		rooms.remove(room);
	}

	@Override
	public void close() throws IOException {
		rooms.forEach(FileChatRoom::close);
		rooms.clear();
	}

	@Override
	public String toString() {
		return rooms.toString();
	}

	@Override
	public List<IRoom> getRooms() {
		return rooms.stream().map(r -> (IRoom) r).toList();
	}

	@Override
	public FileChatRoom getRoom(int roomId) {
		return rooms.stream().filter(r -> r.getRoomId() == roomId).findFirst().orElse(null);
	}

	@Override
	public boolean isInRoom(int roomId) {
		return getRoom(roomId) != null;
	}

	@Override
	public String getMessageContent(long messageId) throws IOException {
		return _getMessageContent(messageId);
	}

	@Override
	public String getOriginalMessageContent(long messageId) throws IOException {
		return _getMessageContent(messageId);
	}

	private String _getMessageContent(long messageId) throws IOException {
		for (var room : rooms) {
			for (var message : room.getAllMessages()) {
				if (message.id() == messageId) {
					return message.content().getContent();
				}
			}
		}
		throw new IOException("Message " + messageId + " not found.");
	}

	@Override
	public Site getSite() {
		return null;
	}

	@Override
	public String uploadImage(String url) throws IOException {
		throw new IOException("Method not implemented.");
	}

	@Override
	public String uploadImage(byte[] data) throws IOException {
		var now = LocalDateTime.now();
		var filename = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replaceAll("[-T:]", "") + ".jpg";
		var path = Paths.get(filename);
		Files.write(path, data);
		return filename;
	}

	public AtomicLong getEventIdCounter() {
		return eventIdCounter;
	}

	public AtomicLong getMessageIdCounter() {
		return messageIdCounter;
	}

	@Override
	public List<UserInfo> getUserInfo(int roomId, List<Integer> userIds) throws IOException {
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
}
