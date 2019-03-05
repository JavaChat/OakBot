package oakbot.command.learn;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import oakbot.Database;

/**
 * Manages all of the bot's learned commands
 * @author Michael Angstadt
 */
public class LearnedCommandsDao implements Iterable<LearnedCommand> {
	private final Database db;
	private final List<LearnedCommand> commands = new ArrayList<>();

	/**
	 * Using this constructor will not persist any learned commands.
	 */
	public LearnedCommandsDao() {
		this(null);
	}

	/**
	 * @param db the database where the learned commands are persisted
	 */
	public LearnedCommandsDao(Database db) {
		this.db = db;
		load();
	}

	/**
	 * Determines if a command exists.
	 * @param commandName the name of the command (case insensitive)
	 * @return true if it exists, false if not
	 */
	public boolean contains(String commandName) {
		return get(commandName) != null;
	}

	/**
	 * Gets a command.
	 * @param commandName the name of the command (case insensitive)
	 * @return the command or null if not found
	 */
	public LearnedCommand get(String commandName) {
		return commands.stream() //@formatter:off
			.filter(c -> commandName.equalsIgnoreCase(c.name()))
			.findFirst()
		.orElse(null); //@formatter:on
	}

	/**
	 * Adds a command.
	 * @param command the command to add
	 */
	public void add(LearnedCommand command) {
		commands.add(command);
		save();
	}

	/**
	 * Removes a command.
	 * @param commandName the command name (case insensitive)
	 * @return true if the command was successfully removed, false if a command
	 * with the given name could not be found
	 */
	public boolean remove(String commandName) {
		boolean found = commands.removeIf(c -> commandName.equalsIgnoreCase(c.name()));
		if (found) {
			save();
		}
		return found;
	}

	private void load() {
		if (db == null) {
			return;
		}

		List<Object> list = db.getList("learned-commands");
		if (list == null) {
			return;
		}

		for (Object item : list) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) item;

			LearnedCommand.Builder builder = new LearnedCommand.Builder();

			/*
			 * When this value is written to the database, the fact that it is a
			 * "long" will be lost if the value is small enough to fit into an
			 * "int". Therefore, check to see if an "int" was returned from the
			 * database and if so, cast it to a "long".
			 */
			Long messageId;
			Object value = map.get("messageId");
			if (value instanceof Integer) {
				messageId = ((Integer) value).longValue();
			} else {
				messageId = (Long) value;
			}
			builder.messageId(messageId);

			builder.authorUserId((Integer) map.get("authorUserId"));
			builder.authorUsername((String) map.get("authorUsername"));
			builder.roomId((Integer) map.get("roomId"));
			builder.created((LocalDateTime) map.get("created"));
			builder.name((String) map.get("name"));
			builder.output((String) map.get("output"));

			commands.add(builder.build());
		}
	}

	/**
	 * Persists the commands to the file system.
	 */
	private void save() {
		if (db == null) {
			return;
		}

		List<Map<String, Object>> list = new ArrayList<>(commands.size());
		for (LearnedCommand command : commands) {
			Map<String, Object> map = new HashMap<>();
			map.put("authorUserId", command.getAuthorUserId());
			map.put("authorUsername", command.getAuthorUsername());
			map.put("roomId", command.getRoomId());
			map.put("messageId", command.getMessageId());
			map.put("created", command.getCreated());
			map.put("name", command.name());
			map.put("output", command.getOutput());
			list.add(map);
		}

		db.set("learned-commands", list);
	}

	@Override
	public Iterator<LearnedCommand> iterator() {
		return commands.iterator();
	}

	/**
	 * Gets all of the learned commands.
	 * @return the learned commands
	 */
	public List<LearnedCommand> getCommands() {
		return commands;
	}
}
