package oakbot.bot;

import oakbot.command.Command;

/**
 * Handles a unrecognized command.
 * @author Michael Angstadt
 */
public interface UnknownCommandHandler extends Command {
	default String name() {
		return null;
	}

	default String description() {
		return null;
	}

	default String helpText(String trigger) {
		return null;
	}
}
