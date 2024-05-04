package oakbot;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import oakbot.util.PropertiesWrapper;

/**
 * Holds environment settings, such as the bot's login credentials.
 * @author Michael Angstadt
 */
public class BotProperties extends PropertiesWrapper {
	private final Path loggingConfig;
	private final String site;
	private final String loginEmail;
	private final String password;
	private final String botUserName;
	private final String trigger;
	private final String greeting;
	private final String helpWebpage;
	private final List<Integer> homeRooms;
	private final List<Integer> quietRooms;
	private final List<Integer> admins;
	private final List<Integer> bannedUsers;
	private final List<Integer> allowedUsers;
	private final Integer botUserId;
	private final int socketPort;
	private final Duration hideOneboxesAfter;
	private final boolean enableLearnedCommands;

	/**
	 * @param properties the properties to parse
	 */
	public BotProperties(Properties properties) {
		super(properties);

		loggingConfig = getPath("logging.config", Paths.get("logging.properties"));

		site = get("account.site");
		loginEmail = get("account.email");
		password = get("account.password");
		botUserName = get("account.username");
		botUserId = getInteger("account.userId");

		trigger = get("trigger", "=");
		greeting = get("greeting");

		var value = get("hideOneboxesAfter");
		hideOneboxesAfter = (value == null) ? null : Duration.parse(value);

		homeRooms = getIntegerList("rooms.home", List.of(1)); //default to "Sandbox"
		quietRooms = getIntegerList("rooms.quiet");

		admins = getIntegerList("users.admins");
		bannedUsers = getIntegerList("users.banned");
		allowedUsers = getIntegerList("users.allowed");

		enableLearnedCommands = getBoolean("enableLearnedCommands", true);

		helpWebpage = get("help.webpage");

		socketPort = getInteger("socket.port", 0);
	}

	/**
	 * Gets the logging configuration file.
	 * @return the logging configuration file (e.g. "logging.properties")
	 */
	public Path getLoggingConfig() {
		return loggingConfig;
	}

	/**
	 * Gets the site to connect to.
	 * @return the site (e.g. "stackoverflow.com") or null if not set
	 */
	public String getSite() {
		return site;
	}

	/**
	 * Gets the login email address.
	 * @return the login email address or null if not set
	 */
	public String getLoginEmail() {
		return loginEmail;
	}

	/**
	 * Gets the login password.
	 * @return the login password or null if not set
	 */
	public String getLoginPassword() {
		return password;
	}

	/**
	 * Gets the user name associated with the bot's account.
	 * @return the bot's name
	 */
	public String getBotUserName() {
		return botUserName;
	}

	/**
	 * Gets the user ID of the account.
	 * @return the user ID
	 */
	public Integer getBotUserId() {
		return botUserId;
	}

	/**
	 * Gets the string sequence that triggers a bot command (defaults to "=").
	 * @return the trigger
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
	 * Gets the rooms that the bot will not post inactivity messages to.
	 * @return the room IDs
	 */
	public List<Integer> getQuietRooms() {
		return quietRooms;
	}

	/**
	 * Gets the users who have bot admin permissions.
	 * @return the user IDs
	 */
	public List<Integer> getAdmins() {
		return admins;
	}

	/**
	 * Gets the users who have been banned from interacting with the bot (black
	 * list).
	 * @return the user IDs
	 */
	public List<Integer> getBannedUsers() {
		return bannedUsers;
	}

	/**
	 * Gets the users who are allowed to interact with the bot (white list). If
	 * this list is empty, then anyone can use the bot (minus those listed in
	 * {@link #getBannedUsers}).
	 * @return the user IDs
	 */
	public List<Integer> getAllowedUsers() {
		return allowedUsers;
	}

	/**
	 * Gets the message to post when the bot joins a room.
	 * @return the message or null not to broadcast anything
	 */
	public String getGreeting() {
		return greeting;
	}

	/**
	 * Gets the amount of time to wait before hiding a onebox the bot posts.
	 * @return the amount of time or null not to hide oneboxes
	 */
	public Duration getHideOneboxesAfter() {
		return hideOneboxesAfter;
	}

	public boolean isEnableLearnedCommands() {
		return enableLearnedCommands;
	}

	public String getHelpWebpage() {
		return helpWebpage;
	}

	/**
	 * Gets the port number to use for the local command socket.
	 * @return the port number or 0 to automatically choose a port
	 */
	public int getSocketPort() {
		return socketPort;
	}
}
