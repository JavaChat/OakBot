package oakbot.command;

import static oakbot.command.Command.reply;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatBuilder;

/**
 * Displays an Advent of Code private leaderboard.
 * @author Michael Angstadt
 */
public class AdventOfCodeCommand implements Command {
	private static final Logger logger = Logger.getLogger(AdventOfCodeCommand.class.getName());

	private final ObjectMapper mapper = new ObjectMapper();
	private final Map<Integer, String> defaultLeaderboardIds;
	private final CookieStore requestCookies = new BasicCookieStore();
	private final String htmlUrlTemplate = "http://adventofcode.com/%s/leaderboard/private/view/%s";
	private final String jsonUrlTemplate = htmlUrlTemplate + ".json";

	public AdventOfCodeCommand(Map<Integer, String> defaultLeaderboardIds, String sessionToken) {
		this.defaultLeaderboardIds = defaultLeaderboardIds;

		BasicClientCookie cookie = new BasicClientCookie("session", sessionToken);
		cookie.setDomain(".adventofcode.com");
		cookie.setPath("/");
		requestCookies.addCookie(cookie);
	}

	@Override
	public String name() {
		return "advent";
	}

	@Override
	public String description() {
		return "Displays an Advent of Code private leaderboard.";
	}

	@Override
	public String helpText(String trigger) {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		if (!isActive()) {
			return reply("This command is only active during the month of December.", chatCommand);
		}

		String leaderboardId = chatCommand.getContent().trim();
		if (leaderboardId.isEmpty()) {
			leaderboardId = defaultLeaderboardIds.get(chatCommand.getMessage().getRoomId());
		}

		if (leaderboardId == null) {
			return reply("Please specify a leaderboard ID (e.g. " + context.getTrigger() + name() + " 123456).", chatCommand);
		}

		int year = LocalDateTime.now().getYear();
		String jsonUrl = String.format(jsonUrlTemplate, year, leaderboardId);

		List<Player> players = new ArrayList<>();
		Player owner = null;
		try {
			JsonNode node = get(jsonUrl);
			String ownerId = node.get("owner_id").asText();
			JsonNode members = node.get("members");
			for (JsonNode member : members) {
				JsonNode nameNode = member.get("name");
				String name = nameNode.isNull() ? null : nameNode.asText();
				int score = member.get("local_score").asInt();
				int stars = member.get("stars").asInt();

				Player player = new Player(name, score, stars);
				players.add(player);

				if (member.get("id").asText().equals(ownerId)) {
					owner = player;
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem querying Advent of Code leaderboard. The session token might not have access to that leaderboard or the token might have expired. URL: " + jsonUrl, e);

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("I couldn't query that leaderboard. The user that my adventofcode.com session token belongs to might not have access to that leaderboard. Or the token might have expired. Error message: ").code(e.getMessage())
			);
			//@formatter:on
		}

		//sort by score descending
		Collections.sort(players, (a, b) -> {
			return b.score - a.score;
		});

		//output leaderboard
		ChatBuilder cb = new ChatBuilder();
		String htmlUrl = String.format(htmlUrlTemplate, year, leaderboardId);
		if (owner == null) {
			cb.append("Leaderboard (").append(htmlUrl).append(")").nl();
		} else {
			cb.append("Leaderboard owned by ").append(owner.name).append(" (").append(htmlUrl).append(")").nl();
		}

		int rank = 1;
		for (Player player : players) {
			//@formatter:off
			cb.append(rank).append(". ")
			.append((player.name == null) ? "annonymous" : player.name)
			.append(" - ").append(player.score)
			.append(" (").append(player.stars).append(" stars)")
			.nl();
			//@formatter:on

			rank++;
		}

		return new ChatResponse(cb);
	}

	/**
	 * Queries the website for the leaderboard data. This method is package
	 * private to allow for unit tests to inject their own JSON data.
	 * @param url the URL
	 * @return the leaderboard data
	 * @throws IOException
	 */
	JsonNode get(String url) throws IOException {
		HttpGet request = new HttpGet(url);
		try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultCookieStore(requestCookies).build()) {
			try (CloseableHttpResponse response = client.execute(request)) {
				try (InputStream in = response.getEntity().getContent()) {
					return mapper.readTree(in);
				}
			}
		}
	}

	/**
	 * Determines whether this command is currently active. This method is
	 * package private so this class can be unit tested.
	 * @return true if the command is active, false if not
	 */
	boolean isActive() {
		return LocalDateTime.now().getMonth() == Month.DECEMBER;
	}

	/**
	 * Represents a player on the leaderboard.
	 * @author Michael Angstadt
	 */
	private static class Player {
		private final String name;
		private final int score;
		private final int stars;

		public Player(String name, int score, int stars) {
			this.name = name;
			this.score = score;
			this.stars = stars;
		}
	}
}
