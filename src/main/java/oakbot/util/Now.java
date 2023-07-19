package oakbot.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Use this class for when you want to call {@link Instant#now} or
 * {@link LocalDateTime#now} in production, but want to change what these
 * methods return during unit testing.
 * @author Michael Angstadt
 */
public class Now {
	private static Duration offset;

	/**
	 * Gets the current time.
	 * @return the current time
	 */
	public static Instant instant() {
		Instant now = Instant.now();
		return (offset == null) ? now : now.plus(offset);
	}

	/**
	 * Gets the current time.
	 * @return the current time
	 */
	public static LocalDateTime local() {
		LocalDateTime now = LocalDateTime.now();
		return (offset == null) ? now : now.plus(offset);
	}

	/**
	 * Sets the current time. All future invocations of {@link #instant} and
	 * {@link local} will be relative to this time.
	 * @param ts the current time
	 */
	public static void setNow(LocalDateTime ts) {
		offset = Duration.between(LocalDateTime.now(), ts);
	}

	/**
	 * Increase the current time.
	 * @param d the amount to increase the current time by
	 */
	public static void fastForward(Duration d) {
		offset = (offset == null) ? d : offset.plus(d);
	}

	/**
	 * Disables the offset and restores to present time.
	 */
	public static void restore() {
		offset = null;
	}

	private Now() {
		//hide constructor
	}
}
