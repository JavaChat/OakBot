package oakbot.command.aoc;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import oakbot.command.aoc.AdventOfCodeApi.Player;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;
import oakbot.util.Now;

/**
 * @author Michael Angstadt
 */
public class AdventOfCodeApiTest {
	@After
	public void after() {
		Now.restore();
		HttpFactory.restore();
	}

	@Test
	public void getLeadeboard() throws Exception {
		Now.setNow(LocalDateTime.of(2018, 12, 1, 0, 0, 0));
		var aoc2018 = new Gobble(getClass(), "advent-of-code-2018.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json")
			.responseOk(aoc2018)
		.build());
		//@formatter:on

		var api = new AdventOfCodeApi("123456");

		var players = api.getLeaderboard("123456");
		assertEquals(10, players.size());

		var owner = players.get(0);
		assertEquals(256093, owner.id());
		assertEquals("Mike Angstadt", owner.name());
		assertEquals(18, owner.score());
		assertEquals(2, owner.stars());
		assertEquals(1, owner.completionTimes().size());
	}

	@Test
	public void getLeadeboardUrl() throws Exception {
		Now.setNow(LocalDateTime.of(2018, 12, 1, 0, 0, 0));

		var api = new AdventOfCodeApi("");
		assertEquals("http://adventofcode.com/2018/leaderboard/private/view/123456", api.getLeaderboardWebsite("123456"));
	}
}
