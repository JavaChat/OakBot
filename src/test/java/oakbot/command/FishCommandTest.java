package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import oakbot.Database;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatCommandBuilder;
import oakbot.util.Now;
import oakbot.util.Rng;

class FishCommandTest {
	@AfterEach
	void after() {
		Now.restore();
		Rng.restore();
	}

	@Test
	void loadInventories_no_data() {
		var db = mock(Database.class);
		new FishCommand(db, "PT1S", "PT1S", "PT1S");
		verify(db).getMap("fish.caught");
	}

	@Test
	void loadInventories_empty() {
		var db = mock(Database.class);
		when(db.getMap("fish.caught")).thenReturn(new HashMap<String, Object>());

		new FishCommand(db, "PT1S", "PT1S", "PT1S");
		verify(db).getMap("fish.caught");
	}

	@Test
	void loadInventories_no_entry_for_user() {
		//@formatter:off
		Map<String, Object> map = Map.of(
			"123456", Map.of(
				"Hellfish", 1
			)
		);
		//@formatter:on

		var db = mock(Database.class);
		when(db.getMap("fish.caught")).thenReturn(map);

		var bot = mock(IBot.class);

		var command = new FishCommand(db, "PT1S", "PT1S", "PT1S");
		verify(db).getMap("fish.caught");

		//@formatter:off
		var actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.userId(789012)
			.content("inv")
		.build(), bot);
		//@formatter:on

		assertMessage(":10 🐟 *Your inventory is empty.*", actual);
	}

	@Test
	void loadInventories_test_sort_order() {
		//@formatter:off
		Map<String, Object> map = Map.of(
			"123456", Map.of(
				"Hellfish", 1,
				"Chlam", 1,
				"Slavug", 2
			)
		);
		//@formatter:on

		var db = mock(Database.class);
		when(db.getMap("fish.caught")).thenReturn(map);

		var bot = mock(IBot.class);

		var command = new FishCommand(db, "PT1S", "PT1S", "PT1S");
		verify(db).getMap("fish.caught");

		//@formatter:off
		var actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.userId(123456)
			.content("inv")
		.build(), bot);
		//@formatter:on

		assertMessage(":10 🐟 *Your inventory: Slavug (x2), Chlam, Hellfish*", actual);
	}

	@Test
	void fish_username_does_not_end_in_s() throws Exception {
		var db = mock(Database.class);

		var bot = mock(IBot.class);

		var rand = mock(Random.class);
		when(rand.nextInt(15 * 60, 30 * 60)).thenReturn(20 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		var command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		var actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Michael")
		.build(), bot);
		//@formatter:on

		assertMessage("🐟 *Michael throws in a line.*", actual);

		Now.fastForward(Duration.ofMinutes(21)); //1 extra minute to ensure the time is right
		command.run(bot);
		verify(bot).sendMessage(1, new PostMessage("🐟 *Michael's line quivers.*"));

		verify(db, never()).set(eq("fish.caught"), any(Map.class));
	}

	@Test
	void fish_too_soon() throws Exception {
		var db = mock(Database.class);

		var bot = mock(IBot.class);

		var rand = mock(Random.class);
		when(rand.nextInt(15 * 60, 30 * 60)).thenReturn(20 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		var command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		var actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);
		//@formatter:on

		assertMessage("🐟 *Zagreus throws in a line.*", actual);

		Now.fastForward(Duration.ofMinutes(15));
		command.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		//@formatter:off
		actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);

		var expected = List.of(
			new PostMessage("🐟 *Zagreus pulls up nothing.*")
		);
		//@formatter:on

		assertEquals(expected, actual.getActions());

		/*
		 * Line won't quiver because it has been pulled.
		 */
		Now.fastForward(Duration.ofMinutes(6)); //1 extra minute to ensure the time is right
		command.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		verify(db, never()).set(eq("fish.caught"), any(Map.class));
	}

	@Test
	void fish_too_late() throws Exception {
		var db = mock(Database.class);

		var bot = mock(IBot.class);

		var rand = mock(Random.class);
		when(rand.nextInt(15 * 60, 30 * 60)).thenReturn(20 * 60, 20 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		var command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		var actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);
		//@formatter:on

		assertMessage("🐟 *Zagreus throws in a line.*", actual);

		Now.fastForward(Duration.ofMinutes(15));
		command.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		Now.fastForward(Duration.ofMinutes(6)); //1 extra minute to ensure the time is right
		command.run(bot);
		verify(bot).sendMessage(1, new PostMessage("🐟 *Zagreus' line quivers.*"));

		/*
		 * Waited too long.
		 */
		Now.fastForward(Duration.ofMinutes(20));

		//@formatter:off
		actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);

		var expected = List.of(
			new PostMessage("🐟 *Zagreus pulls up nothing. They weren't quick enough.*")
		);
		//@formatter:on

		assertEquals(expected, actual.getActions());

		verify(db, never()).set(eq("fish.caught"), any(Map.class));
	}

	@Test
	void fish_quiver_again() throws Exception {
		var db = mock(Database.class);

		var bot = mock(IBot.class);

		var rand = mock(Random.class);
		when(rand.nextInt(15 * 60, 30 * 60)).thenReturn(20 * 60, 25 * 60); //line will quiver after (15+5) and (15+10) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		var command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		var actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);
		//@formatter:on

		assertMessage("🐟 *Zagreus throws in a line.*", actual);

		Now.fastForward(Duration.ofMinutes(21)); //1 extra minute to ensure the time is right
		command.run(bot);
		verify(bot).sendMessage(1, new PostMessage("🐟 *Zagreus' line quivers.*"));

		Now.fastForward(Duration.ofMinutes(26)); //1 extra minute to ensure the time is right
		command.run(bot);
		verify(bot).sendMessage(1, new PostMessage("🐟 *Zagreus' line quivers.*"));

		verify(db, never()).set(eq("fish.caught"), any(Map.class));
	}

	@Test
	void fish() throws Exception {
		var db = mock(Database.class);

		var bot = mock(IBot.class);

		var rand = mock(Random.class);
		when(rand.nextInt(15 * 60, 30 * 60)).thenReturn(20 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		var command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		var actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);
		//@formatter:on

		assertMessage("🐟 *Zagreus throws in a line.*", actual);

		Now.fastForward(Duration.ofMinutes(15));
		command.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		Now.fastForward(Duration.ofMinutes(6)); //1 extra minute to ensure the time is right
		command.run(bot);
		verify(bot).sendMessage(1, new PostMessage("🐟 *Zagreus' line quivers.*"));

		//@formatter:off
		actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);

		var expected = List.of(
			new PostMessage("🐟 *Zagreus caught a **Hellfish**!*"),
			new PostMessage("https://static.wikia.nocookie.net/hades_gamepedia_en/images/3/3d/Hellfish.png")
		);
		//@formatter:on

		assertEquals(expected, actual.getActions());

		//@formatter:off
		Map<String, Object> map = Map.of(
			"123456", Map.of(
				"Hellfish", 1
			)
		);
		//@formatter:on

		verify(db).set("fish.caught", map);
	}

	@Test
	void fish_again() throws Exception {
		var db = mock(Database.class);

		var bot = mock(IBot.class);

		var rand = mock(Random.class);
		when(rand.nextInt(15 * 60, 30 * 60)).thenReturn(20 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		var command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		var actual = command.onMessage(new ChatCommandBuilder(command)
			.content("again")
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);
		//@formatter:on

		assertMessage("🐟 *Zagreus throws in a line.*", actual);

		Now.fastForward(Duration.ofMinutes(15));
		command.run(bot);
		verify(bot, never()).sendMessage(anyInt(), any(PostMessage.class));

		Now.fastForward(Duration.ofMinutes(6)); //1 extra minute to ensure the time is right
		command.run(bot);
		verify(bot).sendMessage(1, new PostMessage("🐟 *Zagreus' line quivers.*"));

		//@formatter:off
		actual = command.onMessage(new ChatCommandBuilder(command)
			.content("again")
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);

		var expected = List.of(
			new PostMessage("🐟 *Zagreus caught a **Hellfish**!*"),
			new PostMessage("https://static.wikia.nocookie.net/hades_gamepedia_en/images/3/3d/Hellfish.png"),
			new PostMessage("🐟 *Zagreus throws in a line.*")
		);
		//@formatter:on

		assertEquals(expected, actual.getActions());

		//@formatter:off
		Map<String, Object> map = Map.of(
			"123456", Map.of(
				"Hellfish", 1
			)
		);
		//@formatter:on

		verify(db).set("fish.caught", map);
	}

	@Test
	void fish_status() throws Exception {
		var db = mock(Database.class);

		var bot = mock(IBot.class);

		var rand = mock(Random.class);
		when(rand.nextInt(15 * 60, 30 * 60)).thenReturn(20 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		var command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		/*
		 * Before throwing out the line.
		 */

		//@formatter:off
		var actual = command.onMessage(new ChatCommandBuilder(command)
			.content("status")
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);
		//@formatter:on

		assertMessage(":10 🐟 *You don't have any lines out.*", actual);

		/*
		 * Throw out the line.
		 */

		//@formatter:off
		actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);
		//@formatter:on

		assertMessage("🐟 *Zagreus throws in a line.*", actual);

		/*
		 * Check status before quiver.
		 */

		//@formatter:off
		actual = command.onMessage(new ChatCommandBuilder(command)
			.content("status")
			.messageId(10)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);
		//@formatter:on

		assertMessage(":10 🐟 *Your line doesn't have anything. You should wait until it quivers.*", actual);

		/*
		 * Check status after quiver.
		 */

		Now.fastForward(Duration.ofMinutes(21));

		//@formatter:off
		actual = command.onMessage(new ChatCommandBuilder(command)
			.content("status")
			.messageId(11)
			.roomId(1)
			.userId(123456)
			.username("Zagreus")
		.build(), bot);

		var expected = List.of(
			new PostMessage(":11 🐟 *Your line is quivering. Better pull it up.*")
		);
		//@formatter:on

		assertEquals(expected, actual.getActions());

		verify(db, never()).set(eq("fish.caught"), any(Map.class));
	}
}
