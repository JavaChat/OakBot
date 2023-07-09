package oakbot.bot;

import java.util.Objects;

import oakbot.chat.SplitStrategy;

/**
 * Instructs the bot to post a chat message.
 * @author Michael Angstadt
 */
public class PostMessage implements ChatAction {
	private String message, condensedMessage;
	private SplitStrategy splitStrategy = SplitStrategy.NONE;
	private boolean bypassFilters, ephemeral;

	/**
	 * @param message the message to post
	 */
	public PostMessage(CharSequence message) {
		message(message);
	}

	/**
	 * Gets the message to post.
	 * @return the message
	 */
	public String message() {
		return message;
	}

	/**
	 * Sets the message to post.
	 * @param message the message
	 * @return this
	 */
	public PostMessage message(CharSequence message) {
		this.message = (message == null) ? null : message.toString();
		return this;
	}

	/**
	 * Gets the text that the original message will be replaced with after the
	 * amount of time specified in the "hideOneboxesAfter" setting has elapsed.
	 * <p>
	 * This new content should be short because the {@link #splitStrategy()}
	 * will not be applied to the condensed message.
	 * <p>
	 * Note that if {@link #ephemeral()} is set to {@code true}, the condensed
	 * message will be ignored.
	 * @return the condensed message or null not to change anything
	 */
	public String condensedMessage() {
		return condensedMessage;
	}

	/**
	 * Sets the text that the original message will be replaced with after the
	 * amount of time specified in the "hideOneboxesAfter" setting has elapsed.
	 * <p>
	 * This new content should be short because the {@link #splitStrategy()}
	 * will not be applied to the condensed message.
	 * <p>
	 * Note that if {@link #ephemeral()} is set to {@code true}, the condensed
	 * message will be ignored.
	 * @param condensedMessage the condensed message or null not to change
	 * anything
	 */
	public PostMessage condensedMessage(CharSequence condensedMessage) {
		this.condensedMessage = (condensedMessage == null) ? null : condensedMessage.toString();
		return this;
	}

	/**
	 * Determines how the message should be split up if it exceeds length
	 * limitations. By default, the message will be truncated.
	 * @return the split strategy
	 */
	public SplitStrategy splitStrategy() {
		return splitStrategy;
	}

	/**
	 * Sets how the message should be split up if it exceeds length limitations.
	 * By default, the message will be truncated.
	 * @param splitStrategy the split strategy
	 */
	public PostMessage splitStrategy(SplitStrategy splitStrategy) {
		this.splitStrategy = splitStrategy;
		return this;
	}

	/**
	 * Determines whether the message should bypass any filters that are in
	 * place.
	 * @return true to bypass all filters, false not to
	 */
	public boolean bypassFilters() {
		return bypassFilters;
	}

	/**
	 * Sets whether the message should bypass any filters that are in place.
	 * @param bypassFilters true to bypass all filters, false not to
	 * @return this
	 */
	public PostMessage bypassFilters(boolean bypassFilters) {
		this.bypassFilters = bypassFilters;
		return this;
	}

	/**
	 * Determines if the message should be deleted after the amount of time
	 * specified in the "hideOneboxesAfter" setting has elapsed.
	 * <p>
	 * Note that if this is set to {@code true}, the {@link #condensedMessage()}
	 * will be ignored.
	 * @return true to delete the message, false not to
	 */
	public boolean ephemeral() {
		return ephemeral;
	}

	/**
	 * Sets whether the message should be deleted after the amount of time
	 * specified in the "hideOneboxesAfter" setting has elapsed.
	 * <p>
	 * Note that if this is set to {@code true}, the {@link #condensedMessage()}
	 * will be ignored.
	 * @param ephemeral true to delete the message, false not to
	 */
	public PostMessage ephemeral(boolean ephemeral) {
		this.ephemeral = ephemeral;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bypassFilters, condensedMessage, ephemeral, message, splitStrategy);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PostMessage other = (PostMessage) obj;
		return bypassFilters == other.bypassFilters && Objects.equals(condensedMessage, other.condensedMessage) && ephemeral == other.ephemeral && Objects.equals(message, other.message) && splitStrategy == other.splitStrategy;
	}

	@Override
	public String toString() {
		return "PostMessage [message=" + message + ", condensedMessage=" + condensedMessage + ", splitStrategy=" + splitStrategy + ", bypassFilters=" + bypassFilters + ", ephemeral=" + ephemeral + "]";
	}

}
