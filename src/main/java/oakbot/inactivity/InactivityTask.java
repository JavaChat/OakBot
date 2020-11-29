package oakbot.inactivity;

import oakbot.bot.Bot;
import oakbot.chat.IRoom;

import java.time.Duration;

/**
 * A task that runs if no messages have been posted to a room for a certain
 * amount of time.
 * @author Michael Angstadt
 */
public interface InactivityTask {
	/**
	 * Returns the amount of time the given room must be inactive for before
	 * the task is run.
	 * @param room the room
	 * @param bot the bot
	 * @return the amount of time or null to never run the task in this room
	 */
	Duration getInactivityTime(IRoom room, Bot bot);

	/**
	 * The code to run when the inactivity time has been reached.
	 * @param room the room
	 * @param bot the bot
	 */
	void run(IRoom room, Bot bot) throws Exception;
}
