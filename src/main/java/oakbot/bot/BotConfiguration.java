package oakbot.bot;

import java.time.Duration;

/**
 * Configuration settings for the Bot.
 * Groups related configuration parameters together.
 */
public class BotConfiguration {
	private final String userName;
	private final Integer userId;
	private final String trigger;
	private final String greeting;
	private final Duration hideOneboxesAfter;

	public BotConfiguration(String userName, Integer userId, String trigger, String greeting, Duration hideOneboxesAfter) {
		this.userName = userName;
		this.userId = userId;
		this.trigger = trigger;
		this.greeting = greeting;
		this.hideOneboxesAfter = hideOneboxesAfter;
	}

	public String getUserName() {
		return userName;
	}

	public Integer getUserId() {
		return userId;
	}

	public String getTrigger() {
		return trigger;
	}

	public String getGreeting() {
		return greeting;
	}

	public Duration getHideOneboxesAfter() {
		return hideOneboxesAfter;
	}
}
