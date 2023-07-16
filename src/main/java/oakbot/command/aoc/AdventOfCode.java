package oakbot.command.aoc;

import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		return Arrays.asList("advent");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((Command)this)
			.summary("Displays the scores from an Advent of Code private leaderboard. Announces when members of the leaderboard complete puzzles.")
			.detail("Only works during the month of December.")
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

		String leaderboardId = chatCommand.getContent().trim();
		boolean displayDefaultLeaderboard = leaderboardId.isEmpty();
		if (displayDefaultLeaderboard) {
			leaderboardId = monitoredLeaderboardByRoom.get(chatCommand.getMessage().getRoomId());
		}

		if (leaderboardId == null) {
			return reply("Please specify a leaderboard ID (e.g. " + bot.getTrigger() + name() + " 123456).", chatCommand);
		}

		List<Player> players;
		try {
			players = api.getLeaderboard(leaderboardId);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem querying Advent of Code leaderboard " + leaderboardId + ". The session token might not have access to that leaderboard or the token might have expired.", e);

			//@formatter:off
			return reply(new ChatBuilder()
				.append("I couldn't query that leaderboard. It might not exist. Or the user that my adventofcode.com session token belongs to might not have access to that leaderboard. Or the token might have expired. Or you're trolling me. Error message: ")
				.code(e.getMessage()),
			chatCommand);
			//@formatter:on
		}

		//sort by score descending
		Collections.sort(players, (a, b) -> {
			int c = b.getScore() - a.getScore();
			if (c != 0) {
				return c;
			}

			return b.getStars() - a.getStars();
		});

		//build names
		List<String> names = new ArrayList<>(players.size());
		int lengthOfLongestName = 0;
		for (Player player : players) {
			String name;
			if (player.getName() == null) {
				name = "(user #" + player.getId() + ")";
			} else {
				/*
				 * Remove '@' symbols to prevent people from trolling by
				 * putting chat mentions in their AoC names.
				 */
				name = player.getName().replace("@", "");
			}

			if (name.length() > lengthOfLongestName) {
				lengthOfLongestName = name.length();
			}
			names.add(name);
		}

		//find number of digits in highest score
		int digitsInHighestScore = 0;
		{
			int highestScore = -1;
			for (Player player : players) {
				if (player.getScore() > highestScore) {
					highestScore = player.getScore();
				}
			}
			digitsInHighestScore = numberOfDigits(highestScore);
		}

		//output leaderboard
		ChatBuilder cb = new ChatBuilder();
		String htmlUrl = api.getLeaderboardWebsite(leaderboardId);
		cb.fixed().append("Leaderboard URL: ").append(htmlUrl).nl();

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
			cb.append(playerName).append(' ', lengthOfLongestName - playerName.length());

			//output score
			cb.append(" (score: ");
			cb.append(' ', digitsInHighestScore - numberOfDigits(player.getScore()));
			cb.append(player.getScore()).append(") ");

			//output stars
			Map<Integer, Instant[]> days = player.getCompletionTimes();
			for (int day = 1; day <= 25; day++) {
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
			}

			//output star count
			cb.append(' ');
			if (player.getStars() < 10) {
				cb.append(' ');
			}
			cb.append(player.getStars()).append(" stars").nl();

			prevScore = player.getScore();
			prevStars = player.getStars();
		}

		ChatBuilder condensed = new ChatBuilder();
		condensed.append("Type ").code().append(bot.getTrigger()).append(name());
		if (!displayDefaultLeaderboard) {
			condensed.append(" " + leaderboardId);
		}
		condensed.code().append(" to see the leaderboard again (or go here: ").append(htmlUrl).append(")");

		//@formatter:off
		return ChatActions.create(
			new PostMessage(cb).bypassFilters(true).condensedMessage(condensed)
		);
		//@formatter:on
	}

	private static int numberOfDigits(int number) {
		if (number == 0) {
			return 1;
		}

		int digits = 0;
		while (number > 0) {
			number = number / 10;
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
				logger.log(Level.SEVERE, "Could not get leaderboard for " + leaderboardId, e);
				continue;
			}

			Instant prevChecked = lastChecked.get(roomId);
			lastChecked.put(roomId, Now.instant());

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
						playerName = player.getName().replace("@", "");
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
