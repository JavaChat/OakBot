package oakbot.command.aoc;

/**
 * Represents an Advent of Code leaderboard.
 * @param id the leaderboard ID
 * @param joinCode the leaderboard join code or null to keep it private
 * @author Michael Angstadt
 */
public record AdventOfCodeLeaderboard(String id, String joinCode) {
	public AdventOfCodeLeaderboard(String id) {
		this(id, null);
	}
}
