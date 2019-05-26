package oakbot.command.aoc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oakbot.command.aoc.AdventOfCodeApi.Player;

/**
 * @author Michael Angstadt
 */
public class AdventOfCodeApiTest {
	@Test
	public void getLeadeboard() throws Exception {
		AdventOfCodeApi api = mock("123456");
		List<Player> players = api.getLeaderboard("123456");
		assertEquals(9, players.size());

		Player owner = players.get(0);
		assertEquals(256093, owner.getId());
		assertEquals("Mike Angstadt", owner.getName());
		assertEquals(18, owner.getScore());
		assertEquals(2, owner.getStars());
		assertEquals(1, owner.getCompletionTimes().size());
	}

	@Test
	public void getLeadeboardUrl() throws Exception {
		AdventOfCodeApi api = new AdventOfCodeApi("");
		int year = LocalDateTime.now().getYear();
		assertEquals("http://adventofcode.com/" + year + "/leaderboard/private/view/123456", api.getLeaderboardWebsite("123456"));
	}

	private static AdventOfCodeApi mock(String expectedLeaderboardId) {
		return new AdventOfCodeApi("") {
			@Override
			JsonNode get(String url) throws IOException {
				int year = LocalDateTime.now().getYear();
				assertEquals("http://adventofcode.com/" + year + "/leaderboard/private/view/" + expectedLeaderboardId + ".json", url);

				ObjectMapper mapper = new ObjectMapper();
				try (InputStream in = getClass().getResourceAsStream("advent-of-code-2018.json")) {
					return mapper.readTree(in);
				}
			}
		};
	}
}
