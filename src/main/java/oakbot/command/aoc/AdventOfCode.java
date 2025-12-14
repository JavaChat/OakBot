package oakbot.command.aoc;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.command.aoc.AdventOfCodeApi.Player;
import oakbot.task.ScheduledTask;
import oakbot.util.ChatBuilder;
import oakbot.util.Now;

/**
 * Monitors Advent of Code leaderboards and posts a message when someone
 * completed a puzzle.
 * @author Michael Angstadt
 */
public class AdventOfCode implements ScheduledTask, Command {
	private static final Logger logger = LoggerFactory.getLogger(AdventOfCode.class);

	private final Duration pollingInterval;
	private final AdventOfCodeApi api;
	private final Map<Integer, AdventOfCodeLeaderboard> monitoredLeaderboardByRoom;
	private final Map<Integer, Instant> lastChecked = new HashMap<>();

	/**
	 * @param pollingInterval polling interval checking the leaderboard and
	 * announcing when users complete the puzzles. AoC asks that this not be
	 * shorter than 15 minutes.
	 * @param monitoredLeaderboardByRoom the leaderboard to monitor in each room
	 * for announcing when users complete the puzzles. Also, this will be the
	 * default leaderboard that is displayed when the user does not specify a
	 * leaderboard ID. Can be empty.
	 * @param api for retrieving data from AoC
	 */
	public AdventOfCode(String pollingInterval, Map<Integer, AdventOfCodeLeaderboard> monitoredLeaderboardByRoom, AdventOfCodeApi api) {
		this.pollingInterval = Duration.parse(pollingInterval);
		this.monitoredLeaderboardByRoom = monitoredLeaderboardByRoom;
		this.api = api;

		var now = Now.instant();
		monitoredLeaderboardByRoom.keySet().forEach(roomId -> lastChecked.put(roomId, now));
	}

	@Override
	public String name() {
		return "aoc";
	}

	@Override
	public List<String> aliases() {
		return List.of("advent");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((Command)this)
			.summary("Displays scores from Advent of Code leaderboards, and announces when members complete puzzles.")
			.detail("Only enabled during the month of December.")
			.example("", "Displays the default leaderboard that is assigned to the current room.")
			.example("12345", "Displays the leaderboard with ID 12345.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		if (!isActive()) {
			return reply("This command is only active during the month of December.", chatCommand);
		}

		var content = chatCommand.getContent();
		var displayDefaultLeaderboard = content.isEmpty();

		String leaderboardId;
		String joinCode;
		if (displayDefaultLeaderboard) {
			var roomId = chatCommand.getMessage().roomId();
			var leaderboard = monitoredLeaderboardByRoom.get(roomId);
			if (leaderboard == null) {
				return reply("Please specify a leaderboard ID (e.g. " + bot.getTrigger() + name() + " 123456).", chatCommand);
			}

			leaderboardId = leaderboard.id();
			joinCode = leaderboard.joinCode();
		} else {
			leaderboardId = content;
			joinCode = null;
		}

		List<Player> players;
		try {
			players = api.getLeaderboard(leaderboardId);
		} catch (Exception e) {
			logger.atError().setCause(e).log(() -> "Problem querying Advent of Code leaderboard " + leaderboardId + ". The session token might not have access to that leaderboard or the token might have expired.");
			return error("I couldn't query that leaderboard. It might not exist. Or the user that my adventofcode.com session token belongs to might not have access to that leaderboard. Or the token might have expired. Or you're trolling me: ", e, chatCommand);
		}

		var websiteUrl = api.getLeaderboardWebsite(leaderboardId);
		var leaderboardStr = buildLeaderboard(players, websiteUrl, joinCode);

		var condensed = new ChatBuilder();
		condensed.append("Type ").code().append(bot.getTrigger()).append(name());
		if (!displayDefaultLeaderboard) {
			condensed.append(" " + leaderboardId);
		}
		condensed.code().append(" to see the leaderboard again (or go here: ").append(websiteUrl).append(")");

		//@formatter:off
		return ChatActions.create(
			new PostMessage(leaderboardStr).bypassFilters(true).condensedMessage(condensed)
		);
		//@formatter:on
	}

	private String buildLeaderboard(List<Player> players, String websiteUrl, String joinCode) {
		sortPlayersByScoreDescending(players);
		var names = buildPlayerNameStrings(players);
		var lengthOfLongestName = lengthOfLongestName(names);
		var lengthOfHighestScore = lengthOfHighestScore(players);

		var cb = new ChatBuilder();
		cb.fixedWidth();
		cb.append("Leaderboard URL: ").append(websiteUrl).nl();
		if (joinCode != null) {
			cb.append("Leaderboard join code: ").append(joinCode).nl();
		}

		var rank = 0;
		var prevScore = -1;
		var prevStars = -1;
		for (var i = 0; i < players.size(); i++) {
			var player = players.get(i);

			/*
			 * Do not increase the rank number if two players have the same
			 * score & stars.
			 */
			if (player.score() != prevScore || player.stars() != prevStars) {
				rank++;
			}

			//output rank
			cb.append(rank).append(". ");
			if (rank < 10) {
				cb.append(' ');
			}

			//output name
			var playerName = names.get(i);
			cb.append(playerName).repeat(' ', lengthOfLongestName - playerName.length());

			//output score
			cb.append(" (score: ");
			cb.repeat(' ', lengthOfHighestScore - numberOfDigits(player.score()));
			cb.append(player.score()).append(") ");

			//output stars
			appendStars(player, cb);

			//output star count
			cb.append(' ');
			if (player.stars() < 10) {
				cb.append(' ');
			}
			cb.append(player.stars()).append(" stars").nl();

			prevScore = player.score();
			prevStars = player.stars();
		}

		return cb.toString().stripTrailing();
	}

	private void appendStars(Player player, ChatBuilder cb) {
		var days = player.completionTimes();
		IntStream.rangeClosed(1, 25).forEach(day -> {
			var parts = days.get(day);
			if (parts == null) {
				//did not finish anything
				cb.append('.');
			} else if (parts[1] == null) {
				//only finished part 1
				cb.append('^');
			} else {
				//finished part 1 and 2
				cb.append('*');
			}

			if (day != 25 && day % 5 == 0) {
				cb.append('|');
			}
		});
	}

	private void sortPlayersByScoreDescending(List<Player> players) {
		players.sort((a, b) -> {
			var c = b.score() - a.score();
			if (c != 0) {
				return c;
			}

			return b.stars() - a.stars();
		});
	}

	private List<String> buildPlayerNameStrings(List<Player> players) {
		//@formatter:off
		return players.stream()
			.map(this::buildPlayerNameForLeaderboard)
		.toList();
		//@formatter:on
	}

	private String buildPlayerNameForLeaderboard(Player player) {
		if (player.name() == null) {
			return "(user #" + player.id() + ")";
		}

		/*
		 * Remove '@' symbols to prevent people from trolling by putting chat
		 * mentions in their AoC names.
		 */
		return player.name().replace("@", "");
	}

	private int lengthOfLongestName(List<String> names) {
		//@formatter:off
		return names.stream()
			.mapToInt(String::length)
		.max().getAsInt();
		//@formatter:on
	}

	private int lengthOfHighestScore(List<Player> players) {
		//@formatter:off
		int highestScore = players.stream()
			.mapToInt(Player::score)
		.max().getAsInt();
		//@formatter:on

		return numberOfDigits(highestScore);
	}

	private int numberOfDigits(int number) {
		if (number == 0) {
			return 1;
		}

		var digits = 0;
		while (number > 0) {
			number /= 10;
			digits++;
		}
		return digits;
	}

	/**
	 * Determines whether this command is currently active.
	 * @return true if the command is active, false if not
	 */
	private boolean isActive() {
		var month = Now.local().getMonth();
		return month == Month.DECEMBER || month == Month.JANUARY;
	}

	@Override
	public long nextRun() {
		var now = Now.local();
		if (now.getMonth() != Month.DECEMBER) {
			var decemberFirst = LocalDateTime.of(now.getYear(), Month.DECEMBER, 1, 0, 0, 0);
			return now.until(decemberFirst, ChronoUnit.MILLIS);
		}

		return pollingInterval.toMillis();
	}

	@Override
	public void run(IBot bot) throws Exception {
		for (var entry : monitoredLeaderboardByRoom.entrySet()) {
			var roomId = entry.getKey();
			var leaderboardId = entry.getValue().id();

			List<Player> leaderboard;
			try {
				leaderboard = api.getLeaderboard(leaderboardId);
			} catch (Exception e) {
				logger.atError().setCause(e).log(() -> "Problem querying Advent of Code leaderboard " + leaderboardId + ". The session token might not have access to that leaderboard or the token might have expired.");
				continue;
			}

			var prevChecked = lastChecked.get(roomId);
			lastChecked.put(roomId, Now.instant());

			for (var player : leaderboard) {
				sendAnnouncementMessage(player, prevChecked, roomId, bot);
			}
		}
	}

	private void sendAnnouncementMessage(Player player, Instant prevChecked, int roomId, IBot bot) throws IOException {
		for (var entry : player.completionTimes().entrySet()) {
			var completionTime = entry.getValue();
			var justFinishedPart1 = completionTime[0].isAfter(prevChecked);
			var justFinishedPart2 = completionTime[1] != null && completionTime[1].isAfter(prevChecked);

			if (!justFinishedPart1 && !justFinishedPart2) {
				continue;
			}

			var day = entry.getKey();
			var playerName = buildPlayerNameForAnnouncements(player);

			var cb = new ChatBuilder();
			cb.bold().append("🎄").link("AoC", "https://adventofcode.com/").append("🎄").bold().append(" ");
			cb.bold(playerName);
			if (justFinishedPart1 && justFinishedPart2) {
				cb.append(" completed parts 1 and 2");
			} else {
				int part = justFinishedPart1 ? 1 : 2;
				cb.append(" completed part ").append(part);
			}
			cb.append(" of day ").append(day).append("! \\o/");

			bot.sendMessage(roomId, new PostMessage(cb));
		}
	}

	private String buildPlayerNameForAnnouncements(Player player) {
		if (player.name() == null) {
			return "anonymous user #" + player.id();
		}

		/*
		 * Remove '@' symbols to prevent people from trolling by putting chat
		 * mentions in their AoC names.
		 */
		return player.name().replace("@", "");
	}
}
