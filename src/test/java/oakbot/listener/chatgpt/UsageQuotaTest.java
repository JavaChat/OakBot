package oakbot.listener.chatgpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;

import org.junit.After;
import org.junit.Test;

import oakbot.util.Now;

/**
 * @author Michael Angstadt
 */
public class UsageQuotaTest {
	@After
	public void after() {
		Now.restore();
	}

	@Test
	public void getTimeUntilUserCanMakeRequest() {
		var period = Duration.ofMinutes(10);
		var quota = new UsageQuota(period, 1);
		var userId = 1;

		//no requests logged
		var expected = Duration.ZERO;
		var actual = quota.getTimeUntilUserCanMakeRequest(userId);
		assertEquals(expected, actual);

		//request logged, quota met
		quota.logRequest(userId);

		//time until next request should be 10 minutes
		expected = period;
		actual = quota.getTimeUntilUserCanMakeRequest(userId);
		assertDurationApprox(expected, actual);

		//other user IDs shouldn't be affected
		expected = Duration.ZERO;
		actual = quota.getTimeUntilUserCanMakeRequest(userId + 1);
		assertEquals(expected, actual);

		//wait 4 minutes
		Now.fastForward(Duration.ofMinutes(4));
		expected = Duration.ofMinutes(6);
		actual = quota.getTimeUntilUserCanMakeRequest(userId);
		assertDurationApprox(expected, actual);

		//allow after waiting 6 more minutes
		Now.fastForward(Duration.ofMinutes(6));
		expected = Duration.ZERO;
		actual = quota.getTimeUntilUserCanMakeRequest(userId);
		assertEquals(expected, actual);
	}

	@Test
	public void multiple_requests_per_period() {
		var period = Duration.ofMinutes(10);
		var quota = new UsageQuota(period, 5);
		var userId = 1;

		quota.logRequest(userId);
		quota.logRequest(userId);
		quota.logRequest(userId);

		Now.fastForward(Duration.ofMinutes(8));
		quota.logRequest(userId);
		quota.logRequest(userId);

		Now.fastForward(Duration.ofMinutes(3));
		quota.logRequest(userId);
		quota.logRequest(userId);

		//1 request left
		var expected = Duration.ZERO;
		var actual = quota.getTimeUntilUserCanMakeRequest(userId);
		assertEquals(expected, actual);

		quota.logRequest(userId);

		//0 requests left, 7 minutes until next request
		actual = quota.getTimeUntilUserCanMakeRequest(userId);
		assertDurationApprox(Duration.ofMinutes(7), actual);
	}

	@Test
	public void allowAll() {
		var quota = UsageQuota.allowAll();
		var userId = 1;

		var expected = Duration.ZERO;
		var actual = quota.getTimeUntilUserCanMakeRequest(userId);
		assertEquals(expected, actual);

		quota.logRequest(userId);

		expected = Duration.ZERO;
		actual = quota.getTimeUntilUserCanMakeRequest(userId);
		assertEquals(expected, actual);

		quota.logRequest(userId);

		expected = Duration.ZERO;
		actual = quota.getTimeUntilUserCanMakeRequest(userId);
		assertEquals(expected, actual);
	}

	private static void assertDurationApprox(Duration expected, Duration actual) {
		var actualMs = actual.toMillis();
		var expectedMs = expected.toMillis();
		assertTrue("Expected " + expected + ", but was " + actual, actualMs > expectedMs - 100);
		assertTrue("Expected " + expected + ", but was " + actual, actualMs < expectedMs + 100);
	}
}
