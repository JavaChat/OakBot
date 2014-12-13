package oakbot.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines how a chat message should be split up if it exceeds the max message
 * size.
 * @author Michael Angstadt
 */
public enum SplitStrategy {
	/**
	 * Split by word, appending an ellipsis onto the end of each part.
	 */
	WORD {
		@Override
		public List<String> _split(String message, int maxLength) {
			String ellipsis = "...";
			maxLength -= ellipsis.length();

			List<String> posts = new ArrayList<>();
			while (message.length() > maxLength) {
				//TODO don't split markdown syntax in half
				int spacePos = message.lastIndexOf(' ', maxLength);
				if (spacePos < 0) {
					posts.add(message.substring(0, maxLength) + ellipsis);
					message = message.substring(maxLength);
				} else {
					posts.add(message.substring(0, spacePos) + ellipsis);
					message = message.substring(spacePos + 1);
				}
			}
			posts.add(message);
			return posts;
		}
	},

	/**
	 * Split by newline.
	 */
	NEWLINE {
		@Override
		public List<String> _split(String message, int maxLength) {
			List<String> posts = new ArrayList<>();
			while (message.length() > maxLength) {
				int newlinePos = message.lastIndexOf("\n", maxLength);
				if (newlinePos < 0) {
					posts.add(message.substring(0, maxLength));
					message = message.substring(maxLength);
				} else {
					posts.add(message.substring(0, newlinePos));
					message = message.substring(newlinePos + 1);
				}
			}
			posts.add(message);
			return posts;
		}
	},

	/**
	 * Just truncate the message.
	 */
	NONE {
		@Override
		protected List<String> _split(String message, int maxLength) {
			return Arrays.asList(message.substring(0, maxLength));
		}
	};

	/**
	 * Splits a chat message into multiple parts
	 * @param message the message to split
	 * @param maxLength the max length a message part can be or &lt; 1 for no limit
	 * @return the split parts
	 */
	public List<String> split(String message, int maxLength) {
		if (maxLength < 1 || message.length() <= maxLength) {
			return Arrays.asList(message);
		}
		return _split(message, maxLength);
	}

	protected abstract List<String> _split(String message, int maxLenght);
}
