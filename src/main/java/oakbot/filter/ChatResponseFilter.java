package oakbot.filter;

/**
 * Modifies the content of a chat message before it is sent.
 * @author Michael Angstadt
 */
public abstract class ChatResponseFilter {
	/**
	 * True if the filter is enabled, false if not.
	 */
	protected boolean enabled = true;

	/**
	 * Determines if the filter is enabled. It only processes messages if it is
	 * enabled.
	 * @return true if enabled, false if not
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables or disables the filter.
	 * @param enabled true to enable, false to disable
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Performs the filter operation.
	 * @param message the message to filter
	 * @return the filtered message
	 */
	public abstract String filter(String message);
}
