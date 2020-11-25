package oakbot.task;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.bot.Bot;
import oakbot.bot.PostMessage;
import oakbot.command.aoc.AdventOfCodeApi;
import oakbot.command.aoc.AdventOfCodeApi.Player;
import oakbot.util.ChatBuilder;

/**
 * Monitors Advent of Code leaderboards and posts a message when someone
 * completed a puzzle.
 * @author Michael Angstadt
 */
public class AdventOfCodeTask implements ScheduledTask {
	private static final Logger logger = Logger.getLogger(AdventOfCodeTask.class.getName());

	private final long interval;
	private final AdventOfCodeApi api;
	private final Map<Integer, String> defaultLeaderboardIds;
	private final Map<Integer, Instant> lastChecked = new HashMap<>();

	public AdventOfCodeTask(long interval, Map<Integer, String> defaultLeaderboardIds, AdventOfCodeApi api) {
		this.interval = interval;
		this.defaultLeaderboardIds = defaultLeaderboardIds;
		this.api = api;

		Instant now = Instant.now();
		for (Integer roomId : defaultLeaderboardIds.keySet()) {
			lastChecked.put(roomId, now);
		}
	}

	@Override
	public long nextRun() {
		LocalDateTime now = LocalDateTime.now();
		if (now.getMonth() != Month.DECEMBER) {
			LocalDateTime decemberFirst = LocalDateTime.of(now.getYear(), Month.DECEMBER, 1, 0, 0, 0);
			return now.until(decemberFirst, ChronoUnit.MILLIS);
		}

		return interval;
	}

	@Override
	public void run(Bot bot) throws Exception {
		for (Map.Entry<Integer, String> entry : defaultLeaderboardIds.entrySet()) {
			Integer roomId = entry.getKey();
			String leaderboardId = entry.getValue();

			List<Player> leaderboard;
			try {
				leaderboard = api.getLeaderboard(leaderboardId);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Could not get leaderboard for " + leaderboardId, e);
				continue;
			}

			Instant prevChecked = lastChecked.get(roomId);
			lastChecked.put(roomId, Instant.now());

			for (Player player : leaderboard) {
				for (Map.Entry<Integer, Instant[]> entry2 : player.getCompletionTimes().entrySet()) {
					Instant[] completionTime = entry2.getValue();
					boolean justFinishedPart1 = completionTime[0].isAfter(prevChecked);
					boolean justFinishedPart2 = completionTime[1] != null && completionTime[1].isAfter(prevChecked);

					if (!justFinishedPart1 && !justFinishedPart2) {
						continue;
					}

					Integer day = entry2.getKey();

					String playerName;
					if (player.getName() == null) {
						playerName = "anonymous user #" + player.getId();
					} else {
						/*
						 * Remove '@' symbols to prevent people from trolling by
						 * putting chat mentions in their AoC names.
						 */
						playerName = player.getName().replaceAll("@", "");
					}

					ChatBuilder cb;
					if (justFinishedPart1 && justFinishedPart2) {
						cb = new ChatBuilder() //@formatter:off
							.bold(playerName)
							.append(" completed parts 1 and 2 of day ").append(day).append("! \\o/"); //@formatter:on
					} else {
						int part = justFinishedPart1 ? 1 : 2;
						cb = new ChatBuilder() //@formatter:off
							.bold(playerName)
							.append(" completed part ").append(part).append(" of day ").append(day).append("! \\o/"); //@formatter:on
					}

					bot.sendMessage(roomId, new PostMessage(cb));
				}
			}
		}
	}
}
