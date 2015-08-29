package oakbot.command;

import java.util.Random;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * Rolls a variable-sided die.
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
		return "Rolls a variable-sided die.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append("Rolls a variable-sided die.").nl()
			.append("Usage: ").append(trigger).append(name()).append(" [SIDES=6] [TIMES=1]").nl()
			.append("Example: ").append(trigger).append(name()).nl()
			.append("Example: ").append(trigger).append(name()).append(" 20").nl()
			.append("Example: ").append(trigger).append(name()).append(" 20 5")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		int sides = 6;
		int times = 1;
		String content = message.getContent().trim();
		if (!content.isEmpty()) {
			String split[] = content.split("\\s+");
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
}
