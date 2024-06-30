package oakbot.discord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import oakbot.util.PropertiesWrapper;

/**
 * @author Michael Angstadt
 */
public class DiscordProperties extends PropertiesWrapper {
	private final String discordToken;
	private final String discordStatus;
	private final String openAIKey;
	private final String stabilityAIKey;
	private final int openAIMessageHistoryCount;
	private final String openAIPrompt;
	private final List<Long> adminUsers;
	private final List<Long> ignoredChannels;

	public DiscordProperties(Path path) throws IOException {
		super(path);
		discordToken = get("discord.token");
		discordStatus = get("discord.status");
		openAIKey = get("openai.key");
		openAIMessageHistoryCount = getInteger("openai.messageHistoryCount", 10);
		openAIPrompt = get("openai.prompt");
		stabilityAIKey = get("stabilityai.key");
		adminUsers = getLongList("admins");
		ignoredChannels = getLongList("ignoredChannels");
	}

	public String getDiscordToken() {
		return discordToken;
	}

	public String getDiscordStatus() {
		return discordStatus;
	}

	public String getOpenAIKey() {
		return openAIKey;
	}

	public String getStabilityAIKey() {
		return stabilityAIKey;
	}

	public int getOpenAIMessageHistoryCount() {
		return openAIMessageHistoryCount;
	}

	public String getOpenAIPrompt() {
		return openAIPrompt;
	}

	public List<Long> getAdminUsers() {
		return adminUsers;
	}

	public List<Long> getIgnoredChannels() {
		return ignoredChannels;
	}
}
