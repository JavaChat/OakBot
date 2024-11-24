package oakbot.listener.chatgpt;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import oakbot.util.Now;

/**
 * Records timestamps to determine whether a user is over-using a service and
 * should be rate-limited.
 * @author Michael Angstadt
 */
public class UsageQuota {
	private final Duration period;
	private final int requestsPerPeriod;
	private final Map<Long, List<Instant>> requestTimesByUser = new HashMap<>();

	/**
	 * @param period a period of time that it records requests for (e.g. 24
	 * hours)
	 * @param requestsPerPeriod the number of requests the user is allowed to
	 * make in the given period (e.g. 10 requests)
	 */
	public UsageQuota(Duration period, int requestsPerPeriod) {
		this.period = period;
		this.requestsPerPeriod = requestsPerPeriod;
	}

	/**
	 * No-op implementation that doesn't perform any rate-limiting.
	 * @return no-op implementation
	 */
	public static UsageQuota allowAll() {
		return new UsageQuota(null, 0);
	}

	/**
	 * Records that a request has been made.
	 * @param userId the user ID
	 */
	public void logRequest(long userId) {
		if (period == null) {
			return;
		}

		var times = getRequestTimes(userId);
		var now = Now.instant();
		times.add(now);
	}

	/**
	 * Removes the last request.
	 * @param userId the user ID
	 */
	public void removeLast(long userId) {
		if (period == null) {
			return;
		}

		var times = getRequestTimes(userId);
		if (!times.isEmpty()) {
			times.remove(times.size() - 1);
		}
	}

	/**
	 * Calculates the amount of time until the user can make another request.
	 * @param userId the user ID
	 * @return the amount of time until the user can make a request or zero if
	 * they can make a request now
	 */
	public Duration getTimeUntilUserCanMakeRequest(long userId) {
		if (period == null) {
			return Duration.ZERO;
		}

		var times = getRequestTimes(userId);
		removeOldRequestTimes(times);

		if (times.size() < requestsPerPeriod) {
			return Duration.ZERO;
		}

		var now = Now.instant();
		var earliestRequest = times.get(0);
		var canMakeRequest = earliestRequest.plus(period);
		return Duration.between(now, canMakeRequest);
	}

	private List<Instant> getRequestTimes(long userId) {
		return requestTimesByUser.computeIfAbsent(userId, key -> new LinkedList<Instant>());
	}

	private void removeOldRequestTimes(List<Instant> times) {
		var now = Now.instant();
		var it = times.iterator();

		while (it.hasNext()) {
			var instant = it.next();
			var howLongAgo = Duration.between(instant, now);
			var happenedBeforePeriod = howLongAgo.compareTo(period) >= 0;
			if (happenedBeforePeriod) {
				it.remove();
			} else {
				/*
				 * Timestamps are guaranteed to be in ascending order, so if a
				 * timestamp falls within the period, we know the rest of the
				 * timestamps in the list also fall within the period.
				 */
				break;
			}
		}
	}

	/**
	 * Gets the current number of logged requests for the given user.
	 * @param userId the user ID
	 * @return the current number of logged requests
	 */
	public int getCurrent(int userId) {
		var times = getRequestTimes(userId);
		removeOldRequestTimes(times);
		return times.size();
	}

	/**
	 * Gets the number of requests allowed per period.
	 * @return the number of requests allowed per period or zero if there is no
	 * limit
	 */
	public int getRequestsPerPeriod() {
		return requestsPerPeriod;
	}

	/**
	 * Gets the amount of time it takes for a logged request to get cleared.
	 * @return the period
	 */
	public Duration getPeriod() {
		return period;
	}
}
