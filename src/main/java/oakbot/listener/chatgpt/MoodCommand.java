package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.reply;

import java.util.HashMap;
import java.util.Map;

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
	private final String defaultMood;
	private Map<Integer, String> moodsByRoom = new HashMap<>();

	public MoodCommand(String defaultMood) {
		this.defaultMood = defaultMood;
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
		int roomId = chatCommand.getMessage().getRoomId();
		String content = chatCommand.getContent();

		if (content.isEmpty()) {
			String currentMood = getMood(roomId);
			return reply("I'm feeling a bit " + currentMood + ".", chatCommand);
		}

		if (content.indexOf(' ') >= 0) {
			return reply("Enter a one-word adjective to set my mood (e.g. happy, grumpy, nostalgic, etc).", chatCommand);
		}

		moodsByRoom.put(roomId, content);
		return reply("I am now " + content + ". :D", chatCommand);
	}

	public String getMood(int roomId) {
		return moodsByRoom.getOrDefault(roomId, defaultMood);
	}
}
