package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.assertEquals;
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

import org.junit.After;
import org.junit.Test;

import oakbot.Database;
import oakbot.bot.ChatAction;
import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.util.ChatCommandBuilder;
import oakbot.util.Now;
import oakbot.util.Rng;

public class FishCommandTest {
	@After
	public void after() {
		Now.restore();
		Rng.restore();
	}

	@Test
	public void loadInventories_no_data() {
		Database db = mock(Database.class);
		new FishCommand(db, "PT1S", "PT1S", "PT1S");
		verify(db).getMap("fish.caught");
	}

	@Test
	public void loadInventories_empty() {
		Database db = mock(Database.class);
		when(db.getMap("fish.caught")).thenReturn(new HashMap<String, Object>());

		new FishCommand(db, "PT1S", "PT1S", "PT1S");
		verify(db).getMap("fish.caught");
	}

	@Test
	public void loadInventories_no_entry_for_user() {
		Map<String, Object> map = new HashMap<>();
		Map<String, Object> fish = new HashMap<>();
		fish.put("Hellfish", 1);
		map.put("123456", fish);

		Database db = mock(Database.class);
		when(db.getMap("fish.caught")).thenReturn(map);

		IBot bot = mock(IBot.class);

		FishCommand command = new FishCommand(db, "PT1S", "PT1S", "PT1S");
		verify(db).getMap("fish.caught");

		//@formatter:off
		ChatActions actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.userId(789012)
			.content("inv")
		.build(), bot);
		//@formatter:on

		assertMessage(":10 🐟 *Your inventory is empty.*", actual);
	}

	@Test
	public void loadInventories_test_sort_order() {
		Map<String, Object> map = new HashMap<>();
		Map<String, Object> fish = new HashMap<>();
		fish.put("Hellfish", 1);
		fish.put("Chlam", 1);
		fish.put("Slavug", 2);
		map.put("123456", fish);

		Database db = mock(Database.class);
		when(db.getMap("fish.caught")).thenReturn(map);

		IBot bot = mock(IBot.class);

		FishCommand command = new FishCommand(db, "PT1S", "PT1S", "PT1S");
		verify(db).getMap("fish.caught");

		//@formatter:off
		ChatActions actual = command.onMessage(new ChatCommandBuilder(command)
			.messageId(10)
			.userId(123456)
			.content("inv")
		.build(), bot);
		//@formatter:on

		assertMessage(":10 🐟 *Your inventory: Slavug (x2), Chlam, Hellfish*", actual);
	}

	@Test
	public void fish_username_does_not_end_in_s() throws Exception {
		Database db = mock(Database.class);

		IBot bot = mock(IBot.class);

		Random rand = mock(Random.class);
		when(rand.nextInt(15 * 60)).thenReturn(5 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		FishCommand command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		ChatActions actual = command.onMessage(new ChatCommandBuilder(command)
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
	public void fish_too_soon() throws Exception {
		Database db = mock(Database.class);

		IBot bot = mock(IBot.class);

		Random rand = mock(Random.class);
		when(rand.nextInt(15 * 60)).thenReturn(5 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		FishCommand command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		ChatActions actual = command.onMessage(new ChatCommandBuilder(command)
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

		List<ChatAction> expected = List.of(
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
	public void fish_too_late() throws Exception {
		Database db = mock(Database.class);

		IBot bot = mock(IBot.class);

		Random rand = mock(Random.class);
		when(rand.nextInt(15 * 60)).thenReturn(5 * 60, 5 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		FishCommand command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		ChatActions actual = command.onMessage(new ChatCommandBuilder(command)
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

		List<ChatAction> expected = List.of(
			new PostMessage("🐟 *Zagreus pulls up nothing.*")
		);
		//@formatter:on

		assertEquals(expected, actual.getActions());

		verify(db, never()).set(eq("fish.caught"), any(Map.class));
	}

	@Test
	public void fish_quiver_again() throws Exception {
		Database db = mock(Database.class);

		IBot bot = mock(IBot.class);

		Random rand = mock(Random.class);
		when(rand.nextInt(15 * 60)).thenReturn(5 * 60, 10 * 60); //line will quiver after (15+5) and (15+10) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		FishCommand command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		ChatActions actual = command.onMessage(new ChatCommandBuilder(command)
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
	public void fish() throws Exception {
		Database db = mock(Database.class);

		IBot bot = mock(IBot.class);

		Random rand = mock(Random.class);
		when(rand.nextInt(15 * 60)).thenReturn(5 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		FishCommand command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		//@formatter:off
		ChatActions actual = command.onMessage(new ChatCommandBuilder(command)
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

		List<ChatAction> expected = List.of(
			new PostMessage("🐟 *Zagreus caught a **Hellfish**!*"),
			new PostMessage("https://static.wikia.nocookie.net/hades_gamepedia_en/images/3/3d/Hellfish.png")
		);
		//@formatter:on

		assertEquals(expected, actual.getActions());

		Map<String, Object> map = new HashMap<>();
		Map<String, Object> fish = new HashMap<>();
		fish.put("Hellfish", 1);
		map.put("123456", fish);

		verify(db).set("fish.caught", map);
	}

	@Test
	public void fish_status() throws Exception {
		Database db = mock(Database.class);

		IBot bot = mock(IBot.class);

		Random rand = mock(Random.class);
		when(rand.nextInt(15 * 60)).thenReturn(5 * 60); //line will quiver after (15+5) minutes
		when(rand.nextDouble()).thenReturn(0.1234);
		Rng.inject(rand);

		FishCommand command = new FishCommand(db, "PT15M", "PT30M", "PT15M");
		verify(db).getMap("fish.caught");

		/*
		 * Before throwing out the line.
		 */

		//@formatter:off
		ChatActions actual = command.onMessage(new ChatCommandBuilder(command)
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

		assertMessage(":10 🐟 *Your line hasn't caught anything yet.*", actual);

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

		List<ChatAction> expected = List.of(
			new PostMessage(":11 🐟 *Your line is quivering. Better pull it up.*")
		);
		//@formatter:on

		assertEquals(expected, actual.getActions());

		verify(db, never()).set(eq("fish.caught"), any(Map.class));
	}
}
