package oakbot.listener.chatgpt;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;

/**
 * Generates images using various AI image models using the exact prompt the
 * user entered. Some models use AI to embellish the prompt. This command tries
 * to ensure that the prompt is not revised.
 * @author Michael Angstadt
 */
public class ImagineExactCommand implements Command {
	private final ImagineCore core;

	public ImagineExactCommand(ImagineCore core) {
		this.core = core;
	}

	@Override
	public String name() {
		return "imagine-exact";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Creates images using OpenAI's DALL·E and Stability.ai, but tells the AI not to revise the prompt.")
			.detail("DALL·E 3 uses AI to embellish the prompt. When using this model, this command instructs the AI not to revise the prompt. May not work consistently. " + core.helpDetail())
			.example("a cute Java programmer", "Generates an image using DALL·E 3 using that exact prompt.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		return core.onMessageExact(chatCommand, bot);
	}
}
