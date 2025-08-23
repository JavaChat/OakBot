package oakbot.listener.chatgpt;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;

/**
 * Generates images using various AI image models.
 * @author Michael Angstadt
 */
public class ImagineCommand implements Command {
	private final ImagineCore core;

	public ImagineCommand(ImagineCore core) {
		this.core = core;
	}

	@Override
	public String name() {
		return "imagine";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Creates images using OpenAI's DALL·E and Stability.ai.")
			.detail(core.helpDetail())
			.example("a cute Java programmer", "Generates an image using DALL·E 3.")
			.example("https://example.com/image.png", "Generates a variation of the given image using DALL·E 2. Image must be a PNG, JPEG, or GIF.")
			.example("https://example.com/sheep.png A sheep wearing sunglasses", "Modifies an image using Stable Diffusion 3.0.")
			.example("si-core A funny cat", "Include the model ID at the beginning of the message to define which model to use.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		return core.onMessage(chatCommand, bot);
	}
}
