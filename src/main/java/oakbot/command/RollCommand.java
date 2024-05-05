package oakbot.command;

import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.util.ChatBuilder;
import oakbot.util.Rng;

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
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Rolls a variable-sided die or makes a choice.")
			.example("", "Rolls a six-sided die.")
			.example("2d20", "Rolls two twenty-sided dice.")
			.example("vi emacs", "Randomly chooses one of the specified keywords (\"vi\" or \"emacs\").")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var parameters = parseParameters(chatCommand);
		if (parameters.choices != null) {
			var choice = Rng.random(parameters.choices);
			return reply(choice, chatCommand);
		}

		if (parameters.times > 100) {
			return reply("Sorry, they don't pay me enough for that.", chatCommand);
		}

		if (parameters.times <= 0) {
			//@formatter:off
			return post(new ChatBuilder()
				.reply(chatCommand)
				.italic("rolls nothing")
			);
			//@formatter:on
		}

		if (parameters.sides <= 0) {
			return reply("I can't roll a zero sided die...", chatCommand);
		}

		//@formatter:off
		var results = IntStream.range(0, parameters.times)
			.map(i -> Rng.next(parameters.sides) + 1)
		.toArray();
		//@formatter:on

		var cb = new ChatBuilder();
		cb.reply(chatCommand);

		//@formatter:off
		cb.append(Arrays.stream(results)
			.mapToObj(Integer::toString)
		.collect(Collectors.joining(", ")));
		//@formatter:on

		if (results.length > 1) {
			var total = Arrays.stream(results).sum();
			var average = (double) total / results.length;
			cb.nl().append("Total = ").append(total);
			cb.nl().append("Average = ").append(Double.toString(average));
		}

		return post(cb);
	}

	private Parameters parseParameters(ChatCommand chatCommand) {
		var content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			//roll 6-sided die
			return new Parameters(1, 6);
		}

		var m = diceRegex.matcher(content);
		if (m.find()) {
			var times = Integer.parseInt(m.group(1));
			var sides = Integer.parseInt(m.group(2));
			return new Parameters(times, sides);
		}

		var words = content.split("[\\s,]+");
		return new Parameters(words);
	}

	private static class Parameters {
		private final int times;
		private final int sides;
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
