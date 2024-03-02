package oakbot.command;

import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableInt;

import oakbot.Database;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.task.ScheduledTask;
import oakbot.util.ChatBuilder;
import oakbot.util.Now;
import oakbot.util.Rng;

/**
 * Allows user to catch fish.
 * @author Michael Angstadt
 */
public class FishCommand implements Command, ScheduledTask {
	private static final int MAX_QUIVERS = 3;

	//@formatter:off
	private static final List<Fish> allFish = new ArrayList<>(List.of(
		//https://hades.fandom.com/wiki/Fishing
		new Fish("Hellfish", 0.9, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/3/3d/Hellfish.png"),
		new Fish("Knucklehead", 0.5, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/9/94/Knucklehead.png"),
		new Fish("Scyllascion", 0.2, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/0/0a/Scyllascion.png"),
		
		new Fish("Slavug", 0.9, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/4/40/Slavug.png"),
		new Fish("Chrustacean", 0.5, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/5/50/Chrustacean.png"),
		new Fish("Flameater", 0.2, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/d/d7/Flameater.png"),
		
		new Fish("Chlam", 0.9, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/0/02/Chlam.png"),
		new Fish("Charp", 0.5, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/a/a9/Charp.png"),
		new Fish("Seamare", 0.2, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/1/11/Seamare.png"),
		
		new Fish("Gupp", 0.9, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/4/4c/Gupp.png"),
		new Fish("Scuffer", 0.5, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/2/27/Scuffer.png"),
		new Fish("Stonewhal", 0.2, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/f/f2/Stonewhal.png"),
		
		new Fish("Mati", 0.2, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/8/85/Mati.png"),
		new Fish("Projelly", 0.1, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/2/2b/Projelly.png"),
		new Fish("Voidskate", 0.05, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/1/12/Voidskate.png"),
		
		new Fish("Trout", 0.1, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/6/66/Trout.png"),
		new Fish("Bass", 0.05, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/b/bd/Bass.png"),
		new Fish("Sturgeon", 0.01, "https://static.wikia.nocookie.net/hades_gamepedia_en/images/4/42/Sturgeon.png")
	));
	//@formatter:on

	private final Database db;
	private final Map<Integer, Map<Integer, PendingCatch>> currentlyFishingByRoom = new HashMap<>();
	private final Map<Integer, Inventory> inventoryByUser;

	private final Duration minTimeUntilQuiver;
	private final Duration maxTimeUntilQuiver;

	private final Duration timeUserHasToCatchFish;

	public FishCommand(Database db, String minTimeUntilQuiver, String maxTimeUntilQuiver, String timeUserHasToCatchFish) {
		this.db = db;
		this.minTimeUntilQuiver = Duration.parse(minTimeUntilQuiver);
		this.maxTimeUntilQuiver = Duration.parse(maxTimeUntilQuiver);
		this.timeUserHasToCatchFish = Duration.parse(timeUserHasToCatchFish);
		inventoryByUser = loadInventories();
	}

	@Override
	public String name() {
		return "fish";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((Command)this)
			.summary("Allows users to fish 🐟.")
			.detail("The fish are from the game \"Hades\".")
			.example("", "Throws in or pulls up the fishing line. Users must wait until their line quivers before pulling up their line in order to get a fish.")
			.example("inv", "Displays the user's inventory of caught fish.")
			.example("status", "Displays the status of the user's fishing line.")
			.example("release bass", "Releases a fish back into the wild.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		List<String> args = chatCommand.getContentAsArgs();
		if (!args.isEmpty()) {
			String subCommand = args.get(0);

			if ("inv".equalsIgnoreCase(subCommand)) {
				return handleInvCommand(chatCommand);
			}

			if ("status".equalsIgnoreCase(subCommand)) {
				return handleStatusCommand(chatCommand);
			}

			if ("release".equalsIgnoreCase(subCommand)) {
				String fishName = (args.size() < 2) ? null : args.get(1);
				return handleReleaseCommand(fishName, chatCommand);
			}

			return reply(fishMessage("Unknown fish command."), chatCommand);
		}

		return throwOrPullLine(chatCommand);
	}

	private ChatActions handleInvCommand(ChatCommand chatCommand) {
		int userId = chatCommand.getMessage().getUserId();
		Inventory inv = inventoryByUser.get(userId);

		String message;
		if (inv == null || inv.isEmpty()) {
			message = "Your inventory is empty.";
		} else {
			//@formatter:off
			message = "Your inventory: " + inv.map.entrySet().stream()
				.sorted((a, b) -> {
					//sort by quantity descending
					int c = b.getValue().compareTo(a.getValue());
					if (c != 0) {
						return c;
					}
					
					//then sort by fish name ascending
					return a.getKey().name.compareTo(b.getKey().name);
				})
				.map(entry -> {
					String fish = entry.getKey().name;
					MutableInt count = entry.getValue();
					if (count.intValue() == 1) {
						return fish;
					} else {
						return fish + " (x" + count + ")";
					}
				})
			.collect(Collectors.joining(", "));
			//@formatter:on
		}

		return reply(fishMessage(message), chatCommand);
	}

	private ChatActions handleStatusCommand(ChatCommand chatCommand) {
		int roomId = chatCommand.getMessage().getRoomId();
		int userId = chatCommand.getMessage().getUserId();
		Map<Integer, PendingCatch> pendingCatchesInThisRoom = getPendingCatchesInRoom(roomId);
		PendingCatch pendingCatch = pendingCatchesInThisRoom.get(userId);

		if (pendingCatch == null) {
			return reply(fishMessage("You don't have any lines out."), chatCommand);
		}

		Duration sinceLineQuivered = Duration.between(pendingCatch.time, Now.instant());
		boolean currentlyQuivering = (!sinceLineQuivered.isNegative() && sinceLineQuivered.compareTo(timeUserHasToCatchFish) < 0);
		String message;
		if (currentlyQuivering) {
			message = "Your line is quivering. Better pull it up.";
		} else {
			message = "Your line doesn't have anything. You should wait until it quivers.";
		}

		return reply(fishMessage(message), chatCommand);
	}

	private ChatActions handleReleaseCommand(String fishName, ChatCommand chatCommand) {
		int userId = chatCommand.getMessage().getUserId();
		String username = chatCommand.getMessage().getUsername();
		Inventory inv = inventoryByUser.get(userId);

		if (fishName == null) {
			return reply(fishMessage("Specify a fish to release."), chatCommand);
		}

		Fish fish = Fish.findOrNull(fishName);
		if (fish == null || inv == null || !inv.has(fish)) {
			return reply(fishMessage("You don't have any of that fish."), chatCommand);
		}

		inv.remove(fish);
		saveInventories();

		//@formatter:off
		return post(fishMessage(new ChatBuilder()
			.append(username).append(" releases a ").bold(fish.name).append(" back into the wild.")
		));
		//@formatter:on
	}

	private ChatActions throwOrPullLine(ChatCommand chatCommand) {
		int roomId = chatCommand.getMessage().getRoomId();
		int userId = chatCommand.getMessage().getUserId();
		String username = chatCommand.getMessage().getUsername();
		Map<Integer, PendingCatch> pendingCatchesInThisRoom = getPendingCatchesInRoom(roomId);

		PendingCatch pendingCatch = pendingCatchesInThisRoom.remove(userId);

		if (pendingCatch == null) {
			pendingCatchesInThisRoom.put(userId, new PendingCatch(username));
			return post(fishMessage(username + " throws in a line."));
		}

		Duration sinceLineQuivered = Duration.between(pendingCatch.time, Now.instant());
		boolean tooSoon = sinceLineQuivered.isNegative();
		if (tooSoon) {
			return post(fishMessage(username + " pulls up nothing."));
		}

		boolean tooLate = (sinceLineQuivered.compareTo(timeUserHasToCatchFish) >= 0);
		if (tooLate) {
			return post(fishMessage(username + " pulls up nothing. They weren't quick enough."));
		}

		Inventory inv = inventoryByUser.computeIfAbsent(userId, key -> new Inventory());
		inv.add(pendingCatch.fish);
		saveInventories();

		ChatActions actions = new ChatActions();
		String word = (inv.count(pendingCatch.fish) > 1) ? "another" : "a";

		//@formatter:off
		actions.addAction(new PostMessage(fishMessage(new ChatBuilder()
			.append(username).append(" caught ").append(word).append(" ").bold(pendingCatch.fish.name).append("!")
		)));
		//@formatter:on

		if (pendingCatch.fish.imageUrl != null) {
			actions.addAction(new PostMessage(pendingCatch.fish.imageUrl));
		}

		return actions;
	}

	private Map<Integer, PendingCatch> getPendingCatchesInRoom(int roomId) {
		return currentlyFishingByRoom.computeIfAbsent(roomId, k -> new HashMap<Integer, PendingCatch>());
	}

	@Override
	public void run(IBot bot) throws Exception {
		for (Map.Entry<Integer, Map<Integer, PendingCatch>> entry : currentlyFishingByRoom.entrySet()) {
			int roomId = entry.getKey();
			Map<Integer, PendingCatch> pendingCatchesInRoom = entry.getValue();
			checkFishingLines(roomId, pendingCatchesInRoom, bot);
		}
	}

	private void checkFishingLines(int roomId, Map<Integer, PendingCatch> pendingCatchesInRoom, IBot bot) throws IOException {
		List<Integer> userIdsToRemove = new ArrayList<>();
		Instant now = Now.instant();

		for (Map.Entry<Integer, PendingCatch> entry : pendingCatchesInRoom.entrySet()) {
			int userId = entry.getKey();
			PendingCatch pendingCatch = entry.getValue();

			if (pendingCatch.userWarned) {
				Duration sinceQuiver = Duration.between(pendingCatch.time, now);
				boolean fishGotAway = (sinceQuiver.compareTo(timeUserHasToCatchFish) > 0);
				if (fishGotAway) {
					if (pendingCatch.timesWarned < MAX_QUIVERS) {
						/*
						 * User must wait until the line quivers again.
						 */
						pendingCatch.resetTime();
					} else {
						/*
						 * Line will stop quivering. User will have to throw
						 * their line back in.
						 */
						userIdsToRemove.add(userId);
					}
				}
				continue;
			}

			boolean fishSnagged = pendingCatch.time.isBefore(now);
			if (fishSnagged) {
				pendingCatch.userWarned = true;
				pendingCatch.timesWarned++;

				PostMessage message = new PostMessage(fishMessage(possessive(pendingCatch.username) + " line quivers."));
				bot.sendMessage(roomId, message);
				continue;
			}
		}

		userIdsToRemove.forEach(pendingCatchesInRoom::remove);
	}

	private String fishMessage(CharSequence message) {
		//@formatter:off
		return new ChatBuilder()
			.append("🐟 ")
			.italic()
			.append(message)
			.italic()
		.toString();
		//@formatter:on
	}

	private String possessive(String s) {
		return s + (s.endsWith("s") ? "'" : "'s");
	}

	@Override
	public long nextRun() {
		return Duration.ofMinutes(1).toMillis();
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, Inventory> loadInventories() {
		Map<Integer, Inventory> inventories = new HashMap<>();

		Map<String, Object> fishesByUser = db.getMap("fish.caught");
		if (fishesByUser != null) {
			for (Map.Entry<String, Object> userFishes : fishesByUser.entrySet()) {
				int userId = Integer.parseInt(userFishes.getKey());
				Map<String, Integer> fishes = (Map<String, Integer>) userFishes.getValue();

				Inventory inv = new Inventory();
				for (Map.Entry<String, Integer> fishesEntry : fishes.entrySet()) {
					Fish fish = Fish.find(fishesEntry.getKey());
					Integer count = fishesEntry.getValue();
					inv.set(fish, count);
				}

				inventories.put(userId, inv);
			}
		}

		return inventories;
	}

	private void saveInventories() {
		Map<String, Object> fishesByUser = new HashMap<>();
		for (Map.Entry<Integer, Inventory> entry : inventoryByUser.entrySet()) {
			int userId = entry.getKey();
			Inventory inv = entry.getValue();

			Map<String, Integer> fishes = new HashMap<>();
			for (Map.Entry<Fish, MutableInt> fish : inv.map.entrySet()) {
				fishes.put(fish.getKey().name, fish.getValue().toInteger());
			}

			fishesByUser.put(userId + "", fishes);
		}

		db.set("fish.caught", fishesByUser);
	}

	private class PendingCatch {
		private final String username;
		private final Fish fish;
		private Instant time;
		private boolean userWarned;
		private int timesWarned;

		public PendingCatch(String username) {
			this.username = username;

			resetTime();
			fish = Fish.randomByRarity();
		}

		public void resetTime() {
			int seconds = Rng.next((int) minTimeUntilQuiver.toSeconds(), (int) maxTimeUntilQuiver.toSeconds());
			time = Now.instant().plus(Duration.ofSeconds(seconds));
			userWarned = false;
		}
	}

	private static class Inventory {
		private final Map<Fish, MutableInt> map = new HashMap<>();

		public void add(Fish fish) {
			MutableInt count = map.computeIfAbsent(fish, k -> new MutableInt());
			count.increment();
		}

		public void remove(Fish fish) {
			MutableInt count = map.get(fish);
			if (count != null) {
				if (count.intValue() == 1) {
					map.remove(fish);
				} else {
					count.decrement();
				}
			}
		}

		public void set(Fish fish, int count) {
			map.put(fish, new MutableInt(count));
		}

		public int count(Fish fish) {
			MutableInt count = map.get(fish);
			return (count == null) ? 0 : count.intValue();
		}

		public boolean has(Fish fish) {
			return count(fish) > 0;
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}
	}

	private static class Fish {
		private final String name, imageUrl;
		private final double rarity;

		public Fish(String name) {
			this(name, 0.0);
		}

		public Fish(String name, double rarity) {
			this(name, rarity, null);
		}

		public Fish(String name, double rarity, String imageUrl) {
			if (rarity < 0.0 || rarity >= 1.0) {
				throw new IllegalArgumentException("Rarity must be in rage [0.0, 1.0).");
			}

			this.name = name;
			this.imageUrl = imageUrl;
			this.rarity = rarity;
		}

		public static Fish findOrNull(String name) {
			//@formatter:off
			return allFish.stream()
				.filter(f -> name.equalsIgnoreCase(f.name))
			.findFirst().orElse(null);
			//@formatter:on
		}

		public static Fish find(String name) {
			Fish fish = findOrNull(name);

			if (fish == null) {
				fish = new Fish(name);
				allFish.add(fish);
			}

			return fish;
		}

		public static Fish randomByRarity() {
			double total = allFish.stream().mapToDouble(fish -> fish.rarity).sum();
			double randValue = Rng.next() * total;
			double cur = 0.0;
			for (Fish fish : allFish) {
				cur += fish.rarity;
				if (randValue < cur) {
					return fish;
				}
			}
			return allFish.get(allFish.size() - 1); //should never reach this line
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Fish other = (Fish) obj;
			return Objects.equals(name, other.name);
		}
	}
}
