package oakbot.bot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.util.ChatBuilder;

/**
 * Contains a list of actions to perform in response to a chat message.
 * @author Michael Angstadt
 */
public class ChatActions implements Iterable<ChatAction> {
	private final List<ChatAction> actions;

	/**
	 * Convenience method for creating a new list of actions.
	 * @param actions the list of actions
	 * @return the created object
	 */
	public static ChatActions create(ChatAction... actions) {
		return new ChatActions(List.of(actions));
	}

	/**
	 * Convenience method for creating an empty list of actions.
	 * @return the created object
	 */
	public static ChatActions doNothing() {
		return ChatActions.create();
	}

	/**
	 * Convenience method for creating a list with a single {@link PostMessage}
	 * action.
	 * @param message the message to post
	 * @return the created object
	 */
	public static ChatActions post(CharSequence message) {
		//@formatter:off
		return ChatActions.create(
			new PostMessage(message)
		);
		//@formatter:on
	}

	/**
	 * Convenience method for creating a list with a single
	 * {@link PostMessage} action that is a reply to a chat message.
	 * @param message the message to post
	 * @param parent the message to reply to
	 * @return the created object
	 */
	public static ChatActions reply(String message, ChatMessage parent) {
		//@formatter:off
		return post(new ChatBuilder()
			.reply(parent)
			.append(message)
		);
		//@formatter:on
	}

	/**
	 * Convenience method for creating a list with a single
	 * {@link PostMessage} action that is a reply to a chat message.
	 * @param message the message to post
	 * @param parent the message to reply to
	 * @return the created object
	 */
	public static ChatActions reply(String message, ChatCommand parent) {
		return reply(message, parent.getMessage());
	}

	/**
	 * Convenience method for creating a list with a single {@link PostMessage}
	 * action that is a reply to a chat message and that contains an exception
	 * message.
	 * @param message the message to post (should end in ": ")
	 * @param exception the exception that was thrown
	 * @param parent the message to reply to
	 * @return the created object
	 */
	public static ChatActions error(CharSequence message, Exception exception, ChatCommand parent) {
		//@formatter:off
		return post(new ChatBuilder()
			.reply(parent)
			.append(message)
			.code(exception.getMessage())
		);
		//@formatter:on
	}

	/**
	 * Creates an empty list of actions.
	 */
	public ChatActions() {
		this(new ArrayList<>());
	}

	/**
	 * Initializes the list of actions with the given list.
	 * @param actions the actions
	 */
	public ChatActions(List<ChatAction> actions) {
		this.actions = actions;
	}

	/**
	 * Adds an action to the list.
	 * @param action the action to add
	 * @return this
	 */
	public ChatActions addAction(ChatAction action) {
		actions.add(action);
		return this;
	}

	/**
	 * Adds all of the actions from the given list.
	 * @param actions the actions to add
	 * @return this
	 */
	public ChatActions addAll(ChatActions actions) {
		this.actions.addAll(actions.actions);
		return this;
	}

	/**
	 * Gets the actions in this list.
	 * @return the actions
	 */
	public List<ChatAction> getActions() {
		return actions;
	}

	/**
	 * Determines if the list is empty.
	 * @return true if empty, false if not
	 */
	public boolean isEmpty() {
		return actions.isEmpty();
	}

	@Override
	public Iterator<ChatAction> iterator() {
		return actions.iterator();
	}
}
