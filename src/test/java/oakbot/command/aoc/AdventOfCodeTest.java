package oakbot.command.aoc;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatCommandBuilder;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;
import oakbot.util.MockHttpClientBuilder;
import oakbot.util.Now;

/**
 * @author Michael Angstadt
 */
public class AdventOfCodeTest {
	@After
	public void after() {
		Now.restore();
		HttpFactory.restore();
	}

	@Test
	public void using_default_id() throws Exception {
		Now.setNow(LocalDateTime.of(2017, 12, 1, 0, 0, 0));

		String aoc2017 = new Gobble(getClass(), "advent-of-code-2017.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://adventofcode.com/2017/leaderboard/private/view/123456.json")
			.responseOk(aoc2017)
		.build());
		//@formatter:on

		AdventOfCodeApi api = new AdventOfCodeApi("");

		Map<Integer, String> leaderboardIds = new HashMap<>();
		leaderboardIds.put(1, "123456");

		AdventOfCode aoc = new AdventOfCode("PT0S", leaderboardIds, api);

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(aoc)
			.messageId(1)
			.roomId(1)
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = aoc.onMessage(message, bot);
		assertLeaderboardResponse("123456", response);
	}

	@Test
	public void no_default_id() {
		Now.setNow(LocalDateTime.of(2017, 12, 1, 0, 0, 0));

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
		.build());
		//@formatter:on

		AdventOfCodeApi api = new AdventOfCodeApi("");

		Map<Integer, String> leaderboardIds = new HashMap<>();

		AdventOfCode command = new AdventOfCode("PT0S", leaderboardIds, api);

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.roomId(1)
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 Please specify a leaderboard ID (e.g. /aoc 123456).", response);
	}

	@Test
	public void override_default_id() throws Exception {
		Now.setNow(LocalDateTime.of(2017, 12, 1, 0, 0, 0));

		String aoc2017 = new Gobble(getClass(), "advent-of-code-2017.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://adventofcode.com/2017/leaderboard/private/view/098765.json")
			.responseOk(aoc2017)
		.build());
		//@formatter:on

		AdventOfCodeApi api = new AdventOfCodeApi("");

		Map<Integer, String> leaderboardIds = new HashMap<>();
		leaderboardIds.put(1, "123456");

		AdventOfCode aoc = new AdventOfCode("PT0S", leaderboardIds, api);

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(aoc)
			.messageId(1)
			.roomId(1)
			.content("098765")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = aoc.onMessage(message, bot);
		assertLeaderboardResponse("098765", response);
	}

	@Test
	public void not_active() {
		Now.setNow(LocalDateTime.of(2017, 2, 1, 0, 0, 0));

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
		.build());
		//@formatter:on

		AdventOfCodeApi api = new AdventOfCodeApi("");

		Map<Integer, String> leaderboardIds = new HashMap<>();

		AdventOfCode aoc = new AdventOfCode("PT0S", leaderboardIds, api);

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(aoc)
			.messageId(1)
			.roomId(1)
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = aoc.onMessage(message, bot);
		assertMessage(":1 This command is only active during the month of December.", response);
	}

	@Test
	public void nextRun() throws Exception {
		Now.setNow(LocalDateTime.of(2017, 12, 1, 0, 0, 0));

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
		.build());
		//@formatter:on

		AdventOfCodeApi api = new AdventOfCodeApi("");

		Map<Integer, String> leaderboardIds = new HashMap<>();

		AdventOfCode aoc = new AdventOfCode("PT15M", leaderboardIds, api);

		assertEquals(Duration.ofMinutes(15).toMillis(), aoc.nextRun());
	}

	@Test
	public void nextRun_not_december() throws Exception {
		Now.setNow(LocalDateTime.of(2017, 10, 1, 0, 0, 0));

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
		.build());
		//@formatter:on

		AdventOfCodeApi api = new AdventOfCodeApi("");

		Map<Integer, String> leaderboardIds = new HashMap<>();

		AdventOfCode aoc = new AdventOfCode("PT15M", leaderboardIds, api);

		assertTrue(aoc.nextRun() > Duration.ofMinutes(15).toMillis());
	}

	@Test
	public void announce_completions() throws Exception {
		LocalDateTime start = LocalDateTime.of(2018, 12, 11, 0, 0, 0);

		MockHttpClientBuilder mockHttp = new MockHttpClientBuilder();
		{
			Now.setNow(start);

			//REQUEST 1====================
			JsonNode root;
			try (InputStream in = getClass().getResourceAsStream("advent-of-code-2018.json")) {
				root = JsonUtils.parse(in);
			}

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));

			//REQUEST 2====================
			Now.fastForward(Duration.ofMinutes(15));

			ObjectNode user = (ObjectNode) root.get("members").get("55305").get("completion_day_level");
			ObjectNode day11 = user.objectNode();
			ObjectNode day11Part1 = day11.objectNode();
			day11Part1.put("get_star_ts", Now.instant().minus(Duration.ofMinutes(5)).getEpochSecond());
			day11.set("1", day11Part1);
			user.set("11", day11);

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));

			//REQUEST 3====================
			Now.fastForward(Duration.ofMinutes(15));

			ObjectNode day11Part2 = day11.objectNode();

			day11Part2.put("get_star_ts", Now.instant().minus(Duration.ofMinutes(5)).getEpochSecond());
			day11.set("2", day11Part2);

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));

			//REQUEST 4====================
			Now.fastForward(Duration.ofMinutes(15));

			ObjectNode day12 = user.objectNode();
			ObjectNode day12Part1 = day12.objectNode();
			ObjectNode day12Part2 = day12.objectNode();
			day12Part1.put("get_star_ts", Now.instant().minus(Duration.ofMinutes(5)).getEpochSecond());
			day12Part2.put("get_star_ts", Now.instant().minus(Duration.ofMinutes(1)).getEpochSecond());
			day12.set("1", day12Part1);
			day12.set("2", day12Part2);
			user.set("12", day12);

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));
		}

		Now.setNow(start);
		HttpFactory.inject(mockHttp.build());

		AdventOfCodeApi api = new AdventOfCodeApi("");

		Map<Integer, String> leaderboardIds = new HashMap<>();
		leaderboardIds.put(1, "123456");

		AdventOfCode aoc = new AdventOfCode("PT0S", leaderboardIds, api);

		IBot bot = mock(IBot.class);

		aoc.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		Now.fastForward(Duration.ofMinutes(15));

		aoc.run(bot);
		PostMessage expected = new PostMessage("**Unihedron** completed part 1 of day 11! \\o/");
		verify(bot).sendMessage(1, expected);

		Now.fastForward(Duration.ofMinutes(15));

		aoc.run(bot);
		expected = new PostMessage("**Unihedron** completed part 2 of day 11! \\o/");
		verify(bot).sendMessage(1, expected);

		Now.fastForward(Duration.ofDays(1));

		aoc.run(bot);
		expected = new PostMessage("**Unihedron** completed parts 1 and 2 of day 12! \\o/");
		verify(bot).sendMessage(1, expected);
	}

	@Test
	public void announce_completions_anon_user() throws Exception {
		LocalDateTime start = LocalDateTime.of(2018, 12, 11, 0, 0, 0);

		MockHttpClientBuilder mockHttp = new MockHttpClientBuilder();
		{
			Now.setNow(start);

			//REQUEST 1====================
			JsonNode root;
			try (InputStream in = getClass().getResourceAsStream("advent-of-code-2018.json")) {
				root = JsonUtils.parse(in);
			}

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));

			//REQUEST 2====================
			Now.fastForward(Duration.ofMinutes(15));

			ObjectNode user = (ObjectNode) root.get("members").get("376542").get("completion_day_level");
			ObjectNode day11 = user.objectNode();
			ObjectNode day11Part1 = day11.objectNode();
			day11Part1.put("get_star_ts", Now.instant().minus(Duration.ofMinutes(5)).getEpochSecond());
			day11.set("1", day11Part1);
			user.set("11", day11);

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));
		}

		Now.setNow(start);
		HttpFactory.inject(mockHttp.build());

		AdventOfCodeApi api = new AdventOfCodeApi("");

		Map<Integer, String> leaderboardIds = new HashMap<>();
		leaderboardIds.put(1, "123456");

		AdventOfCode aoc = new AdventOfCode("PT0S", leaderboardIds, api);

		IBot bot = mock(IBot.class);

		aoc.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		Now.fastForward(Duration.ofMinutes(15));

		aoc.run(bot);
		PostMessage expected = new PostMessage("**anonymous user #376542** completed part 1 of day 11! \\o/");
		verify(bot).sendMessage(1, expected);
	}

	@Test
	public void announce_completions_at_symbol_in_username() throws Exception {
		LocalDateTime start = LocalDateTime.of(2018, 12, 11, 0, 0, 0);

		MockHttpClientBuilder mockHttp = new MockHttpClientBuilder();
		{
			Now.setNow(start);

			//REQUEST 1====================
			JsonNode root;
			try (InputStream in = getClass().getResourceAsStream("advent-of-code-2018.json")) {
				root = JsonUtils.parse(in);
			}

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));

			//REQUEST 2====================
			Now.fastForward(Duration.ofMinutes(15));

			ObjectNode user = (ObjectNode) root.get("members").get("376568").get("completion_day_level");
			ObjectNode day11 = user.objectNode();
			ObjectNode day11Part1 = day11.objectNode();
			day11Part1.put("get_star_ts", Now.instant().minus(Duration.ofMinutes(5)).getEpochSecond());
			day11.set("1", day11Part1);
			user.set("11", day11);

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));
		}

		Now.setNow(start);
		HttpFactory.inject(mockHttp.build());

		AdventOfCodeApi api = new AdventOfCodeApi("");

		Map<Integer, String> leaderboardIds = new HashMap<>();
		leaderboardIds.put(1, "123456");

		AdventOfCode aoc = new AdventOfCode("PT0S", leaderboardIds, api);

		IBot bot = mock(IBot.class);

		aoc.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		Now.fastForward(Duration.ofMinutes(15));

		aoc.run(bot);
		PostMessage expected = new PostMessage("**Hey Michael** completed part 1 of day 11! \\o/");
		verify(bot).sendMessage(1, expected);
	}

	private static void assertLeaderboardResponse(String expectedId, ChatActions actual) {
		//@formatter:off
		String expected = 
		    "    Leaderboard URL: http://adventofcode.com/2017/leaderboard/private/view/" + expectedId + "\n" +
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
	        "    15. Hey, Michael, what's up? (score:   0) .....|.....|.....|.....|.....  0 stars";
		//@formatter:on

		assertMessage(expected, actual);
	}
}
