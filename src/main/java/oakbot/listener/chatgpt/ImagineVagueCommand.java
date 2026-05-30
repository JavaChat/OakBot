package oakbot.listener.chatgpt;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;

/**
 * Generates images using various AI image models, using an embellished prompt
 * written by AI.
 * @author Michael Angstadt
 */
public class ImagineVagueCommand implements Command {
	private final ImagineCore core;

	public ImagineVagueCommand(ImagineCore core) {
		this.core = core;
	}

	@Override
	public String name() {
		return "imagine-vague";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Creates images using AI, using an embellished prompt written by AI.")
			.detail(core.helpDetail())
			.example("friiday", "Generates a prompt based on the word \"friiday\", and creates an image from that generated prompt.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		return core.onMessageEmbellish(chatCommand, bot);
	}
}
