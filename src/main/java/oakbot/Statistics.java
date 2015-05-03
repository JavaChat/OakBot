package oakbot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Date;
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
	private final Date since;
	private int responses;

	public Statistics(Path file) throws IOException {
		if (Files.exists(file)) {
			properties = new PropertiesWrapper(file);
			Date since;
			try {
				since = properties.getDate("since");
			} catch (ParseException e) {
				logger.log(Level.SEVERE, "Unable to parse \"since\" date.", e);
				since = null;
			}
			this.since = since;
		} else {
			properties = new PropertiesWrapper();
			since = new Date();
			properties.set("since", since);
		}

		this.file = file;
		responses = properties.getInteger("responses", 0);
	}

	public synchronized void incMessagesRespondedTo(int amount) {
		responses += amount;
		properties.set("responses", responses);
		save();
	}

	public synchronized int getMessagesRespondedTo() {
		return responses;
	}

	public Date getSince() {
		return since;
	}

	private void save() {
		try {
			properties.store(file);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not persist statistics data.", e);
		}
	}
}
