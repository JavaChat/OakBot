package oakbot.command;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;

/**
 * A chat bot command.
 * @author Michael Angstadt
 */
public interface Command {
	/**
	 * Gets the command's name. This is what is used to invoke the command.
	 * @return the name
	 */
	String name();

	/**
	 * Gets other names that can be used to invoke the command.
	 * @return the command name aliases
	 */
	default Collection<String> aliases() {
		return Collections.emptyList();
	}

	/**
	 * Gets the command's help documentation.
	 * @return the help documentation
	 */
	HelpDoc help();

	/**
	 * Called when a user invokes this command.
	 * @param chatCommand the command that the user has sent
	 * @param context the bot context
	 * @return the action(s) to perform in response to the message
	 */
	ChatActions onMessage(ChatCommand chatCommand, BotContext context);

	/**
	 * Random number generator.
	 */
	static Random random = new Random();

	/**
	 * Chooses a random element from an array.
	 * @param array the array
	 * @return the random element
	 */
	@SafeVarargs
	static <T> T random(T... array) {
		int index = random.nextInt(array.length);
		return array[index];
	}

	/**
	 * Chooses a random element from a list.
	 * @param list the list
	 * @return the random element
	 */
	static <T> T random(List<T> list) {
		int index = random.nextInt(list.size());
		return list.get(index);
	}
}
