package oakbot.command;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Interacts with the adventofcode.com website.
 * @author Michael Angstadt
 */
public class AdventOfCodeApi {
	private final CookieStore cookieStore = new BasicCookieStore();
	private final String htmlUrlTemplate = "http://adventofcode.com/%s/leaderboard/private/view/%s";
	private final String jsonUrlTemplate = htmlUrlTemplate + ".json";

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
		int year = LocalDateTime.now().getYear();
		String jsonUrl = String.format(jsonUrlTemplate, year, leaderboardId);

		JsonNode root = get(jsonUrl);
		String ownerId = root.get("owner_id").asText();
		JsonNode members = root.get("members");

		List<Player> players = new ArrayList<>();

		for (JsonNode member : members) {
			JsonNode nameNode = member.get("name");
			String name = nameNode.isNull() ? null : nameNode.asText();
			int score = member.get("local_score").asInt();
			int stars = member.get("stars").asInt();

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

			Player player = new Player(name, score, stars, completionTimes);

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
		int year = LocalDateTime.now().getYear();
		return String.format(htmlUrlTemplate, year, leaderboardId);
	}

	/**
	 * Sends a GET request and parses the response as JSON. This method is
	 * package private to allow for unit tests to inject their own JSON data.
	 * @param url the URL
	 * @return the JSON response
	 * @throws IOException if there's a network problem or a problem parsing the
	 * JSON
	 */
	JsonNode get(String url) throws IOException {
		HttpGet request = new HttpGet(url);
		try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				try (InputStream in = response.getEntity().getContent()) {
					ObjectMapper mapper = new ObjectMapper();
					return mapper.readTree(in);
				}
			}
		}
	}

	/**
	 * Parses the value of a JSON node as an {@link Instant}.
	 * @param node the JSON node (must be a string in {@link OffsetDateTime}
	 * format
	 * @return the parsed value
	 */
	private static Instant asInstant(JsonNode node) {
		String text = node.asText();

		//add a colon to the offset
		text = text.substring(0, text.length() - 2) + ":" + text.substring(text.length() - 2);

		OffsetDateTime offset = OffsetDateTime.parse(text);
		return offset.toInstant();
	}

	/**
	 * Represents a player on the leaderboard.
	 * @author Michael Angstadt
	 */
	public static class Player {
		private final String name;
		private final int score;
		private final int stars;
		private final Map<Integer, Instant[]> completionTimes;

		public Player(String name, int score, int stars, Map<Integer, Instant[]> completionTimes) {
			this.name = name;
			this.score = score;
			this.stars = stars;
			this.completionTimes = completionTimes;
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
