package oakbot.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import oakbot.util.ChatBuilder;

/**
 * Helper class that standardizes the way help documentation is formatted across
 * all commands.
 * @author Michael Angstadt
 */
public class HelpBuilder {
	private final Command command;
	private final String trigger;

	private String description, detail;
	private final List<String[]> examples = new ArrayList<>();

	/**
	 * @param trigger the bot's trigger
	 * @param command the command instance
	 */
	public HelpBuilder(String trigger, Command command) {
		this.trigger = trigger;
		this.command = command;
	}

	/**
	 * The full description of the command. This completely replaces the
	 * description returned by {@link Command#description}.
	 * @param description the description
	 * @return this
	 */
	public HelpBuilder description(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Any additional detail to include about the command. This will come after
	 * whatever is returned by {@link Command#description()} (unless an
	 * alternate description was provided with {@link #description()}.
	 * @param detail the additional details
	 * @return this
	 */
	public HelpBuilder detail(String detail) {
		this.detail = detail;
		return this;
	}

	/**
	 * Adds an example for how the command can be invoked.
	 * @param parameters the command's parameters, or empty string for no
	 * parameters
	 * @param description the description of this example
	 * @return this
	 */
	public HelpBuilder example(String parameters, String description) {
		examples.add(new String[] { parameters, description });
		return this;
	}

	/**
	 * Builds the help message to post to the chat room.
	 * @return the help message
	 */
	@Override
	public String toString() {
		ChatBuilder cb = new ChatBuilder();
		cb.append(command.name()).nl();

		cb.append(description == null ? command.description() : description);
		if (detail != null) {
			cb.append(' ').append(detail);
		}

		if (!examples.isEmpty()) {
			cb.nl().nl().append(plural("Example", examples.size())).append(":");
			for (String[] example : examples) {
				String parameters = example[0];
				String description = example[1];

				cb.nl().append(trigger).append(command.name());
				if (!parameters.isEmpty()) {
					cb.append(' ').append(parameters);
				}
				if (!description.isEmpty()) {
					cb.append(" : ").append(description);
				}
			}
		}

		Collection<String> aliases = command.aliases();
		if (!aliases.isEmpty()) {
			cb.nl().nl().append(plural("Alias", aliases.size())).append(": ").append(String.join(",", aliases));
		}

		return cb.toString();
	}

	/**
	 * Determines if a word should be plural.
	 * @param word the singular version of the word
	 * @param number the number
	 * @return the plural or singular version of the word, depending on the
	 * provided number
	 */
	private static String plural(String word, int number) {
		if (number == 1) {
			return word;
		}

		return word + (word.endsWith("s") ? "es" : "s");
	}
}
