package oakbot;

import java.util.HashMap;
import java.util.Map;

/**
 * A database that only holds its values in memory.
 * @author Michael Angstadt
 */
public class MemoryDatabase implements Database {
	private final Map<String, Object> fields = new HashMap<>();

	@Override
	public Object get(String key) {
		return fields.get(key);
	}

	@Override
	public void set(String key, Object value) {
		fields.put(key, value);
	}

	@Override
	public void commit() {
		//do nothing
	}
}
