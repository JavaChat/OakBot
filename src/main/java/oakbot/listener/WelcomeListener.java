package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.UserInfo;

import oakbot.Database;
import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Welcomes new users to the chat room.
 * @author Michael Angstadt
 */
public class WelcomeListener implements Listener {
	private static final Logger logger = LoggerFactory.getLogger(WelcomeListener.class);

	private final Database db;
	private final Map<Integer, String> welcomeMessagesByRoom;
	private final Map<Integer, Set<Integer>> welcomedUsersByRoom = new HashMap<>();
	private final int minReputation;

	public WelcomeListener(Database db, int minReputation, Map<Integer, String> welcomeMessagesByRoom) {
		this.db = db;
		this.minReputation = minReputation;
		this.welcomeMessagesByRoom = welcomeMessagesByRoom;

		loadData();
	}

	@Override
	public String name() {
		return "welcome";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Welcomes new users to the chat room.")
			.detail("If a welcome message is defined for this room, users with a reputation less than " + minReputation + " will receive a welcome message the first time they post a message.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		var roomId = message.getRoomId();
		if (!roomHasWelcomeMessage(roomId)) {
			return doNothing();
		}

		var userId = message.getUserId();
		var userIds = welcomedUsersByRoom.computeIfAbsent(roomId, k -> new HashSet<>());
		if (hasSeenUserBefore(userId, userIds)) {
			return doNothing();
		}

		userIds.add(userId);
		saveData();

		UserInfo userInfo;
		try {
			var room = bot.getRoom(roomId);
			userInfo = room.getUserInfo(userId);
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Could not get user info for user " + userId + ".");
			return doNothing();
		}

		if (userInfo == null) {
			return doNothing();
		}

		if (!shouldUserBeWelcomed(userInfo)) {
			return doNothing();
		}

		var welcomeMessage = welcomeMessagesByRoom.get(roomId);

		//@formatter:off
		return ChatActions.create(
			new PostMessage(new ChatBuilder()
				.reply(message)
				.append(welcomeMessage)
			)
			.bypassFilters(true)
		);
		//@formatter:on
	}

	/**
	 * Loads this listener's data from the database.
	 */
	@SuppressWarnings("unchecked")
	private void loadData() {
		var map = (Map<String, Object>) db.get("welcome");
		if (map == null) {
			return;
		}

		for (var entry : map.entrySet()) {
			var roomId = Integer.parseInt(entry.getKey());
			var roomData = (Map<String, Object>) entry.getValue();
			var userIds = (List<Integer>) roomData.get("users");

			welcomedUsersByRoom.put(roomId, new HashSet<>(userIds));
		}
	}

	/**
	 * Persists this listener's data to the database.
	 */
	private void saveData() {
		var data = new HashMap<String, Object>();

		for (var entry : welcomedUsersByRoom.entrySet()) {
			var roomId = entry.getKey();
			var userIds = entry.getValue();

			data.put(Integer.toString(roomId), Map.of("users", userIds));
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
		return userInfo.getReputation() < minReputation;
	}
}
