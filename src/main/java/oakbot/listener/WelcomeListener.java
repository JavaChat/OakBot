package oakbot.listener;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.Database;
import oakbot.bot.BotContext;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.IRoom;
import oakbot.chat.SplitStrategy;
import oakbot.chat.UserInfo;
import oakbot.util.ChatBuilder;

/**
 * Welcomes new users to the chat room.
 * @author Michael Angstadt
 */
public class WelcomeListener implements Listener {
	private static final Logger logger = Logger.getLogger(WelcomeListener.class.getName());
	private final Database db;
	private final Map<Integer, String> welcomeMessagesByRoom;
	private final Map<Integer, Set<Integer>> welcomedUsersByRoom = new HashMap<>();

	public WelcomeListener(Database db, Map<Integer, String> welcomeMessagesByRoom) {
		this.db = db;
		this.welcomeMessagesByRoom = welcomeMessagesByRoom;

		loadData();

		for (Integer roomId : welcomeMessagesByRoom.keySet()) {
			if (!welcomedUsersByRoom.containsKey(roomId)) {
				welcomedUsersByRoom.put(roomId, new HashSet<>());
			}
		}
	}

	@Override
	public String name() {
		return "welcome";
	}

	@Override
	public String description() {
		return "Welcomes new users to the chat room.";
	}

	@Override
	public String helpText() {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, BotContext context) {
		int roomId = message.getRoomId();
		if (!roomHasWelcomeMessage(roomId)) {
			return null;
		}

		int userId = message.getUserId();
		Set<Integer> userIds = welcomedUsersByRoom.get(roomId);
		if (hasSeenUserBefore(userId, userIds)) {
			return null;
		}

		userIds.add(userId);
		saveData();

		List<UserInfo> userInfo;
		try {
			IRoom room = context.getRoom(roomId);
			userInfo = room.getUserInfo(Arrays.asList(userId));
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not get user info for user " + userId, e);
			return null;
		}

		if (userInfo.isEmpty()) {
			return null;
		}

		UserInfo first = userInfo.get(0);
		if (!shouldUserBeWelcomed(first)) {
			return null;
		}

		String welcomeMessage = welcomeMessagesByRoom.get(roomId);

		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(message)
			.append(welcomeMessage),
		SplitStrategy.NONE, true);
		//@formatter:on
	}

	/**
	 * Loads this listener's data from the database.
	 */
	@SuppressWarnings("unchecked")
	private void loadData() {
		Map<String, Object> map = (Map<String, Object>) db.get("welcome");
		if (map == null) {
			return;
		}

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Integer roomId = Integer.parseInt(entry.getKey());
			Map<String, Object> roomData = (Map<String, Object>) entry.getValue();
			List<Integer> userIds = (List<Integer>) roomData.get("users");

			welcomedUsersByRoom.put(roomId, new HashSet<>(userIds));
		}
	}

	/**
	 * Persists this listener's data to the database.
	 */
	private void saveData() {
		Map<String, Object> data = new HashMap<>();

		for (Map.Entry<Integer, Set<Integer>> entry : welcomedUsersByRoom.entrySet()) {
			Integer roomId = entry.getKey();
			Set<Integer> userIds = entry.getValue();

			Map<String, Object> roomInfo = new HashMap<>(1);
			roomInfo.put("users", userIds);

			data.put(Integer.toString(roomId), roomInfo);
		}

		db.set("welcome", data);
	}

	/**
	 * Determines if a room has a welcome message defined.
	 * @param roomId the room ID
	 * @return true if it has a welcome message, false if not
	 */
	private boolean roomHasWelcomeMessage(int roomId) {
		return welcomeMessagesByRoom.containsKey(roomId);
	}

	/**
	 * Determines if a user has been seen before by the bot.
	 * @param userId the user ID
	 * @param seenUsers the IDs of the users that have been seen before
	 * @return true if the user has been seen before, false if not
	 */
	private boolean hasSeenUserBefore(int userId, Set<Integer> seenUsers) {
		return seenUsers.contains(userId);
	}

	/**
	 * Determines if the given user should receive a welcome message.
	 * @param userInfo the user info
	 * @return true to give the user a welcome message, false not to
	 */
	private boolean shouldUserBeWelcomed(UserInfo userInfo) {
		return userInfo.getReputation() < 1000;
	}
}
