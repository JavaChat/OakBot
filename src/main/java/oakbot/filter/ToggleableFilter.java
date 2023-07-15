package oakbot.filter;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;

/**
 * A filter which users can toggle using a command.
 * @author Michael Angstadt
 */
public abstract class ToggleableFilter extends ChatResponseFilter implements Command {
	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		int roomId = chatCommand.getMessage().getRoomId();
		boolean enabled = toggle(roomId);

		//@formatter:off
		return ChatActions.create(
			new PostMessage(new ChatBuilder()
				.reply(chatCommand)
				.append("Filter ").append(enabled ? "enabled" : "disabled").append(".")
			).bypassFilters(true)
		);
		//@formatter:on
	}
}
