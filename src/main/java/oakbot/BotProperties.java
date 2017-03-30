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
	private final String loginEmail, password, botUserName, trigger, greeting, dictionaryKey, aboutHost;
	private final List<Integer> homeRooms, admins, bannedUsers;
	private final int heartbeat, botUserId;
	private final Path javadocPath;

	/**
	 * @param properties the properties file to pull the settings from
	 */
	public BotProperties(Properties properties) {
		super(properties);

		loginEmail = get("login.email");
		password = get("login.password");
		botUserName = get("bot.userName");
		botUserId = getInteger("bot.userId");
		trigger = get("trigger", "=");
		homeRooms = getIntegerList("rooms", Arrays.asList(1)); //default to "Sandbox"
		admins = getIntegerList("admins");
		bannedUsers = getIntegerList("bannedUsers");
		heartbeat = getInteger("heartbeat", 3000);

		String javadocPathStr = get("javadoc.folder");
		javadocPath = (javadocPathStr == null) ? null : Paths.get(javadocPathStr);

		greeting = get("greeting");
		dictionaryKey = get("dictionary.key");
		aboutHost = get("about.host");
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
	public String getBotUserName() {
		return botUserName;
	}

	/**
	 * Gets the user ID of the SO account.
	 * @return the user ID
	 */
	public Integer getBotUserId() {
		return botUserId;
	}

	/**
	 * Gets the string sequence that triggers a bot command.
	 * @return the trigger (defaults to "=")
	 */
	public String getTrigger() {
		return trigger;
	}

	/**
	 * Gets the rooms that the bot cannot be unsummoned from.
	 * @return the room IDs
	 */
	public List<Integer> getHomeRooms() {
		return homeRooms;
	}

	/**
	 * Gets the users who have bot admin permissions.
	 * @return the user IDs
	 */
	public List<Integer> getAdmins() {
		return admins;
	}

	/**
	 * Gets the users who have been banned from interacting with the bot.
	 * @return the user IDs
	 */
	public List<Integer> getBannedUsers() {
		return bannedUsers;
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
	 * Gets the message to post when the bot joins a room.
	 * @return the message or null not to broadcast anything
	 */
	public String getGreeting() {
		return greeting;
	}

	/**
	 * Gets the API key for the define command.
	 * @return the API key or null if not found
	 */
	public String getDictionaryKey() {
		return dictionaryKey;
	}

	/**
	 * Gets the display name of the server hosting the bot.
	 * @return the host or null if not defined
	 */
	public String getAboutHost() {
		return aboutHost;
	}
}
