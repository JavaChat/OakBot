package oakbot.util;

/**
 * Use this class for when you want to call {@link Thread#sleep} in production,
 * but not while running unit tests.
 * @author Michael Angstadt
 */
public class Sleeper {
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
	 * @throws InterruptedException
	 */
	public static void sleep(long ms) throws InterruptedException {
		if (unitTest) {
			timeSlept += ms;
		} else {
			Thread.sleep(ms);
		}
	}
}
