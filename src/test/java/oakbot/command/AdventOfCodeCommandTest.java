package oakbot.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class AdventOfCodeCommandTest {
	private final ChatCommandBuilder chatCommandBuilder = new ChatCommandBuilder(new AdventOfCodeCommand(new HashMap<>(), "").name());

	@Test
	public void using_default_id() {
		ChatCommand message = chatCommandBuilder.build(1, 1, "");

		Map<Integer, String> leaderboardIds = new HashMap<>();
		leaderboardIds.put(1, "123456");
		AdventOfCodeCommand command = mock(leaderboardIds, "123456");

		ChatResponse response = command.onMessage(message, null);
		assertLeaderboardResponse("123456", response);
	}

	@Test
	public void no_default_id() {
		ChatCommand message = chatCommandBuilder.build(1, 1, "");

		Map<Integer, String> leaderboardIds = new HashMap<>();
		AdventOfCodeCommand command = new AdventOfCodeCommand(leaderboardIds, "") {
			@Override
			JsonNode get(String url) throws IOException {
				fail("Should not be called because no leaderboard ID was specified.");
				return null;
			}
		};

		BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), null);
		ChatResponse response = command.onMessage(message, context);
		assertEquals(":1 Please specify a leaderboard ID (e.g. /advent 123456).", response.getMessage());
	}

	@Test
	public void override_default_id() {
		ChatCommand message = chatCommandBuilder.build(1, 1, "098765");

		Map<Integer, String> leaderboardIds = new HashMap<>();
		leaderboardIds.put(1, "123456");
		AdventOfCodeCommand command = mock(leaderboardIds, "098765");

		ChatResponse response = command.onMessage(message, null);
		assertLeaderboardResponse("098765", response);
	}

	private static AdventOfCodeCommand mock(Map<Integer, String> leaderboardIds, String expectedLeaderboardId) {
		return new AdventOfCodeCommand(leaderboardIds, "") {
			@Override
			JsonNode get(String url) throws IOException {
				int year = LocalDateTime.now().getYear();
				assertEquals("http://adventofcode.com/" + year + "/leaderboard/private/view/" + expectedLeaderboardId + ".json", url);

				ObjectMapper mapper = new ObjectMapper();
				try (InputStream in = getClass().getResourceAsStream("advent-of-code-2017.json")) {
					return mapper.readTree(in);
				}
			}
		};
	}

	private static void assertLeaderboardResponse(String expectedId, ChatResponse actual) {
		int year = LocalDateTime.now().getYear();

		//@formatter:off
		String expected = 
		"Leaderboard owned by Unihedron (http://adventofcode.com/" + year + "/leaderboard/private/view/" + expectedId + ")\n" +
		"1. gzgreg - 312 (20 stars)\n" +
		"2. Unihedron - 306 (20 stars)\n" +
		"3. geisterfurz007 - 230 (20 stars)\n" +
		"4. Lazy Zefiris - 230 (20 stars)\n" +
		"5. Rishav - 227 (18 stars)\n" +
		"6. asterisk man - 205 (20 stars)\n" +
		"7. ByteCommander - 201 (18 stars)\n" +
		"8. Mike Angstadt - 124 (20 stars)\n" +
		"9. ProgramFOX - 104 (13 stars)\n" +
		"10. ArcticEcho - 102 (12 stars)\n" +
		"11. Shady_maniac - 90 (8 stars)\n" +
		"12. dSolver - 90 (12 stars)\n" +
		"13. annonymous - 38 (6 stars)\n" +
		"14. Michael Prieto - 31 (5 stars)\n" +
		"15. Simon - 26 (5 stars)\n" +
		"16. Jacob Gray - 0 (0 stars)\n";
		//@formatter:on

		assertEquals(expected, actual.getMessage());
	}
}
