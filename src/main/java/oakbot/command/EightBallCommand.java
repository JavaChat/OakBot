package oakbot.command;

import static oakbot.command.Command.random;
import static oakbot.command.Command.reply;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatBuilder;

/**
 * Simulates a magic 8-ball.
 * @author Michael Angstadt
 */
public class EightBallCommand implements Command {
	//@formatter:off
	private final String answers[] = {
		//positive
		"It is certain",
		"It is decidedly so",
		"Without a doubt",
		"Yes definitely",
		"You may rely on it",
		"As I see it, yes",
		"Most likely",
		"Outlook good",
		"Yes",
		"Signs point to yes",
		
		//neutral
		"Reply hazy try again",
		"Ask again later",
		"Better not tell you now",
		"Cannot predict now",
		"Concentrate and ask again",
		
		//negative
		"Don't count on it",
		"My reply is no",
		"My sources say no",
		"Outlook not so good",
		"Very doubtful",
		"That's impossible",
		"When pigs fly",
		"Chances are lower than skynet",
		"Are you kidding? No!",
		"No way"
	};
	//@formatter:on

	@Override
	public String name() {
		return "8ball";
	}

	@Override
	public String description() {
		return "Simulates a magic 8-ball.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append("Simulates a magic 8-ball.").nl()
			.append("Usage: ").append(trigger).append(name()).append(" QUESTION").nl()
			.append("Example: ").append(trigger).append(name()).append(" Is Java the best?")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String answer = random(answers);
		return reply(answer, chatCommand);
	}
}
