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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
	private static final Logger logger = Logger.getLogger(AdventOfCode.class.getName());

	private final Duration pollingInterval;
	private final AdventOfCodeApi api;
	private final Map<Integer, String> monitoredLeaderboardByRoom;
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
	public AdventOfCode(String pollingInterval, Map<Integer, String> monitoredLeaderboardByRoom, AdventOfCodeApi api) {
		this.pollingInterval = Duration.parse(pollingInterval);
		this.monitoredLeaderboardByRoom = monitoredLeaderboardByRoom;
		this.api = api;

		Instant now = Now.instant();
		for (Integer roomId : monitoredLeaderboardByRoom.keySet()) {
			lastChecked.put(roomId, now);
		}
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

		String content = chatCommand.getContent();
		boolean displayDefaultLeaderboard = content.isEmpty();

		String leaderboardId;
		if (displayDefaultLeaderboard) {
			int roomId = chatCommand.getMessage().getRoomId();
			leaderboardId = monitoredLeaderboardByRoom.get(roomId);
			if (leaderboardId == null) {
				return reply("Please specify a leaderboard ID (e.g. " + bot.getTrigger() + name() + " 123456).", chatCommand);
			}
		} else {
			leaderboardId = content;
		}

		List<Player> players;
		try {
			players = api.getLeaderboard(leaderboardId);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e, () -> "Problem querying Advent of Code leaderboard " + leaderboardId + ". The session token might not have access to that leaderboard or the token might have expired.");
			return error("I couldn't query that leaderboard. It might not exist. Or the user that my adventofcode.com session token belongs to might not have access to that leaderboard. Or the token might have expired. Or you're trolling me: ", e, chatCommand);
		}

		String websiteUrl = api.getLeaderboardWebsite(leaderboardId);
		String leaderboardStr = buildLeaderboard(players, websiteUrl);

		ChatBuilder condensed = new ChatBuilder();
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

	private String buildLeaderboard(List<Player> players, String websiteUrl) {
		sortPlayersByScoreDescending(players);
		List<String> names = buildPlayerNameStrings(players);
		int lengthOfLongestName = lengthOfLongestName(names);
		int lengthOfHighestScore = lengthOfHighestScore(players);

		ChatBuilder cb = new ChatBuilder();
		cb.fixed().append("Leaderboard URL: ").append(websiteUrl).nl();

		int rank = 0;
		int prevScore = -1;
		int prevStars = -1;
		for (int i = 0; i < players.size(); i++) {
			Player player = players.get(i);

			/*
			 * Do not increase the rank number if two players have the same
			 * score & stars.
			 */
			if (player.getScore() != prevScore || player.getStars() != prevStars) {
				rank++;
			}

			cb.fixed();

			//output rank
			cb.append(rank).append(". ");
			if (rank < 10) {
				cb.append(' ');
			}

			//output name
			String playerName = names.get(i);
			cb.append(playerName).repeat(' ', lengthOfLongestName - playerName.length());

			//output score
			cb.append(" (score: ");
			cb.repeat(' ', lengthOfHighestScore - numberOfDigits(player.getScore()));
			cb.append(player.getScore()).append(") ");

			//output stars
			Map<Integer, Instant[]> days = player.getCompletionTimes();
			IntStream.rangeClosed(1, 25).forEach(day -> {
				Instant[] parts = days.get(day);
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

			//output star count
			cb.append(' ');
			if (player.getStars() < 10) {
				cb.append(' ');
			}
			cb.append(player.getStars()).append(" stars").nl();

			prevScore = player.getScore();
			prevStars = player.getStars();
		}

		return cb.toString();
	}

	private void sortPlayersByScoreDescending(List<Player> players) {
		players.sort((a, b) -> {
			int c = b.getScore() - a.getScore();
			if (c != 0) {
				return c;
			}

			return b.getStars() - a.getStars();
		});
	}

	private List<String> buildPlayerNameStrings(List<Player> players) {
		//@formatter:off
		return players.stream()
			.map(this::buildPlayerNameForLeaderboard)
		.collect(Collectors.toList());
		//@formatter:on
	}

	private String buildPlayerNameForLeaderboard(Player player) {
		if (player.getName() == null) {
			return "(user #" + player.getId() + ")";
		}

		/*
		 * Remove '@' symbols to prevent people from trolling by putting chat
		 * mentions in their AoC names.
		 */
		return player.getName().replace("@", "");
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
			.mapToInt(Player::getScore)
		.max().getAsInt();
		//@formatter:on

		return numberOfDigits(highestScore);
	}

	private int numberOfDigits(int number) {
		if (number == 0) {
			return 1;
		}

		int digits = 0;
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
		Month month = Now.local().getMonth();
		return month == Month.DECEMBER || month == Month.JANUARY;
	}

	@Override
	public long nextRun() {
		LocalDateTime now = Now.local();
		if (now.getMonth() != Month.DECEMBER) {
			LocalDateTime decemberFirst = LocalDateTime.of(now.getYear(), Month.DECEMBER, 1, 0, 0, 0);
			return now.until(decemberFirst, ChronoUnit.MILLIS);
		}

		return pollingInterval.toMillis();
	}

	@Override
	public void run(IBot bot) throws Exception {
		for (Map.Entry<Integer, String> entry : monitoredLeaderboardByRoom.entrySet()) {
			Integer roomId = entry.getKey();
			String leaderboardId = entry.getValue();

			List<Player> leaderboard;
			try {
				leaderboard = api.getLeaderboard(leaderboardId);
			} catch (Exception e) {
				logger.log(Level.SEVERE, e, () -> "Problem querying Advent of Code leaderboard " + leaderboardId + ". The session token might not have access to that leaderboard or the token might have expired.");
				continue;
			}

			Instant prevChecked = lastChecked.get(roomId);
			lastChecked.put(roomId, Now.instant());

			for (Player player : leaderboard) {
				sendAnnouncementMessage(player, prevChecked, roomId, bot);
			}
		}
	}

	private void sendAnnouncementMessage(Player player, Instant prevChecked, int roomId, IBot bot) throws IOException {
		for (Map.Entry<Integer, Instant[]> entry : player.getCompletionTimes().entrySet()) {
			Instant[] completionTime = entry.getValue();
			boolean justFinishedPart1 = completionTime[0].isAfter(prevChecked);
			boolean justFinishedPart2 = completionTime[1] != null && completionTime[1].isAfter(prevChecked);

			if (!justFinishedPart1 && !justFinishedPart2) {
				continue;
			}

			Integer day = entry.getKey();
			String playerName = buildPlayerNameForAnnouncements(player);

			ChatBuilder cb = new ChatBuilder().bold(playerName);
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
		if (player.getName() == null) {
			return "anonymous user #" + player.getId();
		}

		/*
		 * Remove '@' symbols to prevent people from trolling by putting chat
		 * mentions in their AoC names.
		 */
		return player.getName().replace("@", "");
	}
}
