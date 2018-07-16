package oakbot.bot;

import oakbot.chat.SplitStrategy;

/**
 * A message to send in response to a command.
 * @author Michael Angstadt
 */
public class ChatResponse {
	private final String message, condensedMessage;
	private final SplitStrategy splitStrategy;
	private final boolean bypassFilters;

	/**
	 * @param message the message to post
	 */
	public ChatResponse(CharSequence message) {
		this(message, SplitStrategy.NONE);
	}

	/**
	 * @param message the message to post
	 * @param splitStrategy how the message should be split up if it exceeds
	 * length limitations
	 */
	public ChatResponse(CharSequence message, SplitStrategy splitStrategy) {
		this(message, splitStrategy, false);
	}

	/**
	 * @param message the message to post
	 * @param splitStrategy how the message should be split up if it exceeds
	 * length limitations
	 * @param bypassFilters true to bypass any filters that are in place, false
	 * not to
	 */
	public ChatResponse(CharSequence message, SplitStrategy splitStrategy, boolean bypassFilters) {
		this(message, splitStrategy, bypassFilters, false);
	}

	/**
	 * @param message the message to post
	 * @param splitStrategy how the message should be split up if it exceeds
	 * length limitations
	 * @param bypassFilters true to bypass any filters that are in place, false
	 * not to
	 * @param deleteMessage true to delete the message after the amount of time
	 * specified in the "hideOneboxesAfter" setting has elapsed, false to leave
	 * it alone
	 */
	public ChatResponse(CharSequence message, SplitStrategy splitStrategy, boolean bypassFilters, boolean deleteMessage) {
		this(message, splitStrategy, bypassFilters, deleteMessage ? "" : null);
	}

	/**
	 * @param message the message to post
	 * @param splitStrategy how the message should be split up if it exceeds
	 * length limitations
	 * @param bypassFilters true to bypass any filters that are in place, false
	 * not to
	 * @param condensedMessage the text that the original message should be
	 * changed to after the amount of time specified in the "hideOneboxesAfter"
	 * setting has elapsed. This new content should be short, as it is not
	 * checked for length limitations.
	 */
	public ChatResponse(CharSequence message, SplitStrategy splitStrategy, boolean bypassFilters, CharSequence condensedMessage) {
		this.message = message.toString();
		this.splitStrategy = splitStrategy;
		this.bypassFilters = bypassFilters;
		this.condensedMessage = (condensedMessage == null) ? null : condensedMessage.toString();
	}

	/**
	 * Gets the message to post.
	 * @return the message to post
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the text that the original message should be changed to after the
	 * amount of time specified in the "hideOneboxesAfter" setting has elapsed.
	 * This new content should be short, as it is not checked for length
	 * limitations
	 * @return the new message, empty string to delete the message, or null not
	 * to change anything
	 */
	public String getCondensedMessage() {
		return condensedMessage;
	}

	/**
	 * Determines how the message should be split up if it exceeds length
	 * limitations.
	 * @return the split strategy
	 */
	public SplitStrategy getSplitStrategy() {
		return splitStrategy;
	}

	/**
	 * Determines whether the message should bypass any filters that are in
	 * place.
	 * @return true to bypass all filters, false not to
	 */
	public boolean isBypassFilters() {
		return bypassFilters;
	}
}
