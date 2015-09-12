package oakbot.command;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * Rolls a variable-sided die or makes a choice.
 * @author Michael Angstadt
 */
public class RollCommand implements Command {
	private final Pattern diceRegex = Pattern.compile("^(\\d+)d(\\d+)$", Pattern.CASE_INSENSITIVE);
	private final Random random = new Random();

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
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		Parameters parameters = parseParameters(message);
		if (parameters.choices != null) {
			int index = random.nextInt(parameters.choices.length);
			String choice = parameters.choices[index];

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append(choice)
			);
			//@formatter:on
		}

		if (parameters.times > 100) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("I'm sorry, I can't do that, Dave.")
			);
			//@formatter:on
		}

		int total = 0;
		int results[] = new int[parameters.times];
		for (int i = 0; i < parameters.times; i++) {
			int result = random.nextInt(parameters.sides) + 1;
			results[i] = result;
			total += result;
		}

		ChatBuilder cb = new ChatBuilder().reply(message);
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

	private Parameters parseParameters(ChatMessage message) {
		String content = message.getContent().trim();
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
