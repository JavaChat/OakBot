package oakbot.filter;

import static oakbot.bot.ChatActions.create;

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
		var roomId = chatCommand.getMessage().roomId();
		var enabled = toggle(roomId);

		//@formatter:off
		return create(
			new PostMessage(new ChatBuilder()
				.append("Filter ").append(enabled ? "enabled" : "disabled").append(".")
			).bypassFilters(true).parentId(chatCommand.getMessage().id())
		);
		//@formatter:on
	}
}
