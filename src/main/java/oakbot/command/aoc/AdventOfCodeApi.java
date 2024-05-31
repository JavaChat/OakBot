package oakbot.command.aoc;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

		var ownerId = root.get("owner_id").asText();
		var members = root.get("members");

		var players = new ArrayList<Player>();

		for (var member : members) {
			var nameNode = member.get("name");
			var name = nameNode.isNull() ? null : nameNode.asText();
			var score = member.get("local_score").asInt();
			var stars = member.get("stars").asInt();
			var id = member.get("id").asInt();

			var completionTimes = new HashMap<Integer, Instant[]>();
			var fields = member.get("completion_day_level").fields();
			while (fields.hasNext()) {
				var field = fields.next();

				var completed = field.getValue();
				var first = JsonUtils.asEpochSecond(completed.get("1").get("get_star_ts"));
				var secondNode = completed.get("2");
				var second = (secondNode == null) ? null : JsonUtils.asEpochSecond(secondNode.get("get_star_ts"));

				var number = Integer.valueOf(field.getKey());
				completionTimes.put(number, new Instant[] { first, second });
			}

			var player = new Player(id, name, score, stars, completionTimes);

			var isOwner = member.get("id").asText().equals(ownerId);
			if (isOwner) {
				players.add(0, player);
			} else {
				players.add(player);
			}
		}

		return players;
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
	public record Player(int id, String name, int score, int stars, Map<Integer, Instant[]> completionTimes) { }
}
