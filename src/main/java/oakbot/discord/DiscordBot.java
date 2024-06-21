package oakbot.discord;

import static java.util.Objects.requireNonNull;

import java.util.EnumSet;
import java.util.List;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

/**
 * @author Michael Angstadt
 */
public class DiscordBot {
	private final JDA jda;

	private final String trigger;
	private final List<Long> adminUsers;
	private final List<Long> ignoredChannels;

	private final List<DiscordListener> mentionListeners;
	private final List<DiscordListener> listeners;

	private DiscordBot(Builder builder) throws InterruptedException {
		trigger = builder.trigger;
		adminUsers = (builder.adminUsers == null) ? List.of() : List.copyOf(builder.adminUsers);
		ignoredChannels = (builder.ignoredChannels == null) ? List.of() : List.copyOf(builder.ignoredChannels);
		listeners = (builder.listeners == null) ? List.of() : List.copyOf(builder.listeners);
		mentionListeners = (builder.mentionListeners == null) ? List.of() : List.copyOf(builder.mentionListeners);

		var intents = EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
		var jdaBuilder = JDABuilder.createLight(requireNonNull(builder.token), intents).addEventListeners(new ListenerAdapter() {
			@Override
			public void onMessageReceived(MessageReceivedEvent event) {
				DiscordBot.this.onMessageReceived(event);
			}
		});

		if (builder.status != null) {
			jdaBuilder.setActivity(Activity.customStatus(builder.status));
		}

		jda = jdaBuilder.build();
		jda.awaitReady();
	}

	public JDA getJDA() {
		return jda;
	}

	public void onMessageReceived(MessageReceivedEvent event) {
		var author = event.getAuthor();
		var authorIsAdmin = adminUsers.contains(author.getIdLong());

		var selfUser = event.getJDA().getSelfUser();
		var messagePostedByBot = author.equals(selfUser);
		if (messagePostedByBot) {
			return;
		}

		var inTextChannel = (event.getChannelType() == ChannelType.TEXT);
		if (!inTextChannel) {
			return;
		}

		var channelId = event.getChannel().getIdLong();
		var inIgnoredChannel = ignoredChannels.contains(channelId);
		if (inIgnoredChannel) {
			return;
		}

		var context = new BotContext(authorIsAdmin, trigger);

		/*
		 * Note: If a message only contains 1 word after the mention, it is not
		 * considered a mention for some reason
		 */
		var botMentioned = event.getMessage().getMentions().getUsers().contains(selfUser);
		if (botMentioned) {
			mentionListeners.forEach(l -> l.onMessage(event, context));
			return;
		}

		listeners.forEach(l -> l.onMessage(event, context));
	}

	public void shutdown() {
		jda.shutdown();
	}

	public static class Builder {
		private String token;
		private String trigger;
		private String status;
		private List<Long> adminUsers;
		private List<Long> ignoredChannels;
		private List<DiscordListener> mentionListeners;
		private List<DiscordListener> listeners;

		public Builder token(String token) {
			this.token = token;
			return this;
		}

		public Builder trigger(String trigger) {
			this.trigger = trigger;
			return this;
		}

		public Builder status(String status) {
			this.status = status;
			return this;
		}

		public Builder adminUsers(List<Long> adminUsers) {
			this.adminUsers = adminUsers;
			return this;
		}

		public Builder ignoredChannels(List<Long> ignoredChannels) {
			this.ignoredChannels = ignoredChannels;
			return this;
		}

		public Builder listeners(List<DiscordListener> listeners) {
			this.listeners = listeners;
			return this;
		}

		public Builder mentionListeners(List<DiscordListener> mentionListeners) {
			this.mentionListeners = mentionListeners;
			return this;
		}

		public DiscordBot build() throws InterruptedException {
			return new DiscordBot(this);
		}
	}
}
