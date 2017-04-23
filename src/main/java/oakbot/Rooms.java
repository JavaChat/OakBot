package oakbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records and persists the rooms the bot has joined.
 * @author Michael Angstadt
 */
public class Rooms {
	private final Database db;
	private final List<Integer> rooms = new ArrayList<>();
	private final List<Integer> homeRooms, quietRooms;

	/**
	 * This constructor will not persist any of the rooms the bot joins.
	 * @param homeRooms the bot's home rooms
	 * @param quietRooms the rooms the bot will not post inactivity messages to
	 */
	public Rooms(List<Integer> homeRooms, List<Integer> quietRooms) {
		this(null, homeRooms, quietRooms);
	}

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

		if (db != null) {
			@SuppressWarnings("unchecked")
			List<Integer> rooms = (List<Integer>) db.get("rooms");
			if (rooms != null) {
				this.rooms.addAll(rooms);
			}
		}

		/*
		 * Make sure all the home rooms are in the list.
		 */
		boolean modified = false;
		for (Integer homeRoom : homeRooms) {
			if (!this.rooms.contains(homeRoom)) {
				this.rooms.add(homeRoom);
				modified = true;
			}
		}
		if (modified) {
			save();
		}
	}

	/**
	 * Gets all the rooms the bot has joined, including home rooms.
	 * @return the room IDs
	 */
	public List<Integer> getRooms() {
		return Collections.unmodifiableList(rooms);
	}

	/**
	 * Gets the rooms the bot cannot be unsummoned from.
	 * @return the room IDs
	 */
	public List<Integer> getHomeRooms() {
		return Collections.unmodifiableList(homeRooms);
	}

	/**
	 * Gets the rooms the bot will not post inactivity messages to.
	 * @return the room IDs
	 */
	public List<Integer> getQuietRooms() {
		return Collections.unmodifiableList(quietRooms);
	}

	/**
	 * Adds a room to the list. This method does nothing if the room is already
	 * in the list.
	 * @param roomId the room ID
	 */
	public void add(int roomId) {
		if (rooms.contains(roomId)) {
			return;
		}

		rooms.add(roomId);
		save();
	}

	/**
	 * Removes a room from the list.
	 * @param roomId the room ID
	 * @return true if it was removed, false if no room was found
	 */
	public boolean remove(int roomId) {
		boolean removed = rooms.remove((Integer) roomId);
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
		return rooms.contains((Integer) roomId);
	}

	private void save() {
		if (db == null) {
			return;
		}

		db.set("rooms", rooms);
	}
}
