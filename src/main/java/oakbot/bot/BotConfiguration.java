package oakbot.bot;

import java.time.Duration;

/**
 * Configuration settings for the Bot.
 * Groups related configuration parameters together.
 * 
 * @param userName the bot's username
 * @param userId the bot's user ID
 * @param trigger the command trigger (e.g., "/")
 * @param greeting the greeting message to post when joining rooms
 * @param hideOneboxesAfter duration after which to condense/hide onebox messages
 */
public record BotConfiguration(
	String userName,
	Integer userId,
	String trigger,
	String greeting,
	Duration hideOneboxesAfter
) {
}
