package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.reply;

import java.util.HashMap;
import java.util.Map;

import oakbot.Database;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;

/**
 * Allows users to change the bot's "mood" for ChatGPT interactions.
 * @author Michael Angstadt
 */
public class MoodCommand implements Command {
	private static final String MOODS_KEY = "chatgpt.moods";

	private final Database db;
	private final String defaultMood;
	private final Map<Integer, String> moodsByRoom;

	public MoodCommand(Database db, String defaultMood) {
		this.db = db;
		this.defaultMood = defaultMood;
		moodsByRoom = loadMoods();
	}

	@Override
	public String name() {
		return "mood";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Set's the bot's \"mood\" for ChatGPT interactions. One word adjectives only.")
			.detail("Moods are defined per-room.")
			.example("grumpy", "Makes the bot grumpy.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var roomId = chatCommand.getMessage().getRoomId();
		var content = chatCommand.getContent();

		if (content.isEmpty()) {
			var currentMood = getMood(roomId);
			return reply("I'm feeling a bit " + currentMood + ".", chatCommand);
		}

		if (content.indexOf(' ') >= 0) {
			return reply("Enter a one-word adjective to set my mood (e.g. happy, grumpy, nostalgic, etc).", chatCommand);
		}

		moodsByRoom.put(roomId, content);
		saveMoods();

		return reply("I am now " + content + ". :D", chatCommand);
	}

	public String getMood(int roomId) {
		return moodsByRoom.getOrDefault(roomId, defaultMood);
	}

	/**
	 * @return a mutable map containing the moods
	 */
	private Map<Integer, String> loadMoods() {
		var map = db.getMap(MOODS_KEY);
		if (map == null) {
			return new HashMap<>();
		}

		var moods = new HashMap<Integer, String>();

		for (var entry : map.entrySet()) {
			Integer roomId = Integer.valueOf(entry.getKey());
			String mood = (String) entry.getValue();
			moods.put(roomId, mood);
		}

		return moods;
	}

	private void saveMoods() {
		var map = new HashMap<>();

		for (Map.Entry<Integer, String> entry : moodsByRoom.entrySet()) {
			String key = Integer.toString(entry.getKey());
			Object value = entry.getValue();
			map.put(key, value);
		}

		db.set(MOODS_KEY, map);
	}
}
