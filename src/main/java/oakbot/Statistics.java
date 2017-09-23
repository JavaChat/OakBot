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

		Map<String, Object> value = db.getMap("statistics");
		if (value == null) {
			since = LocalDateTime.now();
			responses = 0;
			save();
		} else {
			since = (LocalDateTime) value.get("since");
			responses = (Integer) value.get("responses");
		}
	}

	public synchronized void incMessagesRespondedTo(int amount) {
		if (amount <= 0) {
			return;
		}

		responses += amount;
		save();
	}

	public synchronized int getMessagesRespondedTo() {
		return responses;
	}

	public LocalDateTime getSince() {
		return since;
	}

	private void save() {
		Map<String, Object> value = new HashMap<>();
		value.put("since", since);
		value.put("responses", responses);
		db.set("statistics", value);
	}
}
