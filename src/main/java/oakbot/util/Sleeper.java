package oakbot.util;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Use this class for when you want to call {@link Thread#sleep} in production,
 * but not while running unit tests.
 * @author Michael Angstadt
 */
public class Sleeper {
	private static final Logger logger = Logger.getLogger(Sleeper.class.getName());

	/**
	 * Keeps a count of how long threads have slept for if {@link #unitTest} is
	 * set to {@code true}.
	 */
	public static long timeSlept;

	/**
	 * If set to true, then {@link Thread#sleep} will NOT be called (defaults to
	 * {@code false}).
	 */
	public static boolean unitTest = false;

	/**
	 * Calls {@link Thread#sleep} if {@link #unitTest} is set to {@code false}.
	 * @param ms the amount of time to sleep (in milliseconds)
	 */
	public static void sleep(long ms) {
		if (unitTest) {
			timeSlept += ms;
			return;
		}

		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			/*
			 * This is how you're supposed to handle InterruptedExceptions.
			 * https://stackoverflow.com/q/1087475/13379
			 * https://rules.sonarsource.com/java/RSPEC-2142/
			 */
			Thread.currentThread().interrupt();
			logger.log(Level.WARNING, "Thread interrupted while sleeping.", e);
		}
	}

	/**
	 * Calls {@link Thread#sleep} if {@link #unitTest} is set to {@code false}.
	 * @param duration the amount of time to sleep
	 */
	public static void sleep(Duration duration) {
		sleep(duration.toMillis());
	}

	private Sleeper() {
		//hide constructor
	}
}
