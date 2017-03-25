package oakbot.command.learn;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages all of the bot's learned commands
 * @author Michael Angstadt
 */
public class LearnedCommands implements Iterable<LearnedCommand> {
	private static final Logger logger = Logger.getLogger(LearnedCommands.class.getName());
	private final Path file;
	private final List<LearnedCommand> commands = new ArrayList<>();

	/**
	 * Using this constructor will not persist any learned commands to a file.
	 */
	public LearnedCommands() {
		this.file = null;
	}

	/**
	 * @param file the file where the learned commands are persisted
	 * @throws IOException if the file exists and there's a problem loading it
	 */
	public LearnedCommands(Path file) throws IOException {
		this.file = file;
		if (Files.exists(file)) {
			load();
		}
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

	/**
	 * Persists the commands to the file system.
	 */
	private void save() {
		if (file == null) {
			return;
		}

		StandardOpenOption[] openOptions;
		if (Files.exists(file)) {
			openOptions = new StandardOpenOption[] { StandardOpenOption.TRUNCATE_EXISTING };
		} else {
			openOptions = new StandardOpenOption[] {};
		}

		try (Writer writer = Files.newBufferedWriter(file, openOptions)) {
			JsonFactory fact = new JsonFactory();
			try (JsonGenerator generator = fact.createGenerator(writer)) {
				generator.setPrettyPrinter(new DefaultPrettyPrinter());
				generator.writeStartArray();

				for (LearnedCommand command : commands) {
					generator.writeStartObject();
					generator.writeStringField("name", command.name());
					generator.writeStringField("output", command.output());
					generator.writeEndObject();
				}

				generator.writeEndArray();
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not persist learned commands.", e);
		}
	}

	/**
	 * Loads the commands from the file system.
	 * @throws IOException if there was a problem reading from the file
	 */
	private void load() throws IOException {
		if (file == null) {
			return;
		}

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root;
		try (Reader reader = Files.newBufferedReader(file)) {
			root = mapper.readTree(reader);
		}

		Iterator<JsonNode> it = root.elements();
		while (it.hasNext()) {
			JsonNode node = it.next();

			String name = getField(node, "name");
			if (name.isEmpty()) {
				continue;
			}

			String output = getField(node, "output");
			if (output.isEmpty()) {
				continue;
			}

			commands.add(new LearnedCommand(name, output));
		}
	}

	private static String getField(JsonNode object, String fieldName) {
		JsonNode field = object.get(fieldName);
		return (field == null) ? "" : field.asText();
	}

	@Override
	public Iterator<LearnedCommand> iterator() {
		return commands.iterator();
	}
}
