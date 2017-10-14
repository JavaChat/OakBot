package oakbot.task;

import oakbot.bot.Bot;

/**
 * Represents a task that runs on a regular basis.
 * @author Michael Angstadt
 */
public interface ScheduledTask {
	/**
	 * Runs the task. If an exception is thrown, the exception is logged and the
	 * task is scheduled to run again.
	 * @param bot the bot instance
	 * @throws Exception if anything bad happens
	 */
	void run(Bot bot) throws Exception;

	/**
	 * Determines how long to wait before running the task again.
	 * @return the wait time (in milliseconds) or zero to stop running the task
	 */
	long nextRun();
}
