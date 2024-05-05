package oakbot.inactivity;

import java.time.Duration;

import com.github.mangstadt.sochat4j.IRoom;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;

/**
 * Causes the bot to post messages when a room has been inactive for some
 * time. "Quiet" rooms are excluded from this.
 * @author Michael Angstadt
 */
public class FillTheSilenceTask implements InactivityTask {
	//@formatter:off
	private final String[] messages = {
		"*farts*",
		"*picks nose*",
		"*reads a book*",
		"*dreams of electric sheep*",
		"*twiddles thumbs*",
		"*yawns loudly*",
		"*solves P vs NP*",
		"*doodles*",
		"*hums a song*",
		"*nods off*",
		"*fights crime*",
		"*uses java.io.File*",
		"*uses java.util.Hashtable*",
		"*opens the pod bay doors*"
	};
	//@formatter:on

	private final Duration inactivityTime;

	public FillTheSilenceTask(String inactivityTime) {
		this.inactivityTime = Duration.parse(inactivityTime);
	}

	@Override
	public Duration getInactivityTime(IRoom room, IBot bot) {
		//never post to quiet rooms
		if (bot.getQuietRooms().contains(room.getRoomId())) {
			return null;
		}

		return inactivityTime;
	}

	@Override
	public void run(IRoom room, IBot bot) throws Exception {
		var message = Command.random(messages);
		bot.sendMessage(room.getRoomId(), new PostMessage(message));
	}
}
