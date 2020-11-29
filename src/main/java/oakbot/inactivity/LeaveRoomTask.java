package oakbot.inactivity;

import oakbot.bot.Bot;
import oakbot.bot.PostMessage;
import oakbot.chat.IRoom;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Causes the bot to leave the room if the room has been inactive for some
 * time. "Home" rooms are excluded from this.
 * @author Michael Angstadt
 */
public class LeaveRoomTask implements InactivityTask {
	private static final Logger logger = Logger.getLogger(LeaveRoomTask.class.getName());

	private final Duration inactivityTime; //e.g. 3 days

	public LeaveRoomTask(long inactivityTimeMs) {
		this.inactivityTime = Duration.ofMillis(inactivityTimeMs);
	}

	@Override
	public Duration getInactivityTime(IRoom room, Bot bot) {
		//never leave home rooms
		if (bot.getRooms().isHomeRoom(room.getRoomId())) {
			return null;
		}

		return inactivityTime;
	}

	@Override
	public void run(IRoom room, Bot bot) throws Exception {
		try {
			bot.sendMessage(room.getRoomId(), new PostMessage("*quietly closes the door behind him*"));
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not post message to room " + room.getRoomId() + ".", e);
		}

		bot.leave(room.getRoomId());
	}
}
