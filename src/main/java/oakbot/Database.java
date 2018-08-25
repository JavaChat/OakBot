package oakbot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * An interface to persistent storage.
 * @author Michael Angstadt
 */
public interface Database {
	/**
	 * Retrieves a value from the database.
	 * @param key the key
	 * @return the value
	 */
	Object get(String key);

	/**
	 * Retrieves a map value from the database.
	 * @param key the key
	 * @return the value
	 */
	@SuppressWarnings("unchecked")
	default Map<String, Object> getMap(String key) {
		return (Map<String, Object>) get(key);
	}

	/**
	 * Retrieves a list value from the database.
	 * @param key the key
	 * @return the value
	 */
	@SuppressWarnings("unchecked")
	default List<Object> getList(String key) {
		return (List<Object>) get(key);
	}

	/**
	 * <p>
	 * Stores a value in the database.
	 * </p>
	 * <p>
	 * If the given key already exists in the database, it will be overwritten.
	 * </p>
	 * <p>
	 * This method will properly serialize the following value objects so that
	 * round-tripping is preserved:
	 * </p>
	 * <ul>
	 * <li>{@code Map<String,Object>}</li>
	 * <li>{@code List<Object>}</li>
	 * <li>{@link LocalDateTime}</li>
	 * <li>{@link Integer}</li>
	 * <li>{@link Long} (if within the range of a Java int, will be converted to
	 * an int)</li>
	 * <li>{@code null}</li>
	 * </ul>
	 * <p>
	 * All other objects are converted to strings using their {@code toString}
	 * method.
	 * </p>
	 * @param key the key
	 * @param value the value
	 */
	void set(String key, Object value);

	/**
	 * Saves all changes made to the database since the last commit (if any).
	 */
	void commit();
}
