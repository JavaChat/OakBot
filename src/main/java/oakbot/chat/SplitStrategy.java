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
			//figure out where it's not safe to split the string
			boolean inMarkdown[];
			{
				boolean inBold = false, inItalic = false, inCode = false, inTag = false, inLink = false;
				String specialChars = "`*[]"; //note: I don't escape () or _ in DescriptionNodeVisitor, so I'm not going to treat these characters as escapable
				inMarkdown = new boolean[message.length()];

				for (int i = 0; i < message.length(); i++) {
					char cur = message.charAt(i);
					char next = (i == message.length() - 1) ? 0 : message.charAt(i + 1);

					boolean skipAheadOne = false;
					switch (cur) {
					case '\\':
						skipAheadOne = (inCode && next == '`') || (!inCode && specialChars.indexOf(next) >= 0);
						break;
					case '`':
						inCode = !inCode;
						break;
					case '*':
						if (!inCode) {
							if (next == '*') {
								inBold = !inBold;
								skipAheadOne = true;
							} else {
								inItalic = !inItalic;
							}
						}
						break;
					case '[':
						if (!inCode) {
							if (i < message.length() - 4) {
								if (message.substring(i + 1, i + 5).equals("tag:")) {
									inTag = true;
								}
							}
							inLink = true;
						}
						break;
					case ']':
						if (inLink) {
							if (next != '(') {
								//it's not a link, just some brackets!
								inLink = false;
							}
						}
						if (inTag) {
							inTag = false;
						}
						break;
					case ')':
						//assumes there are no parens in the URL or title string
						inLink = false;
						break;
					}

					if (skipAheadOne) {
						inMarkdown[i] = inMarkdown[i + 1] = true;
						i++;
					} else {
						inMarkdown[i] = (inBold || inItalic || inCode || inLink || inTag);
					}
				}
			}

			String ellipsis = " ...";
			maxLength -= ellipsis.length();

			List<String> posts = new ArrayList<>();
			while (message.length() > maxLength) {
				int spacePos = message.lastIndexOf(' ', maxLength);
				if (spacePos < 0) {
					posts.add(message.substring(0, maxLength) + ellipsis);
					message = message.substring(maxLength);
					inMarkdown = Arrays.copyOfRange(inMarkdown, maxLength, inMarkdown.length);
				} else {
					while (spacePos >= 0 && inMarkdown[spacePos]) {
						spacePos = message.lastIndexOf(' ', spacePos-1);
					}
					posts.add(message.substring(0, spacePos) + ellipsis);
					message = message.substring(spacePos + 1);
					inMarkdown = Arrays.copyOfRange(inMarkdown, spacePos + 1, inMarkdown.length);
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
	 * @param maxLength the max length a message part can be or &lt; 1 for no
	 * limit
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
