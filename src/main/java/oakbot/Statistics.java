package oakbot;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Records statistics on the bot.
 * @author Michael Angstadt
 */
public class Statistics {
	private final Database db;
	private final LocalDateTime since;
	private int responses;

	/**
	 * @param db the database to save the stats to
	 */
	public Statistics(Database db) {
		this.db = db;

		var value = db.getMap("statistics");
		if (value == null) {
			since = LocalDateTime.now();
			responses = 0;
			save();
		} else {
			since = (LocalDateTime) value.get("since");
			responses = (Integer) value.get("responses");
		}
	}

	public synchronized void incMessagesRespondedTo() {
		responses++;
		save();
	}

	public synchronized int getMessagesRespondedTo() {
		return responses;
	}

	public LocalDateTime getSince() {
		return since;
	}

	private void save() {
		db.set("statistics", Map.of(
			"since", since,
			"responses", responses
		));
	}
}
