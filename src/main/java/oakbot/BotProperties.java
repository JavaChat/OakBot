package oakbot;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import oakbot.util.PropertiesWrapper;

/**
 * Holds environment settings, such as the bot's login credentials.
 * @author Michael Angstadt
 */
public class BotProperties extends PropertiesWrapper {
	private final String loginEmail, password, trigger;
	private final List<Integer> rooms, admins;
	private final int heartbeat;

	/**
	 * @param properties the properties file to pull the settings from
	 */
	public BotProperties(Properties properties) {
		super(properties);

		loginEmail = get("login.email");
		password = get("login.password");
		trigger = get("trigger", "=");
		rooms = getIntList("rooms", Arrays.asList(1)); //default to "Sandbox"
		admins = getIntList("admins");
		heartbeat = getInt("heartbeat", 3000);
	}

	/**
	 * Gets the login email address.
	 * @return the login email address or null if not set
	 */
	public String getLoginEmail() {
		return loginEmail;
	}

	/**
	 * Gets the login password
	 * @return the login password or null if not set
	 */
	public String getLoginPassword() {
		return password;
	}

	/**
	 * Gets the string sequence that triggers the bot.
	 * @return the trigger (defaults to "=")
	 */
	public String getTrigger() {
		return trigger;
	}

	/**
	 * Gets the IDs of the rooms to join.
	 * @return the room IDs (defaults to "1" for "Sandbox")
	 */
	public List<Integer> getRooms() {
		return rooms;
	}

	/**
	 * Gets the IDs of the users who have bot admin permissions.
	 * @return the user IDs
	 */
	public List<Integer> getAdmins() {
		return admins;
	}

	/**
	 * Gets the amount of time to wait between pings.
	 * @return the ping time in milliseconds
	 */
	public int getHeartbeat() {
		return heartbeat;
	}
}
