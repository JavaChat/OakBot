package oakbot.discord;

import java.util.List;
import java.util.Optional;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * @author Michael Angstadt
 */
public class CommandListener implements DiscordListener {
	private final String trigger;
	private final List<DiscordCommand> commands;

	public CommandListener(String trigger, List<DiscordCommand> commands) {
		this.trigger = trigger;

		/*
		 * Do not copy this list defensively. List must be modified after
		 * constructor runs in order to add "help" command.
		 */
		this.commands = commands;
	}

	@Override
	public void onMessage(MessageReceivedEvent event, BotContext context) {
		var message = event.getMessage().getContentDisplay();
		var partsOpt = parseCommandParts(message);
		if (!partsOpt.isPresent()) {
			return;
		}

		var parts = partsOpt.get();

		//@formatter:off
		commands.stream()
			.filter(c -> c.name().equalsIgnoreCase(parts.name()))
		.forEach(c -> c.onMessage(parts.content(), event, context));
		//@formatter:on
	}

	private record CommandParts(String name, String content) {
	}

	private Optional<CommandParts> parseCommandParts(String message) {
		if (!message.startsWith(trigger)) {
			return Optional.empty();
		}

		String name;
		String content;
		var afterTrigger = message.substring(trigger.length()).trim();
		var space = afterTrigger.indexOf(' ');
		if (space < 0) {
			name = afterTrigger;
			content = "";
		} else {
			name = afterTrigger.substring(0, space).toLowerCase();
			content = afterTrigger.substring(space + 1);
		}

		return Optional.of(new CommandParts(name, content));
	}
}
