package oakbot;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import oakbot.util.PropertiesWrapper;

/**
 * Holds environment settings, such as the bot's login credentials.
 * @author Michael Angstadt
 */
public class BotProperties extends PropertiesWrapper {
	private final String loginEmail, password, botname, trigger, dictionaryKey;
	private final List<Integer> rooms, admins;
	private final int heartbeat;
	private final Path javadocPath;

	/**
	 * @param properties the properties file to pull the settings from
	 */
	public BotProperties(Properties properties) {
		super(properties);

		loginEmail = get("login.email");
		password = get("login.password");
		botname = get("botname");
		trigger = get("trigger", "=");
		rooms = getIntegerList("rooms", Arrays.asList(1)); //default to "Sandbox"
		admins = getIntegerList("admins");
		heartbeat = getInteger("heartbeat", 3000);
		javadocPath = Paths.get(get("javadoc.folder", "javadocs"));
		dictionaryKey = get("dictionary.key");
	}

	/**
	 * Gets the SO login email address.
	 * @return the login email address or null if not set
	 */
	public String getLoginEmail() {
		return loginEmail;
	}

	/**
	 * Gets the SO login password.
	 * @return the login password or null if not set
	 */
	public String getLoginPassword() {
		return password;
	}

	/**
	 * Gets the user name associated with the bot's SO account.
	 * @return the bot's name
	 */
	public String getBotname() {
		return botname;
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
	 * Gets the amount of time to wait in between checks for new messages.
	 * @return the pause time in milliseconds
	 */
	public int getHeartbeat() {
		return heartbeat;
	}

	/**
	 * Gets the path to the folder where the javadoc ZIP files are held.
	 * @return the path to the javadoc folder (defaults to "javadocs" if not
	 * specified)
	 */
	public Path getJavadocPath() {
		return javadocPath;
	}

	/**
	 * Gets the API key for the define command.
	 * @return the API key or null if not found
	 */
	public String getDictionaryKey() {
		return dictionaryKey;
	}
}
