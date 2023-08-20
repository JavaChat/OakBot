package oakbot.command;

import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.util.List;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;

/**
 * Makes the bot say something.
 * @author Michael Angstadt
 */
public class EchoCommand implements Command {
	@Override
	public String name() {
		return "echo";
	}

	@Override
	public List<String> aliases() {
		return List.of("say");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Makes the bot say something.")
			.example("I love Java!", "Makes the bot post the message, \"I love Java!\".")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String content = chatCommand.getContentMarkdown().trim();
		if (content.isEmpty()) {
			return reply("Tell me what to say.", chatCommand);
		}

		return post(content);
	}
}
