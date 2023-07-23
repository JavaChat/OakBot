package oakbot.command.aoc;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.util.Http;
import oakbot.util.HttpFactory;
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
		BasicClientCookie cookie = new BasicClientCookie("session", sessionToken);
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
		String jsonUrl = jsonUrl(leaderboardId);

		JsonNode root;
		try (Http http = HttpFactory.connect()) {
			root = http.get(jsonUrl).getBodyAsJson();
		}

		String ownerId = root.get("owner_id").asText();
		JsonNode members = root.get("members");

		List<Player> players = new ArrayList<>();

		for (JsonNode member : members) {
			JsonNode nameNode = member.get("name");
			String name = nameNode.isNull() ? null : nameNode.asText();
			int score = member.get("local_score").asInt();
			int stars = member.get("stars").asInt();
			int id = member.get("id").asInt();

			Map<Integer, Instant[]> completionTimes = new HashMap<>();
			Iterator<Map.Entry<String, JsonNode>> fields = member.get("completion_day_level").fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();

				JsonNode completed = field.getValue();
				Instant first = asInstant(completed.get("1").get("get_star_ts"));
				JsonNode secondNode = completed.get("2");
				Instant second = (secondNode == null) ? null : asInstant(secondNode.get("get_star_ts"));

				Integer number = Integer.valueOf(field.getKey());
				completionTimes.put(number, new Instant[] { first, second });
			}

			Player player = new Player(id, name, score, stars, completionTimes);

			boolean isOwner = member.get("id").asText().equals(ownerId);
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
	 * Parses the value of a JSON node as an {@link Instant}.
	 * @param node the JSON node (must contain the number of seconds since the
	 * epoch)
	 * @return the parsed value
	 */
	private static Instant asInstant(JsonNode node) {
		long ts = node.asLong();
		return Instant.ofEpochSecond(ts);
	}

	/**
	 * Represents a player on the leaderboard.
	 * @author Michael Angstadt
	 */
	public static class Player {
		private final int id;
		private final String name;
		private final int score;
		private final int stars;
		private final Map<Integer, Instant[]> completionTimes;

		public Player(int id, String name, int score, int stars, Map<Integer, Instant[]> completionTimes) {
			this.id = id;
			this.name = name;
			this.score = score;
			this.stars = stars;
			this.completionTimes = completionTimes;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public int getScore() {
			return score;
		}

		public int getStars() {
			return stars;
		}

		public Map<Integer, Instant[]> getCompletionTimes() {
			return completionTimes;
		}
	}
}
