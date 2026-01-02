package oakbot.task;

import java.time.Duration;

import oakbot.bot.IBot;
import oakbot.command.HelpDoc;

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
	void run(IBot bot) throws Exception;

	/**
	 * Determines how long to wait before running the task again.
	 * @return the wait time or null to stop running the task
	 */
	Duration nextRun();

	/**
	 * Gets the task's name to display in the help documentation.
	 * @return the name or null not to display this task in the help
	 * documentation
	 */
	default String name() {
		return null;
	}

	/**
	 * Gets the task's help documentation.
	 * @return the help documentation or null if this task does not have any
	 * help documentation
	 */
	default HelpDoc help() {
		return null;
	}
}
