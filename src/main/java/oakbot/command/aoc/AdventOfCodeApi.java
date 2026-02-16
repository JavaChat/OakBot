package oakbot.command.aoc;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;
import oakbot.util.Now;

/**
 * Interacts with the adventofcode.com website.
 * @author Michael Angstadt
 */
public class AdventOfCodeApi {
	private final CookieStore cookieStore = new BasicCookieStore();

	/**
	 * @param sessionToken the session token that's needed to query the API.
	 * This value can be retrieved from your own Advent of Code login--just open
	 * the site and look at your browser cookies. This class will only be able
	 * to query the leaderboards that your user account has access to.
	 */
	public AdventOfCodeApi(String sessionToken) {
		var cookie = new BasicClientCookie("session", sessionToken);
		cookie.setDomain(".adventofcode.com");
		cookie.setPath("/");
		cookieStore.addCookie(cookie);
	}

	/**
	 * Gets a leaderboard.
	 * @param leaderboardId the leaderboard ID
	 * @return the players on the leaderboard. The first item in the returned
	 * list is the owner of the leaderboard
	 * @throws IOException if there's a problem getting the leaderboard
	 */
	public List<Player> getLeaderboard(String leaderboardId) throws IOException {
		var jsonUrl = jsonUrl(leaderboardId);

		JsonNode root;
		try (var http = HttpFactory.connect(cookieStore)) {
			root = http.get(jsonUrl).getBodyAsJson();
		}

		var ownerId = root.get("owner_id").asInt();

		//@formatter:off
		return root.get("members").valueStream()
			.map(this::parsePlayer)
			.sorted((a, b) -> moveOwnerToBeginning(a, b, ownerId))
		.collect(Collectors.toCollection(ArrayList::new)); //return a mutable list so it can be sorted later
		//@formatter:on
	}

	private Player parsePlayer(JsonNode node) {
		var nameNode = node.get("name");
		var name = nameNode.isNull() ? null : nameNode.asText();
		var score = node.get("local_score").asInt();
		var stars = node.get("stars").asInt();
		var id = node.get("id").asInt();

		//@formatter:off
		var completionTimes = node.get("completion_day_level").propertyStream().collect(Collectors.toMap(
			field -> Integer.valueOf(field.getKey()),
			field -> {
				var completed = field.getValue();

				var part1Node = completed.get("1");
				var part1 = JsonUtils.asEpochSecond(part1Node.get("get_star_ts"));

				var part2Node = completed.get("2");
				var part2 = (part2Node == null) ? null : JsonUtils.asEpochSecond(part2Node.get("get_star_ts"));

				return new Instant[] { part1, part2 };
			}
		));
		//@formatter:on

		return new Player(id, name, score, stars, completionTimes);
	}

	private int moveOwnerToBeginning(Player a, Player b, int ownerId) {
		if (a.id == ownerId) {
			return -1;
		}
		if (b.id == ownerId) {
			return 1;
		}
		return 0;
	}

	/**
	 * Gets the website URL of a leaderboard.
	 * @param leaderboardId the leaderboard ID
	 * @return the URL
	 */
	public String getLeaderboardWebsite(String leaderboardId) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("http")
			.setHost("adventofcode.com")
			.setPathSegments(Integer.toString(Now.local().getYear()), "leaderboard", "private", "view", leaderboardId)
		.toString();
		//@formatter:on
	}

	private String jsonUrl(String leaderboardId) {
		return getLeaderboardWebsite(leaderboardId) + ".json";
	}

	/**
	 * Represents a player on the leaderboard.
	 * @author Michael Angstadt
	 */
	public record Player(int id, String name, int score, int stars, Map<Integer, Instant[]> completionTimes) {
	}
}
