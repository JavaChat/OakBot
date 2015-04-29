package oakbot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.util.PropertiesWrapper;

/**
 * Records statistics on the bot.
 * @author Michael Angstadt
 */
public class Statistics {
	private static final Logger logger = Logger.getLogger(Statistics.class.getName());
	private final PropertiesWrapper properties;
	private final Path file;
	private int responses;

	public Statistics(Path file) throws IOException {
		this.properties = Files.exists(file) ? new PropertiesWrapper(file) : new PropertiesWrapper();
		this.file = file;
		responses = properties.getInteger("responses", 0);
	}

	public synchronized void incMessagesRespondedTo() {
		responses++;
		properties.set("responses", responses);
		save();
	}

	private void save() {
		try {
			properties.store(file);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Could not persist statistics data.", e);
		}
	}
}
