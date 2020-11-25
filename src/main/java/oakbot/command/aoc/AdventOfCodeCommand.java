package oakbot.command.aoc;

import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.command.aoc.AdventOfCodeApi.Player;
import oakbot.util.ChatBuilder;

/**
 * Displays an Advent of Code private leaderboard.
 * @author Michael Angstadt
 */
public class AdventOfCodeCommand implements Command {
	private static final Logger logger = Logger.getLogger(AdventOfCodeCommand.class.getName());

	private final Map<Integer, String> defaultLeaderboardIds;
	private final AdventOfCodeApi api;

	public AdventOfCodeCommand(Map<Integer, String> defaultLeaderboardIds, AdventOfCodeApi api) {
		this.defaultLeaderboardIds = defaultLeaderboardIds;
		this.api = api;
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
		return new HelpDoc.Builder(this)
			.summary("Displays the scores from an Advent of Code private leaderboard.")
			.detail("Only works during the month of December.")
			.example("", "Displays the default leaderboard that is assigned to the current room.")
			.example("12345", "Displays the leaderboard with ID 12345.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, BotContext context) {
		if (!isActive()) {
			return reply("This command is only active during the month of December.", chatCommand);
		}

		String leaderboardId = chatCommand.getContent().trim();
		boolean displayDefaultLeaderboard = leaderboardId.isEmpty();
		if (displayDefaultLeaderboard) {
			leaderboardId = defaultLeaderboardIds.get(chatCommand.getMessage().getRoomId());
		}

		if (leaderboardId == null) {
			return reply("Please specify a leaderboard ID (e.g. " + context.getTrigger() + name() + " 123456).", chatCommand);
		}

		List<Player> players;
		try {
			players = api.getLeaderboard(leaderboardId);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem querying Advent of Code leaderboard " + leaderboardId + ". The session token might not have access to that leaderboard or the token might have expired.", e);

			//@formatter:off
			return post(new ChatBuilder()
				.reply(chatCommand)
				.append("I couldn't query that leaderboard. It might not exist. Or the user that my adventofcode.com session token belongs to might not have access to that leaderboard. Or the token might have expired. Or you're trolling me. Error message: ")
				.code(e.getMessage())
			);
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
				name = player.getName().replaceAll("@", "");
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
		int prevScore = -1, prevStars = -1;
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
		condensed.append("Type ").code().append(context.getTrigger()).append(name());
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
	 * Determines whether this command is currently active. This method is
	 * package private so this class can be unit tested.
	 * @return true if the command is active, false if not
	 */
	boolean isActive() {
		Month month = LocalDateTime.now().getMonth();
		return month == Month.DECEMBER || month == Month.JANUARY;
	}
}
