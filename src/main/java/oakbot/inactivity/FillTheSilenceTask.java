package oakbot.inactivity;

import oakbot.bot.Bot;
import oakbot.bot.PostMessage;
import oakbot.chat.IRoom;
import oakbot.command.Command;

import java.time.Duration;

/**
 * Causes the bot to post messages when a room has been inactive for some
 * time. "Quiet" rooms are excluded from this.
 * @author Michael Angstadt
 */
public class FillTheSilenceTask implements InactivityTask {
	private final String[] messages = { //@formatter:off
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
		"*uses java.io.File*"
	}; //@formatter:on

	private final Duration inactivityTime; //e.g. 6 hours

	public FillTheSilenceTask(long inactivityTimeMs) {
		this.inactivityTime = Duration.ofMillis(inactivityTimeMs);
	}

	@Override
	public Duration getInactivityTime(IRoom room, Bot bot) {
		//never post to quiet rooms
		if (bot.getRooms().isQuietRoom(room.getRoomId())) {
			return null;
		}

		return inactivityTime;
	}

	@Override
	public void run(IRoom room, Bot bot) throws Exception {
		String message = Command.random(messages);
		bot.sendMessage(room.getRoomId(), new PostMessage(message));
	}
}
