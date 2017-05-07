package oakbot.chat;

import java.util.Collections;
import java.util.List;

/**
 * Contains information about a room, such as its name and description.
 * @author Michael Angstadt
 */
public class RoomInfo {
	private final int id;
	private final String name;
	private final String description;
	private final List<String> tags;

	/**
	 * @param id the room ID
	 * @param name the room name
	 * @param description the room description
	 * @param tags the room tags
	 */
	public RoomInfo(int id, String name, String description, List<String> tags) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.tags = Collections.unmodifiableList(tags);
	}

	/**
	 * Gets the room ID
	 * @return the room ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gets the room name
	 * @return the room name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the room description.
	 * @return the room description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets the room's tags.
	 * @return the room's tags
	 */
	public List<String> getTags() {
		return tags;
	}

	@Override
	public String toString() {
		return "RoomInfo [id=" + id + ", name=" + name + ", description=" + description + ", tags=" + tags + "]";
	}
}
