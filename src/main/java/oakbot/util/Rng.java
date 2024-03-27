package oakbot.util;

import java.util.Random;

/**
 * Allows random values to be injected during unit tests.
 * @author Michael Angstadt
 */
public class Rng {
	private static final Random instance = new Random();
	private static Random random = instance;

	/**
	 * Injects a mock {@link Random} instance.
	 * @param mock the mock instance
	 */
	public static void inject(Random mock) {
		random = mock;
	}

	/**
	 * Gets a random integer value.
	 * @param startInclusive start range inclusive
	 * @param endExclusive end range exclusive
	 * @return the random value
	 */
	public static int next(int startInclusive, int endExclusive) {
		if (startInclusive == endExclusive) {
			return startInclusive;
		}

		int diff = endExclusive - startInclusive;
		return random.nextInt(diff) + startInclusive;
	}

	/**
	 * Gets a random double in range [0.0, 1.0).
	 * @return a random double
	 */
	public static double next() {
		return random.nextDouble();
	}

	/**
	 * Removes the mock instance that was set with {@link #inject}.
	 */
	public static void restore() {
		random = instance;
	}

	/**
	 * Gets the {@link Random} instance.
	 * @return the random instance
	 */
	public static Random get() {
		return random;
	}

	private Rng() {
		//hide
	}
}
