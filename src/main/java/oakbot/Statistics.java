package oakbot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.util.PropertiesWrapper;

public class Statistics extends PropertiesWrapper {
	private static final Logger logger = Logger.getLogger(Statistics.class.getName());
	private final Path file;
	private int responses;

	public Statistics(Path file) throws IOException {
		super(file);
		this.file = file;
		responses = getInteger("responses", 0);
	}

	public synchronized void incMessagesRespondedTo() {
		responses++;
		set("responses", responses);
		save();
	}

	private void save() {
		try {
			store(file);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Could not persist statistics data.", e);
		}
	}
}
