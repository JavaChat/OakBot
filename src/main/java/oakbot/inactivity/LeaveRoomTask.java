package oakbot.inactivity;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.IRoom;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;

/**
 * Causes the bot to leave the room if the room has been inactive for some
 * time. "Home" rooms are excluded from this.
 * @author Michael Angstadt
 */
public class LeaveRoomTask implements InactivityTask {
	private static final Logger logger = LoggerFactory.getLogger(LeaveRoomTask.class);

	private final Duration inactivityTime; //e.g. 3 days

	public LeaveRoomTask(String inactivityTime) {
		this.inactivityTime = Duration.parse(inactivityTime);
	}

	@Override
	public Duration getInactivityTime(IRoom room, IBot bot) {
		//never leave home rooms
		if (bot.getHomeRooms().contains(room.getRoomId())) {
			return null;
		}

		return inactivityTime;
	}

	@Override
	public void run(IRoom room, IBot bot) throws Exception {
		try {
			bot.sendMessage(room.getRoomId(), new PostMessage("*quietly closes the door behind him*"));
		} catch (Exception e) {
			logger.atError().setCause(e).log(() -> "Could not post message to room " + room.getRoomId() + ".");
		}

		bot.leave(room.getRoomId());
	}
}
