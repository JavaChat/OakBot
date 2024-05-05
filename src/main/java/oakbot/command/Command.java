package oakbot.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;

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
	 * @param bot the bot instance
	 * @return the action(s) to perform in response to the message
	 */
	ChatActions onMessage(ChatCommand chatCommand, IBot bot);

	/**
	 * Determines if the given chat message is invoking this command.
	 * @param message the message
	 * @param trigger the bot's command trigger
	 * @return true if the message is invoking this command, false if not
	 */
	default boolean isInvokingMe(ChatMessage message, String trigger) {
		var content = message.getContent().getContent();

		var names = new ArrayList<String>();
		names.add(name());
		names.addAll(aliases());

		return names.stream().anyMatch(name -> {
			var invocation = trigger + name;
			return content.equals(invocation) || content.startsWith(invocation + " ");
		});
	}
}
