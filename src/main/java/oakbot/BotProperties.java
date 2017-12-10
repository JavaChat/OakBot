package oakbot;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.command.AdventOfCodeCommand;
import oakbot.util.PropertiesWrapper;

/**
 * Holds environment settings, such as the bot's login credentials.
 * @author Michael Angstadt
 */
public class BotProperties extends PropertiesWrapper {
	private final String loginEmail, password, botUserName, trigger, greeting, dictionaryKey, aboutHost, catKey, reactKey, adventOfCodeSession;
	private final List<Integer> homeRooms, quietRooms, admins, bannedUsers;
	private final int botUserId;
	private final Integer hideOneboxesAfter;
	private final Path javadocPath;
	private final boolean javadocCache;
	private final Map<Integer, String> welcomeMessages, adventOfCodeLeaderboards;

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
		homeRooms = getIntegerList("homeRooms", Arrays.asList(1)); //default to "Sandbox"
		quietRooms = getIntegerList("quietRooms");
		admins = getIntegerList("admins");
		bannedUsers = getIntegerList("bannedUsers");
		javadocPath = getFile("javadoc.folder");
		javadocCache = getBoolean("javadoc.cache", true);
		greeting = get("greeting");
		dictionaryKey = get("dictionary.key");
		aboutHost = get("about.host");
		catKey = get("cat.key");
		reactKey = get("react.key");
		hideOneboxesAfter = getInteger("hideOneboxesAfter");

		welcomeMessages = new HashMap<>();
		{
			Pattern p = Pattern.compile("^welcome\\.(\\d+)\\.message$");
			for (String key : keySet()) {
				Matcher m = p.matcher(key);
				if (m.find()) {
					Integer roomId = Integer.valueOf(m.group(1));
					String message = get(key);
					if (message != null) {
						welcomeMessages.put(roomId, message);
					}
				}
			}
		}

		adventOfCodeLeaderboards = new HashMap<>();
		{
			Pattern p = Pattern.compile("^advent\\.(\\d+)\\.id$");
			for (String key : keySet()) {
				Matcher m = p.matcher(key);
				if (m.find()) {
					Integer roomId = Integer.valueOf(m.group(1));
					String id = get(key);
					if (id != null && !id.isEmpty()) {
						adventOfCodeLeaderboards.put(roomId, id);
					}
				}
			}
		}

		adventOfCodeSession = get("advent.session");
	}

	/**
	 * Gets the Stack Overflow login email address.
	 * @return the login email address or null if not set
	 */
	public String getLoginEmail() {
		return loginEmail;
	}

	/**
	 * Gets the Stack Overflow login password.
	 * @return the login password or null if not set
	 */
	public String getLoginPassword() {
		return password;
	}

	/**
	 * Gets the user name associated with the bot's Stack Overflow account.
	 * @return the bot's name
	 */
	public String getBotUserName() {
		return botUserName;
	}

	/**
	 * Gets the user ID of the Stack Overflow account.
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
	 * Gets the users who have been banned from interacting with the bot.
	 * @return the user IDs
	 */
	public List<Integer> getBannedUsers() {
		return bannedUsers;
	}

	/**
	 * Gets the path to the folder where the javadoc ZIP files are held.
	 * @return the path to the javadoc folder or null if not defined
	 */
	public Path getJavadocPath() {
		return javadocPath;
	}

	/**
	 * Gets whether the javadoc command should use a cache or not.
	 * @return true to use a cache, false not to
	 */
	public boolean getJavadocCache() {
		return javadocCache;
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
	 * @return the API key or null if not defined
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

	/**
	 * Gets the API key for <a href="http://thecatapi.com">thecatapi.com</a>.
	 * Requests to the API can still be made without a key.
	 * @return the API key or null if not defined
	 */
	public String getCatKey() {
		return catKey;
	}

	/**
	 * Gets the API key for <a href="http://replygif.net">replygif.net</a>.
	 * @return the key or null if not defined
	 */
	public String getReactKey() {
		return reactKey;
	}

	/**
	 * Gets the amount of time to wait before hiding a onebox the bot posts.
	 * @return the amount of time (in milliseconds) or null not to hide oneboxes
	 */
	public Integer getHideOneboxesAfter() {
		return hideOneboxesAfter;
	}

	/**
	 * Gets the messages to post when a new user joins a room.
	 * @return the messages (key = room ID, value = message)
	 */
	public Map<Integer, String> getWelcomeMessages() {
		return welcomeMessages;
	}

	/**
	 * Gets the default Advent of Code leaderboard ID to use for the
	 * {@link AdventOfCodeCommand} command.
	 * @return the messages (key = room ID, value = leaderboard ID)
	 */
	public Map<Integer, String> getAdventOfCodeLeaderboards() {
		return adventOfCodeLeaderboards;
	}

	/**
	 * Gets the session token used to query the Advent of Code leaderboards.
	 * @return the session token or null if not set
	 */
	public String getAdventOfCodeSession() {
		return adventOfCodeSession;
	}
}
