package oakbot.command.aoc;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import oakbot.bot.ChatActions;
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
class AdventOfCodeTest {
	@AfterEach
	void after() {
		Now.restore();
		HttpFactory.restore();
	}

	@Test
	void using_default_id() throws Exception {
		Now.setNow(LocalDateTime.of(2017, 12, 1, 0, 0, 0));

		var aoc2017 = new Gobble(getClass(), "advent-of-code-2017.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://adventofcode.com/2017/leaderboard/private/view/123456.json")
			.responseOk(aoc2017)
		.build());
		//@formatter:on

		var api = new AdventOfCodeApi("");

		var leaderboards = Map.of(1, new AdventOfCodeLeaderboard("123456", "join-code"));

		var aoc = new AdventOfCode("PT0S", leaderboards, api);

		//@formatter:off
		var message = new ChatCommandBuilder(aoc)
			.messageId(1)
			.roomId(1)
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = aoc.onMessage(message, bot);
		assertLeaderboardResponse("123456", "join-code", response);
	}

	@Test
	void no_default_id() {
		Now.setNow(LocalDateTime.of(2017, 12, 1, 0, 0, 0));

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
		.build());
		//@formatter:on

		var api = new AdventOfCodeApi("");

		Map<Integer, AdventOfCodeLeaderboard> leaderboards = Map.of();

		var command = new AdventOfCode("PT0S", leaderboards, api);

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.roomId(1)
		.build();
		//@formatter:on

		var bot = mock(IBot.class);
		when(bot.getTrigger()).thenReturn("/");

		var response = command.onMessage(message, bot);
		assertMessage("Please specify a leaderboard ID (e.g. /aoc 123456).", 1, response);
	}

	@Test
	void override_default_id() throws Exception {
		Now.setNow(LocalDateTime.of(2017, 12, 1, 0, 0, 0));

		var aoc2017 = new Gobble(getClass(), "advent-of-code-2017.json").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("http://adventofcode.com/2017/leaderboard/private/view/098765.json")
			.responseOk(aoc2017)
		.build());
		//@formatter:on

		var api = new AdventOfCodeApi("");

		var leaderboards = Map.of(1, new AdventOfCodeLeaderboard("123456"));

		var aoc = new AdventOfCode("PT0S", leaderboards, api);

		//@formatter:off
		var message = new ChatCommandBuilder(aoc)
			.messageId(1)
			.roomId(1)
			.content("098765")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = aoc.onMessage(message, bot);
		assertLeaderboardResponse("098765", null, response);
	}

	@Test
	void not_active() {
		Now.setNow(LocalDateTime.of(2017, 2, 1, 0, 0, 0));

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
		.build());
		//@formatter:on

		var api = new AdventOfCodeApi("");

		Map<Integer, AdventOfCodeLeaderboard> leaderboards = Map.of();

		var aoc = new AdventOfCode("PT0S", leaderboards, api);

		//@formatter:off
		var message = new ChatCommandBuilder(aoc)
			.messageId(1)
			.roomId(1)
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = aoc.onMessage(message, bot);
		assertMessage("This command is only active during the month of December.", 1, response);
	}

	@Test
	void nextRun() {
		Now.setNow(LocalDateTime.of(2017, 12, 1, 0, 0, 0));

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
		.build());
		//@formatter:on

		var api = new AdventOfCodeApi("");

		Map<Integer, AdventOfCodeLeaderboard> leaderboards = Map.of();

		var aoc = new AdventOfCode("PT15M", leaderboards, api);

		assertEquals(Duration.ofMinutes(15).toMillis(), aoc.nextRun());
	}

	@Test
	void nextRun_not_december() {
		Now.setNow(LocalDateTime.of(2017, 10, 1, 0, 0, 0));

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
		.build());
		//@formatter:on

		var api = new AdventOfCodeApi("");

		Map<Integer, AdventOfCodeLeaderboard> leaderboards = Map.of();

		var aoc = new AdventOfCode("PT15M", leaderboards, api);

		assertTrue(aoc.nextRun() > Duration.ofMinutes(15).toMillis());
	}

	@Test
	void announce_completions() throws Exception {
		var start = LocalDateTime.of(2018, 12, 11, 0, 0, 0);

		var mockHttp = new MockHttpClientBuilder();
		{
			Now.setNow(start);

			//REQUEST 1====================
			JsonNode root;
			try (var in = getClass().getResourceAsStream("advent-of-code-2018.json")) {
				root = JsonUtils.parse(in);
			}

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));

			//REQUEST 2====================
			Now.fastForward(Duration.ofMinutes(15));

			var user = (ObjectNode) root.get("members").get("55305").get("completion_day_level");
			var day11 = user.objectNode();
			var day11Part1 = day11.objectNode();
			day11Part1.put("get_star_ts", Now.instant().minus(Duration.ofMinutes(5)).getEpochSecond());
			day11.set("1", day11Part1);
			user.set("11", day11);

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));

			//REQUEST 3====================
			Now.fastForward(Duration.ofMinutes(15));

			var day11Part2 = day11.objectNode();

			day11Part2.put("get_star_ts", Now.instant().minus(Duration.ofMinutes(5)).getEpochSecond());
			day11.set("2", day11Part2);

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));

			//REQUEST 4====================
			Now.fastForward(Duration.ofMinutes(15));

			var day12 = user.objectNode();
			var day12Part1 = day12.objectNode();
			var day12Part2 = day12.objectNode();
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

		var api = new AdventOfCodeApi("");

		var leaderboards = Map.of(1, new AdventOfCodeLeaderboard("123456"));

		var aoc = new AdventOfCode("PT0S", leaderboards, api);

		var bot = mock(IBot.class);

		aoc.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		Now.fastForward(Duration.ofMinutes(15));

		aoc.run(bot);
		var expected = new PostMessage("**🎄[AoC](https://adventofcode.com/)🎄** **Unihedron** completed part 1 of day 11! \\o/");
		verify(bot).sendMessage(1, expected);

		Now.fastForward(Duration.ofMinutes(15));

		aoc.run(bot);
		expected = new PostMessage("**🎄[AoC](https://adventofcode.com/)🎄** **Unihedron** completed part 2 of day 11! \\o/");
		verify(bot).sendMessage(1, expected);

		Now.fastForward(Duration.ofDays(1));

		aoc.run(bot);
		expected = new PostMessage("**🎄[AoC](https://adventofcode.com/)🎄** **Unihedron** completed parts 1 and 2 of day 12! \\o/");
		verify(bot).sendMessage(1, expected);
	}

	@Test
	void announce_completions_anon_user() throws Exception {
		var start = LocalDateTime.of(2018, 12, 11, 0, 0, 0);

		var mockHttp = new MockHttpClientBuilder();
		{
			Now.setNow(start);

			//REQUEST 1====================
			JsonNode root;
			try (var in = getClass().getResourceAsStream("advent-of-code-2018.json")) {
				root = JsonUtils.parse(in);
			}

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));

			//REQUEST 2====================
			Now.fastForward(Duration.ofMinutes(15));

			var user = (ObjectNode) root.get("members").get("376542").get("completion_day_level");
			var day11 = user.objectNode();
			var day11Part1 = day11.objectNode();
			day11Part1.put("get_star_ts", Now.instant().minus(Duration.ofMinutes(5)).getEpochSecond());
			day11.set("1", day11Part1);
			user.set("11", day11);

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));
		}

		Now.setNow(start);
		HttpFactory.inject(mockHttp.build());

		var api = new AdventOfCodeApi("");

		var leaderboards = Map.of(1, new AdventOfCodeLeaderboard("123456"));

		var aoc = new AdventOfCode("PT0S", leaderboards, api);

		var bot = mock(IBot.class);

		aoc.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		Now.fastForward(Duration.ofMinutes(15));

		aoc.run(bot);
		var expected = new PostMessage("**🎄[AoC](https://adventofcode.com/)🎄** **anonymous user #376542** completed part 1 of day 11! \\o/");
		verify(bot).sendMessage(1, expected);
	}

	@Test
	void announce_completions_at_symbol_in_username() throws Exception {
		var start = LocalDateTime.of(2018, 12, 11, 0, 0, 0);

		var mockHttp = new MockHttpClientBuilder();
		{
			Now.setNow(start);

			//REQUEST 1====================
			JsonNode root;
			try (var in = getClass().getResourceAsStream("advent-of-code-2018.json")) {
				root = JsonUtils.parse(in);
			}

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));

			//REQUEST 2====================
			Now.fastForward(Duration.ofMinutes(15));

			var user = (ObjectNode) root.get("members").get("376568").get("completion_day_level");
			var day11 = user.objectNode();
			var day11Part1 = day11.objectNode();
			day11Part1.put("get_star_ts", Now.instant().minus(Duration.ofMinutes(5)).getEpochSecond());
			day11.set("1", day11Part1);
			user.set("11", day11);

			mockHttp.requestGet("http://adventofcode.com/2018/leaderboard/private/view/123456.json");
			mockHttp.responseOk(JsonUtils.toString(root));
		}

		Now.setNow(start);
		HttpFactory.inject(mockHttp.build());

		var api = new AdventOfCodeApi("");

		var leaderboards = Map.of(1, new AdventOfCodeLeaderboard("123456"));

		var aoc = new AdventOfCode("PT0S", leaderboards, api);

		var bot = mock(IBot.class);

		aoc.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		Now.fastForward(Duration.ofMinutes(15));

		aoc.run(bot);
		var expected = new PostMessage("**🎄[AoC](https://adventofcode.com/)🎄** **Hey Michael** completed part 1 of day 11! \\o/");
		verify(bot).sendMessage(1, expected);
	}

	private static void assertLeaderboardResponse(String expectedId, String joinCode, ChatActions actual) {
		var joinCodeStr = (joinCode == null) ? "" : System.lineSeparator() + "    Leaderboard join code: " + joinCode;

		//'@' symbols should be removed from usernames
		var expected = """
				    Leaderboard URL: http://adventofcode.com/2017/leaderboard/private/view/%s%s
				    1.  gzgreg                   (score: 312) *****|*****|.....|.....|..... 20 stars
				    2.  Unihedron                (score: 306) *****|*****|.....|.....|..... 20 stars
				    3.  geisterfurz007           (score: 230) *****|*****|.....|.....|..... 20 stars
				    3.  Lazy Zefiris             (score: 230) *****|*****|.....|.....|..... 20 stars
				    4.  Rishav                   (score: 227) *****|****.|.....|.....|..... 18 stars
				    5.  asterisk man             (score: 205) *****|*****|.....|.....|..... 20 stars
				    6.  ByteCommander            (score: 201) *****|****.|.....|.....|..... 18 stars
				    7.  Mike Angstadt            (score: 124) *****|*****|.....|.....|..... 20 stars
				    8.  ProgramFOX               (score: 104) *****|*^...|.....|.....|..... 13 stars
				    9.  ArcticEcho               (score: 102) *****|*....|.....|.....|..... 12 stars
				    10. dSolver                  (score:  90) *****|*....|.....|.....|..... 12 stars
				    11. Shady_maniac             (score:  90) **.**|.....|.....|.....|.....  8 stars
				    12. (user #238463)           (score:  38) ***..|.....|.....|.....|.....  6 stars
				    13. Michael Prieto           (score:  31) **^..|.....|.....|.....|.....  5 stars
				    14. Simon                    (score:  26) **.^.|.....|.....|.....|.....  5 stars
				    15. Hey, Michael, what's up? (score:   0) .....|.....|.....|.....|.....  0 stars
				""".formatted(expectedId, joinCodeStr).stripTrailing();

		assertMessage(expected, actual);
	}
}
