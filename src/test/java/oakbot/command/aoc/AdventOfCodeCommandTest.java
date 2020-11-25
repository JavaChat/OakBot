package oakbot.command.aoc;

import static oakbot.bot.ChatActionsUtils.assertMessage;
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
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class AdventOfCodeCommandTest {
	@Test
	public void using_default_id() {
		Map<Integer, String> leaderboardIds = new HashMap<>();
		leaderboardIds.put(1, "123456");
		AdventOfCodeCommand command = mock(leaderboardIds, "123456");
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.roomId(1)
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, mockBotContext());
		assertLeaderboardResponse("123456", response);
	}

	@Test
	public void no_default_id() {
		Map<Integer, String> leaderboardIds = new HashMap<>();
		AdventOfCodeApi api = new AdventOfCodeApi("") {
			@Override
			JsonNode get(String url) throws IOException {
				fail("Should not be called because no leaderboard ID was specified.");
				return null;
			}
		};
		AdventOfCodeCommand command = new AdventOfCodeCommand(leaderboardIds, api) {
			@Override
			boolean isActive() {
				return true;
			}
		};
		ChatCommand message = new ChatCommandBuilder(command).messageId(1).build();

		BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), null);
		ChatActions response = command.onMessage(message, context);
		assertMessage(":1 Please specify a leaderboard ID (e.g. /aoc 123456).", response);
	}

	@Test
	public void override_default_id() {
		Map<Integer, String> leaderboardIds = new HashMap<>();
		leaderboardIds.put(1, "123456");
		AdventOfCodeCommand command = mock(leaderboardIds, "098765");
		ChatCommand message = new ChatCommandBuilder(command) //@formatter:off
			.messageId(1)
			.content("098765")
		.build(); //@formatter:on

		ChatActions response = command.onMessage(message, mockBotContext());
		assertLeaderboardResponse("098765", response);
	}

	@Test
	public void not_active() {
		Map<Integer, String> leaderboardIds = new HashMap<>();
		AdventOfCodeApi api = new AdventOfCodeApi("") {
			@Override
			JsonNode get(String url) throws IOException {
				fail("Should not be called because the command is not active.");
				return null;
			}
		};
		AdventOfCodeCommand command = new AdventOfCodeCommand(leaderboardIds, api) {
			@Override
			boolean isActive() {
				return false;
			}
		};
		ChatCommand message = new ChatCommandBuilder(command).messageId(1).build();

		BotContext context = new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), null);
		ChatActions response = command.onMessage(message, context);
		assertMessage(":1 This command is only active during the month of December.", response);
	}

	private static AdventOfCodeCommand mock(Map<Integer, String> leaderboardIds, String expectedLeaderboardId) {
		AdventOfCodeApi api = new AdventOfCodeApi("") {
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
		return new AdventOfCodeCommand(leaderboardIds, api) {
			@Override
			boolean isActive() {
				return true;
			}
		};
	}

	private static void assertLeaderboardResponse(String expectedId, ChatActions actual) {
		int year = LocalDateTime.now().getYear();

		//@formatter:off
		String expected = 
		    "    Leaderboard URL: http://adventofcode.com/" + year + "/leaderboard/private/view/" + expectedId + "\n" +
	        "    1.  gzgreg                   (score: 312) *****|*****|.....|.....|..... 20 stars\n" +
	        "    2.  Unihedron                (score: 306) *****|*****|.....|.....|..... 20 stars\n" +
	        "    3.  geisterfurz007           (score: 230) *****|*****|.....|.....|..... 20 stars\n" +
	        "    3.  Lazy Zefiris             (score: 230) *****|*****|.....|.....|..... 20 stars\n" +
	        "    4.  Rishav                   (score: 227) *****|****.|.....|.....|..... 18 stars\n" +
	        "    5.  asterisk man             (score: 205) *****|*****|.....|.....|..... 20 stars\n" +
	        "    6.  ByteCommander            (score: 201) *****|****.|.....|.....|..... 18 stars\n" +
	        "    7.  Mike Angstadt            (score: 124) *****|*****|.....|.....|..... 20 stars\n" +
	        "    8.  ProgramFOX               (score: 104) *****|*^...|.....|.....|..... 13 stars\n" +
	        "    9.  ArcticEcho               (score: 102) *****|*....|.....|.....|..... 12 stars\n" +
	        "    10. dSolver                  (score:  90) *****|*....|.....|.....|..... 12 stars\n" +
	        "    11. Shady_maniac             (score:  90) **.**|.....|.....|.....|.....  8 stars\n" +
	        "    12. (user #238463)           (score:  38) ***..|.....|.....|.....|.....  6 stars\n" +
	        "    13. Michael Prieto           (score:  31) **^..|.....|.....|.....|.....  5 stars\n" +
	        "    14. Simon                    (score:  26) **.^.|.....|.....|.....|.....  5 stars\n" +
			     //'@' symbols should be removed
	        "    15. Hey, Michael, what's up? (score:   0) .....|.....|.....|.....|.....  0 stars\n";
		//@formatter:on

		assertMessage(expected, actual);
	}

	private static BotContext mockBotContext() {
		return new BotContext(false, "/", null, Collections.emptyList(), Collections.emptyList(), 1);
	}
}
