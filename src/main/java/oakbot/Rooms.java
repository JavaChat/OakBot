package oakbot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import oakbot.util.PropertiesWrapper;

/**
 * Records and persists the rooms the bot has joined.
 * @author Michael Angstadt
 */
public class Rooms {
	private static final Logger logger = Logger.getLogger(Rooms.class.getName());
	private final Path file;
	private final List<Integer> rooms = new ArrayList<>();
	private final List<Integer> homeRooms;

	/**
	 * This constructor will not persist any of the rooms the bot joins.
	 * @param homeRooms the bot's home rooms
	 * @throws IOException if there's a problem reading from the file
	 */
	public Rooms(List<Integer> homeRooms) {
		this.file = null;
		this.homeRooms = homeRooms;
		rooms.addAll(homeRooms);
	}

	/**
	 * This constructor will persist the rooms the bot joins.
	 * @param file the file where the room data is stored.
	 * @param homeRooms the bot's home rooms
	 * @throws IOException if there's a problem reading from the file
	 */
	public Rooms(Path file, List<Integer> homeRooms) throws IOException {
		this.file = file;
		this.homeRooms = homeRooms;

		if (Files.exists(file)) {
			load();
		}

		/*
		 * Make sure all the home rooms are in the list.
		 */
		boolean modified = false;
		for (Integer homeRoom : homeRooms) {
			if (!rooms.contains(homeRoom)) {
				rooms.add(homeRoom);
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

	private void load() throws IOException {
		if (file == null) {
			return;
		}

		PropertiesWrapper properties = new PropertiesWrapper(file);
		rooms.addAll(properties.getIntegerList("rooms"));
	}

	private void save() {
		if (file == null) {
			return;
		}

		PropertiesWrapper properties = new PropertiesWrapper();
		properties.setIntegerList("rooms", rooms);
		try {
			properties.store(file);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not persist room data.", e);
		}
	}
}
