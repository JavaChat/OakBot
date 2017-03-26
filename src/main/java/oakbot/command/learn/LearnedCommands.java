package oakbot.command.learn;

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
public class LearnedCommands implements Iterable<LearnedCommand> {
	private final Database db;
	private final List<LearnedCommand> commands = new ArrayList<>();

	/**
	 * Using this constructor will not persist any learned commands.
	 */
	public LearnedCommands() {
		this(null);
	}

	/**
	 * @param db the database where the learned commands are persisted
	 */
	public LearnedCommands(Database db) {
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
		for (LearnedCommand command : commands) {
			if (commandName.equalsIgnoreCase(command.name())) {
				return command;
			}
		}
		return null;
	}

	/**
	 * Adds a command.
	 * @param commandName the command name
	 * @param output the command output
	 */
	public void add(String commandName, String output) {
		commands.add(new LearnedCommand(commandName, output));
		save();
	}

	/**
	 * Removes a command.
	 * @param commandName the command name (case insensitive)
	 * @return true if the command was successfully removed, false if a command
	 * with the given name could not be found
	 */
	public boolean remove(String commandName) {
		Iterator<LearnedCommand> it = commands.iterator();
		while (it.hasNext()) {
			LearnedCommand command = it.next();
			if (commandName.equalsIgnoreCase(command.name())) {
				it.remove();
				save();
				return true;
			}
		}
		return false;
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
			Map<String, String> map = (Map<String, String>) item;

			String name = map.get("name");
			String output = map.get("output");
			commands.add(new LearnedCommand(name, output));
		}
	}

	/**
	 * Persists the commands to the file system.
	 */
	private void save() {
		if (db == null) {
			return;
		}

		List<Map<String, String>> list = new ArrayList<>();
		for (LearnedCommand command : commands) {
			Map<String, String> map = new HashMap<>();
			map.put("name", command.name());
			map.put("output", command.output());
			list.add(map);
		}

		db.set("learned-commands", list);
	}

	@Override
	public Iterator<LearnedCommand> iterator() {
		return commands.iterator();
	}
}
