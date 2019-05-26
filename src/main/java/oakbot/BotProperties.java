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
	private final String site, loginEmail, password, botUserName, trigger, greeting;
	private final List<Integer> homeRooms, quietRooms, admins, bannedUsers, allowedUsers;
	private final Integer botUserId, hideOneboxesAfter;
	private boolean enableLearnedCommands;

	/**
	 * @param properties the properties to parse
	 */
	public BotProperties(Properties properties) {
		super(properties);

		site = get("account.site");
		loginEmail = get("account.email");
		password = get("account.password");
		botUserName = get("account.username");
		botUserId = getInteger("account.userId");

		trigger = get("trigger", "=");
		greeting = get("greeting");
		hideOneboxesAfter = getInteger("hideOneboxesAfter");

		homeRooms = getIntegerList("rooms.home", Arrays.asList(1)); //default to "Sandbox"
		quietRooms = getIntegerList("rooms.quiet");

		admins = getIntegerList("users.admins");
		bannedUsers = getIntegerList("users.banned");
		allowedUsers = getIntegerList("users.allowed");

		enableLearnedCommands = getBoolean("enableLearnedCommands", true);
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
	 * @return the amount of time (in milliseconds) or null not to hide oneboxes
	 */
	public Integer getHideOneboxesAfter() {
		return hideOneboxesAfter;
	}

	public boolean isEnableLearnedCommands() {
		return enableLearnedCommands;
	}
}
