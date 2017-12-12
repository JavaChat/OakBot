package oakbot.command;

import static oakbot.command.Command.reply;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.command.AdventOfCodeApi.Player;
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

		List<Player> players;
		try {
			players = api.getLeaderboard(leaderboardId);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem querying Advent of Code leaderboard " + leaderboardId + ". The session token might not have access to that leaderboard or the token might have expired.", e);

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("I couldn't query that leaderboard. It might not exist. Or the user that my adventofcode.com session token belongs to might not have access to that leaderboard. Or the token might have expired. Or you're trolling me. Error message: ").code(e.getMessage())
			);
			//@formatter:on
		}

		Player owner = players.get(0);

		//sort by score descending
		Collections.sort(players, (a, b) -> {
			return b.getScore() - a.getScore();
		});

		//output leaderboard
		ChatBuilder cb = new ChatBuilder();
		String htmlUrl = api.getLeaderboardWebsite(leaderboardId);
		cb.append("Leaderboard owned by ").append(owner.getName()).append(" (").append(htmlUrl).append(")").nl();

		int rank = 1;
		for (Player player : players) {
			//@formatter:off
			cb.append(rank).append(". ")
			.append((player.getName() == null) ? "anonymous" : player.getName())
			.append(" - ").append(player.getScore())
			.append(" (").append(player.getStars()).append(" stars)")
			.nl();
			//@formatter:on

			rank++;
		}

		return new ChatResponse(cb);
	}

	/**
	 * Determines whether this command is currently active. This method is
	 * package private so this class can be unit tested.
	 * @return true if the command is active, false if not
	 */
	boolean isActive() {
		return LocalDateTime.now().getMonth() == Month.DECEMBER;
	}
}
