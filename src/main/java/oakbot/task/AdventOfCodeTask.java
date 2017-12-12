package oakbot.task;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.bot.Bot;
import oakbot.bot.ChatResponse;
import oakbot.command.AdventOfCodeApi;
import oakbot.command.AdventOfCodeApi.Player;
import oakbot.util.ChatBuilder;

/**
 * Monitors Advent of Code leaderboards and posts a message when someone
 * completed a puzzle.
 * @author Michael Angstadt
 */
public class AdventOfCodeTask implements ScheduledTask {
	private static final Logger logger = Logger.getLogger(AdventOfCodeTask.class.getName());

	private final long interval = Duration.ofMinutes(10).toMillis();
	private final AdventOfCodeApi api;
	private final Map<Integer, String> defaultLeaderboardIds;
	private final Map<Integer, Instant> lastChecked = new HashMap<>();

	public AdventOfCodeTask(Map<Integer, String> defaultLeaderboardIds, AdventOfCodeApi api) {
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
		return (now.getMonth() == Month.DECEMBER) ? interval : 0;
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
					Integer day = entry2.getKey();
					Instant[] completionTime = entry2.getValue();

					if (completionTime[0].isAfter(prevChecked)) {
						//@formatter:off
						bot.sendMessage(roomId, new ChatResponse(new ChatBuilder()
							.bold(player.getName())
							.append(" completed part 1 of day ").append(day).append("! \\o/")
						));
						//@formatter:on
					}

					if (completionTime[1] != null && completionTime[1].isAfter(prevChecked)) {
						//@formatter:off
						bot.sendMessage(roomId, new ChatResponse(new ChatBuilder()
							.bold(player.getName())
							.append(" completed part 2 of day ").append(day).append("! \\o/")
						));
						//@formatter:on
					}
				}
			}
		}
	}
}
