package oakbot;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records and persists the rooms the bot has joined.
 * @author Michael Angstadt
 */
public class Rooms {
	private final Database db;
	private final List<Integer> roomIds = new ArrayList<>();
	private final List<Integer> homeRooms;
	private final List<Integer> quietRooms;

	/**
	 * This constructor will persist the rooms the bot joins.
	 * @param db the database
	 * @param homeRooms the bot's home rooms
	 * @param quietRooms the rooms the bot will not post inactivity messages to
	 */
	public Rooms(Database db, List<Integer> homeRooms, List<Integer> quietRooms) {
		this.db = db;
		this.homeRooms = homeRooms;
		this.quietRooms = quietRooms;

		@SuppressWarnings("unchecked")
		var rooms = (List<Integer>) db.get("rooms");
		if (rooms != null) {
			roomIds.addAll(rooms);
		}

		/*
		 * Make sure all the home rooms are in the list.
		 */
		var sizeBefore = roomIds.size();
		homeRooms.stream().filter(not(roomIds::contains)).forEach(roomIds::add);
		var sizeAfter = roomIds.size();

		var modified = sizeBefore != sizeAfter;
		if (modified) {
			save();
		}
	}

	/**
	 * Gets all the rooms the bot has joined, including home rooms.
	 * @return the room IDs
	 */
	public List<Integer> getRooms() {
		return Collections.unmodifiableList(roomIds);
	}

	/**
	 * Gets the rooms the bot cannot be unsummoned from.
	 * @return the room IDs
	 */
	public List<Integer> getHomeRooms() {
		return Collections.unmodifiableList(homeRooms);
	}

	/**
	 * Determines if a room is a home room.
	 * @param roomId the room
	 * @return true if it's a home room, false if not
	 */
	public boolean isHomeRoom(Integer roomId) {
		return homeRooms.contains(roomId);
	}

	/**
	 * Gets the rooms the bot will not post inactivity messages to.
	 * @return the room IDs
	 */
	public List<Integer> getQuietRooms() {
		return Collections.unmodifiableList(quietRooms);
	}

	/**
	 * Determines if a room is a quiet room.
	 * @param roomId the room
	 * @return true if it's a quiet room, false if not
	 */
	public boolean isQuietRoom(Integer roomId) {
		return quietRooms.contains(roomId);
	}

	/**
	 * Adds a room to the list. This method does nothing if the room is already
	 * in the list.
	 * @param roomId the room ID
	 */
	public void add(int roomId) {
		if (roomIds.contains(roomId)) {
			return;
		}

		roomIds.add(roomId);
		save();
	}

	/**
	 * Removes a room from the list.
	 * @param roomId the room ID
	 * @return true if it was removed, false if no room was found
	 */
	public boolean remove(int roomId) {
		var removed = roomIds.remove((Integer) roomId);
		if (removed) {
			save();
		}
		return removed;
	}

	/**
	 * Determines if a room is in the list.
	 * @param roomId the room ID
	 * @return true if it's in the list, false if not
	 */
	public boolean contains(int roomId) {
		return roomIds.contains(roomId);
	}

	private void save() {
		if (db == null) {
			return;
		}

		db.set("rooms", roomIds);
	}
}
