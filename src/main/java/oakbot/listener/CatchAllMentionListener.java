package oakbot.listener;

/**
 * Used for listeners that respond to mentions containing nothing in particular.
 * Allows other listeners to respond to mentions and tell the "catch all"
 * mention listeners not to respond.
 * @author Michael Angstadt
 */
public interface CatchAllMentionListener extends Listener {
	/**
	 * Tells this listener to not respond to the next message it receives.
	 */
	void ignoreNextMessage();
}
