package oakbot.command;

import static oakbot.command.Command.random;
import static oakbot.command.Command.reply;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.util.ChatBuilder;

/**
 * Rolls a variable-sided die or makes a choice.
 * @author Michael Angstadt
 */
public class RollCommand implements Command {
	private final Pattern diceRegex = Pattern.compile("^(\\d+)d(\\d+)$", Pattern.CASE_INSENSITIVE);

	@Override
	public String name() {
		return "roll";
	}

	@Override
	public String description() {
		return "Rolls a variable-sided die or makes a choice.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append("Rolls a variable-sided die or makes a choice.").nl()
			.append("Usage: ").append(trigger).append(name()).append(" <TIMES>d<SIDES>").nl()
			.append("Usage: ").append(trigger).append(name()).append(" choice1 choice2 ... choiceN").nl()
			.append("Examples").nl()
			.append("Roll a six-sided die: ").append(trigger).append(name()).nl()
			.append("Roll two twenty-sided dice: ").append(trigger).append(name()).append(" 2d20").nl()
			.append("Pick a choice: ").append(trigger).append(name()).append(" java scala c#")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		Parameters parameters = parseParameters(chatCommand);
		if (parameters.choices != null) {
			String choice = random(parameters.choices);
			return reply(choice, chatCommand);
		}

		if (parameters.times > 100) {
			return reply("Sorry, they don't pay me enough for that.", chatCommand);
		}

		if (parameters.times <= 0) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.italic("rolls nothing")
			);
			//@formatter:on
		}

		if (parameters.sides <= 0) {
			return reply("I can't roll a zero sided die...", chatCommand);
		}

		int total = 0;
		int results[] = new int[parameters.times];
		for (int i = 0; i < parameters.times; i++) {
			int result = random.nextInt(parameters.sides) + 1;
			results[i] = result;
			total += result;
		}

		ChatBuilder cb = new ChatBuilder().reply(chatCommand);
		boolean first = true;
		for (int result : results) {
			if (first) {
				first = false;
			} else {
				cb.append(", ");
			}
			cb.append(result);
		}
		if (results.length > 1) {
			cb.nl().append("Total = ").append(total);
		}

		return new ChatResponse(cb);
	}

	private Parameters parseParameters(ChatCommand chatCommand) {
		String content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			//roll 6-sided die
			return new Parameters(1, 6);
		}

		Matcher m = diceRegex.matcher(content);
		if (m.find()) {
			int times = Integer.parseInt(m.group(1));
			int sides = Integer.parseInt(m.group(2));
			return new Parameters(times, sides);
		}

		String[] words = content.split("[\\s,]+");
		return new Parameters(words);
	}

	private class Parameters {
		private final int times, sides;
		private final String[] choices;

		public Parameters(int times, int sides) {
			this.times = times;
			this.sides = sides;
			this.choices = null;
		}

		public Parameters(String[] choices) {
			this.sides = 0;
			this.times = 0;
			this.choices = choices;
		}
	}
}
