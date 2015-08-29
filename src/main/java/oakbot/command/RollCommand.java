package oakbot.command;

import java.util.Random;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * Rolls a variable-sided die or makes a choice.
 * @author Michael Angstadt
 */
public class RollCommand implements Command {
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
			.append("Usage: ").append(trigger).append(name()).append(" [SIDES=6] [TIMES=1]").nl()
			.append("Usage: ").append(trigger).append(name()).append(" choice1 choice2 ... choiceN").nl()
			.append("Example: ").append(trigger).append(name()).nl()
			.append("Example: ").append(trigger).append(name()).append(" 20").nl()
			.append("Example: ").append(trigger).append(name()).append(" 20 5").nl()
			.append("Example: ").append(trigger).append(name()).append(" java scala c#")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		ChatResponse response = choiceRoll(message);
		if (response != null) {
			return response;
		}

		return diceRoll(message);
	}

	private ChatResponse choiceRoll(ChatMessage message) {
		String content = message.getContent().trim();
		if (content.isEmpty()) {
			//no words specified, do a dice roll
			return null;
		}

		String[] words = getParameters(content);
		if (isNumber(words[0]) && (words.length < 2 || isNumber(words[1]))) {
			//first two words are numbers, which means it's a dice roll
			return null;
		}

		int index = random.nextInt(words.length);
		String word = words[index];

		//@formatter:off
		return new ChatResponse(new ChatBuilder()
			.reply(message)
			.append(word)
		);
		//@formatter:on

	}

	private boolean isNumber(String value) {
		return value.matches("\\d+");
	}

	private ChatResponse diceRoll(ChatMessage message) {
		int sides = 6;
		int times = 1;
		String content = message.getContent().trim();
		if (!content.isEmpty()) {
			String split[] = getParameters(content);

			if (split.length > 0) {
				try {
					sides = Integer.parseInt(split[0]);
				} catch (NumberFormatException e) {
					//ignore
				}
			}

			if (split.length > 1) {
				try {
					times = Integer.parseInt(split[1]);
				} catch (NumberFormatException e) {
					//ignore
				}
			}
		}

		int results[] = new int[times];
		for (int i = 0; i < times; i++) {
			results[i] = random.nextInt(sides) + 1;
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

		return new ChatResponse(cb);
	}

	private String[] getParameters(String content) {
		return content.split("\\s+");
	}
}
