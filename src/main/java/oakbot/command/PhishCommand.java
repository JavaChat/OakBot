package oakbot.command;

import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;
import static oakbot.util.StringUtils.a;
import static oakbot.util.StringUtils.possessive;

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
 * Allows user to phish.
 * @author Michael Angstadt
 */
public class PhishCommand implements Command, ScheduledTask {
	private static final String PHISH_EMOJI = "📧";
	private static final Phish FBI = new Phish("FBI", 0.2);

	//@formatter:off
	private static final List<Phish> allPhish = new ArrayList<>(List.of(
		new Phish("gift card number", 0.5),
		new Phish("bank account number", 0.4),
		new Phish("credit card number", 0.4),
		new Phish("social security number", 0.1),
		new Phish("bank account fund transfer", 0.1),
		
		new Phish("Outlook password", 0.9),
		new Phish("Hotmail password", 0.9),
		new Phish("Gmail password", 0.9),
		new Phish("Yahoo password", 0.9),
		new Phish("Facebook password", 0.9),
		new Phish("Instagram password", 0.9),
		new Phish("Tiktok password", 0.9),
		new Phish("GitHub password", 0.5),
		new Phish("StackOverflow password", 0.3),
		new Phish("PayPal password", 0.3),
		new Phish("bank password", 0.3),
		
		new Phish("phone number", 0.5),
		new Phish("home address", 0.3),
		
		new Phish("identity", 0.05),
		FBI
	));
	//@formatter:on

	private final Database db;
	private final Map<Integer, Map<Integer, PendingCatch>> currentlyPhishingByRoom = new HashMap<>();
	private final Map<Integer, Inbox> inboxesByUser;

	private final Duration minTimeUntilQuiver;
	private final Duration maxTimeUntilQuiver;

	public PhishCommand(Database db, String minTimeUntilQuiver, String maxTimeUntilQuiver) {
		this.db = db;
		this.minTimeUntilQuiver = Duration.parse(minTimeUntilQuiver);
		this.maxTimeUntilQuiver = Duration.parse(maxTimeUntilQuiver);
		inboxesByUser = loadInventories();
	}

	@Override
	public String name() {
		return "phish";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((Command)this)
			.summary("Allows users to phish " + PHISH_EMOJI + ".")
			.example("", "Starts a phishing campaign or checks your inbox if you have a new message.")
			.example("inbox", "Displays the user's old emails.")
			.example("status", "Displays the status of the user's phishing campaign.")
			.example("delete Gmail password", "Deletes one of your emails.")
			.example("again", "Checks your inbox for new messages, and starts another phishing campaign.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContent();
		if (!content.isEmpty()) {
			var args = chatCommand.getContent().split("\\s+", 2);
			var subCommand = args[0];

			if ("inbox".equalsIgnoreCase(subCommand)) {
				return handleInboxCommand(chatCommand);
			}

			if ("status".equalsIgnoreCase(subCommand)) {
				return handleStatusCommand(chatCommand);
			}

			if ("delete".equalsIgnoreCase(subCommand)) {
				var fishName = (args.length < 2) ? null : args[1];
				return handleDeleteCommand(fishName, chatCommand);
			}

			if ("again".equalsIgnoreCase(subCommand)) {
				return sendEmailOrCheckInbox(chatCommand, true);
			}

			return reply(phishMessage("Unknown phish command."), chatCommand);
		}

		return sendEmailOrCheckInbox(chatCommand, false);
	}

	private ChatActions handleInboxCommand(ChatCommand chatCommand) {
		var userId = chatCommand.getMessage().getUserId();
		var inbox = inboxesByUser.get(userId);

		String message;
		if (inbox == null || inbox.isEmpty()) {
			message = "Your inbox is empty.";
		} else {
			//@formatter:off
			message = "Your inbox: " + inbox.map.entrySet().stream()
				.sorted((a, b) -> {
					//sort by quantity descending
					var c = b.getValue().compareTo(a.getValue());
					if (c != 0) {
						return c;
					}
					
					//then sort by phish name ascending
					return a.getKey().name.compareTo(b.getKey().name);
				})
				.map(entry -> {
					var phish = entry.getKey().name;
					var count = entry.getValue();
					if (count.intValue() == 1) {
						return phish;
					} else {
						return phish + " (x" + count + ")";
					}
				})
			.collect(Collectors.joining(", "));
			//@formatter:on
		}

		return reply(phishMessage(message), chatCommand);
	}

	private ChatActions handleStatusCommand(ChatCommand chatCommand) {
		var roomId = chatCommand.getMessage().getRoomId();
		var userId = chatCommand.getMessage().getUserId();
		var pendingCatchesInThisRoom = getPendingCatchesInRoom(roomId);
		var pendingCatch = pendingCatchesInThisRoom.get(userId);

		if (pendingCatch == null) {
			return reply(phishMessage("You haven't sent any emails out."), chatCommand);
		}

		var sinceLineQuivered = Duration.between(pendingCatch.time, Now.instant());
		var currentlyQuivering = !sinceLineQuivered.isNegative();
		String message;
		if (currentlyQuivering) {
			message = "You have an unread message.";
		} else {
			message = "No one has fallen for your phish yet. You should wait until someone does.";
		}

		return reply(phishMessage(message), chatCommand);
	}

	private ChatActions handleDeleteCommand(String phishName, ChatCommand chatCommand) {
		var userId = chatCommand.getMessage().getUserId();
		var username = chatCommand.getMessage().getUsername();
		var inbox = inboxesByUser.get(userId);

		if (phishName == null) {
			return reply(phishMessage("Specify a phish to delete."), chatCommand);
		}

		var phish = Phish.findOrNull(phishName);
		if (phish == null || inbox == null || !inbox.has(phish)) {
			return reply(phishMessage("You don't have any of that."), chatCommand);
		}

		inbox.remove(phish);
		saveInboxes();

		//@formatter:off
		return post(phishMessage(new ChatBuilder()
			.append(username).append(" deletes ").append(a(phish.name)).append(' ').bold(phish.name)
		));
		//@formatter:on
	}

	private ChatActions sendEmailOrCheckInbox(ChatCommand chatCommand, boolean again) {
		var roomId = chatCommand.getMessage().getRoomId();
		var userId = chatCommand.getMessage().getUserId();
		var username = chatCommand.getMessage().getUsername();
		var pendingCatchesInThisRoom = getPendingCatchesInRoom(roomId);

		var pendingCatch = pendingCatchesInThisRoom.get(userId);

		if (pendingCatch == null) {
			pendingCatchesInThisRoom.put(userId, new PendingCatch(username));
			return post(phishMessage(username + " sends some emails."));
		}

		var actions = new ChatActions();

		var sinceInboxPinged = Duration.between(pendingCatch.time, Now.instant());
		var tooSoon = sinceInboxPinged.isNegative();
		if (tooSoon) {
			actions.addAction(new PostMessage(phishMessage(username + " has no responses yet. They should wait until their inbox pings.")));
		} else {
			var inbox = inboxesByUser.computeIfAbsent(userId, key -> new Inbox());

			if (pendingCatch.phish == FBI) {
				inbox = new Inbox();
				inboxesByUser.put(userId, inbox);

				//@formatter:off
				actions.addAction(new PostMessage(phishMessage(new ChatBuilder()
					.append(username).append(" was caught by the authorities! Their email account was deleted!")
				)));
				//@formatter:on				
			} else {
				inbox.add(pendingCatch.phish);

				var word = (inbox.count(pendingCatch.phish) > 1) ? "another" : a(pendingCatch.phish.name);

				//@formatter:off
				actions.addAction(new PostMessage(phishMessage(new ChatBuilder()
					.append(username).append(" caught ").append(word).append(' ').bold(pendingCatch.phish.name).append('!')
				)));
				//@formatter:on
			}

			saveInboxes();
			pendingCatchesInThisRoom.remove(userId);

			if (again) {
				pendingCatchesInThisRoom.put(userId, new PendingCatch(username));
				actions.addAction(new PostMessage(phishMessage(username + " sends some emails.")));
			}
		}

		return actions;
	}

	private Map<Integer, PendingCatch> getPendingCatchesInRoom(int roomId) {
		return currentlyPhishingByRoom.computeIfAbsent(roomId, k -> new HashMap<Integer, PendingCatch>());
	}

	@Override
	public void run(IBot bot) throws Exception {
		for (var entry : currentlyPhishingByRoom.entrySet()) {
			var roomId = entry.getKey();
			var pendingCatchesInRoom = entry.getValue();
			checkInboxes(roomId, pendingCatchesInRoom, bot);
		}
	}

	private void checkInboxes(int roomId, Map<Integer, PendingCatch> pendingCatchesInRoom, IBot bot) throws IOException {
		var now = Now.instant();

		for (var entry : pendingCatchesInRoom.entrySet()) {
			var pendingCatch = entry.getValue();

			if (pendingCatch.userWarned) {
				continue;
			}

			var phishSnagged = pendingCatch.time.isBefore(now);
			if (phishSnagged) {
				pendingCatch.userWarned = true;

				var message = new PostMessage(phishMessage(possessive(pendingCatch.username) + " inbox pings."));
				bot.sendMessage(roomId, message);
				continue;
			}
		}
	}

	private String phishMessage(CharSequence message) {
		//@formatter:off
		return new ChatBuilder()
			.append(PHISH_EMOJI).append(' ')
			.italic(message)
		.toString();
		//@formatter:on
	}

	@Override
	public long nextRun() {
		return Duration.ofMinutes(1).toMillis();
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, Inbox> loadInventories() {
		var inboxes = new HashMap<Integer, Inbox>();

		var phishesByUser = db.getMap("phish.caught");
		if (phishesByUser != null) {
			for (var userPhishes : phishesByUser.entrySet()) {
				var userId = Integer.parseInt(userPhishes.getKey());
				var phishes = (Map<String, Integer>) userPhishes.getValue();

				var inv = new Inbox();
				for (var phishEntry : phishes.entrySet()) {
					var phish = Phish.find(phishEntry.getKey());
					var count = phishEntry.getValue();
					inv.set(phish, count);
				}

				inboxes.put(userId, inv);
			}
		}

		return inboxes;
	}

	private void saveInboxes() {
		var phishesByUser = new HashMap<String, Object>();
		for (var entry : inboxesByUser.entrySet()) {
			var userId = entry.getKey();
			var inbox = entry.getValue();

			var phishes = new HashMap<String, Integer>();
			for (var phish : inbox.map.entrySet()) {
				phishes.put(phish.getKey().name, phish.getValue().toInteger());
			}

			phishesByUser.put(userId + "", phishes);
		}

		db.set("phish.caught", phishesByUser);
	}

	private class PendingCatch {
		private final String username;
		private final Phish phish;
		private Instant time;
		private boolean userWarned;

		public PendingCatch(String username) {
			this.username = username;

			resetTime();
			phish = Phish.randomByRarity();
		}

		public void resetTime() {
			var seconds = Rng.next((int) minTimeUntilQuiver.toSeconds(), (int) maxTimeUntilQuiver.toSeconds());
			time = Now.instant().plus(Duration.ofSeconds(seconds));
			userWarned = false;
		}
	}

	private static class Inbox {
		private final Map<Phish, MutableInt> map = new HashMap<>();

		public void add(Phish phish) {
			var count = map.computeIfAbsent(phish, k -> new MutableInt());
			count.increment();
		}

		public void remove(Phish phish) {
			var count = map.get(phish);
			if (count != null) {
				if (count.intValue() == 1) {
					map.remove(phish);
				} else {
					count.decrement();
				}
			}
		}

		public void set(Phish phish, int count) {
			map.put(phish, new MutableInt(count));
		}

		public int count(Phish phish) {
			var count = map.get(phish);
			return (count == null) ? 0 : count.intValue();
		}

		public boolean has(Phish phish) {
			return count(phish) > 0;
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}
	}

	private static class Phish {
		private final String name;
		private final double rarity;

		public Phish(String name) {
			this(name, 0.0);
		}

		public Phish(String name, double rarity) {
			if (rarity < 0.0 || rarity >= 1.0) {
				throw new IllegalArgumentException("Rarity must be in rage [0.0, 1.0).");
			}

			this.name = name;
			this.rarity = rarity;
		}

		public static Phish findOrNull(String name) {
			//@formatter:off
			return allPhish.stream()
				.filter(phish -> name.equalsIgnoreCase(phish.name))
			.findFirst().orElse(null);
			//@formatter:on
		}

		public static Phish find(String name) {
			var phish = findOrNull(name);

			if (phish == null) {
				phish = new Phish(name);
				allPhish.add(phish);
			}

			return phish;
		}

		public static Phish randomByRarity() {
			var total = allPhish.stream().mapToDouble(phish -> phish.rarity).sum();
			var randValue = Rng.next() * total;
			var cur = 0.0;
			for (var phish : allPhish) {
				cur += phish.rarity;
				if (randValue < cur) {
					return phish;
				}
			}
			return allPhish.get(allPhish.size() - 1); //should never reach this line
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
			var other = (Phish) obj;
			return Objects.equals(name, other.name);
		}
	}
}
