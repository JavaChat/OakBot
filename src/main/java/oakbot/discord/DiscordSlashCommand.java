package oakbot.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * @author Michael Angstadt
 */
public interface DiscordSlashCommand {
	SlashCommandData data();

	void onMessage(SlashCommandInteractionEvent event, BotContext context);
}
