package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.reply;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;

/**
 * Displays the room's ChatGPT prompt.
 * @author Michael Angstadt
 */
public class PromptCommand implements Command {
	private final ChatGPT chatGpt;

	public PromptCommand(ChatGPT chatGpt) {
		this.chatGpt = chatGpt;
	}

	@Override
	public String name() {
		return "prompt";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays the room's ChatGPT prompt.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var roomId = chatCommand.getMessage().getRoomId();
		var prompt = chatGpt.buildPrompt(roomId);
		return reply(prompt, chatCommand);
	}
}
