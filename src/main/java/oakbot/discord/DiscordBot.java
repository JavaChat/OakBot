package oakbot.discord;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

/**
 * @author Michael Angstadt
 */
public class DiscordBot {
	private static final Logger logger = Logger.getLogger(DiscordBot.class.getName());

	private final JDA jda;

	private final String trigger;
	private final List<Long> adminUsers;
	private final List<Long> ignoredChannels;

	private final List<DiscordListener> mentionListeners;
	private final List<DiscordListener> listeners;
	private final Map<String, DiscordSlashCommand> slashCommands;

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

			@Override
			public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
				DiscordBot.this.onSlashCommandInteraction(event);
			}
		});

		if (builder.status != null) {
			jdaBuilder.setActivity(Activity.customStatus(builder.status));
		}

		jda = jdaBuilder.build();
		jda.awaitReady();

		if (builder.slashCommands == null) {
			slashCommands = Map.of();
		} else {
			var slashCommandsMut = new HashMap<String, DiscordSlashCommand>();
			var slashCommandData = new ArrayList<SlashCommandData>();
			for (var slashCommand : builder.slashCommands) {
				var data = slashCommand.data();
				slashCommandData.add(data);
				slashCommandsMut.put(data.getName(), slashCommand);
			}

			slashCommands = Collections.unmodifiableMap(slashCommandsMut);
			jda.updateCommands().addCommands(slashCommandData).complete();
		}
	}

	public JDA getJDA() {
		return jda;
	}

	void onMessageReceived(MessageReceivedEvent event) {
		var author = event.getAuthor();

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

		var authorIsAdmin = adminUsers.contains(author.getIdLong());
		var context = new BotContext(authorIsAdmin, trigger);

		/*
		 * Note: If a message only contains 1 word after the mention, it is not
		 * considered a mention for some reason
		 */
		var botMentioned = event.getMessage().getMentions().getUsers().contains(selfUser);
		if (botMentioned) {
			mentionListeners.forEach(l -> {
				try {
					l.onMessage(event, context);
				} catch (Exception e) {
					logUnhandledException(l, e);
				}
			});
			return;
		}

		listeners.forEach(l -> {
			try {
				l.onMessage(event, context);
			} catch (Exception e) {
				logUnhandledException(l, e);
			}
		});
	}

	void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var command = slashCommands.get(event.getName());
		boolean unknownCommand = (command == null);
		if (unknownCommand) {
			return;
		}

		var author = event.getUser();
		var authorIsAdmin = adminUsers.contains(author.getIdLong());
		var context = new BotContext(authorIsAdmin, trigger);

		try {
			command.onMessage(event, context);
		} catch (Exception e) {
			logUnhandledException(command, e);
		}
	}

	private void logUnhandledException(Object thrownBy, Exception e) {
		logger.log(Level.SEVERE, e, () -> "Unhandled exception thrown by " + thrownBy.getClass().getName() + ".");
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
		private List<DiscordSlashCommand> slashCommands;

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

		public Builder slashCommands(List<DiscordSlashCommand> slashCommands) {
			this.slashCommands = slashCommands;
			return this;
		}

		public DiscordBot build() throws InterruptedException {
			return new DiscordBot(this);
		}
	}
}
