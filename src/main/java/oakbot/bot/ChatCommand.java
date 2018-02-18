package oakbot.bot;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import oakbot.chat.ChatMessage;
import oakbot.chat.Content;
import oakbot.util.ChatBuilder;

/**
 * Represents a chat message that is in the form of a bot command.
 * @author Michael Angstadt
 */
public class ChatCommand {
	private final ChatMessage message;
	private final String commandName;
	private final String content;

	/**
	 * @param message the original chat message
	 * @param commandName the name of the command
	 * @param content the rest of the text that came after the command name
	 * (must not be null)
	 */
	public ChatCommand(ChatMessage message, String commandName, String content) {
		this.message = message;
		this.commandName = commandName;
		this.content = content;
	}

	/**
	 * Gets the original chat message.
	 * @return the chat message
	 */
	public ChatMessage getMessage() {
		return message;
	}

	/**
	 * <p>
	 * Gets the command name.
	 * </p>
	 * <p>
	 * For example, given the chat message "/define java", this method would
	 * return "define".
	 * <p>
	 * @return the command name
	 */
	public String getCommandName() {
		return commandName;
	}

	/**
	 * <p>
	 * Gets the text that comes after the command name.
	 * </p>
	 * <p>
	 * For example, given the chat message "/define java", this method would
	 * return "java". All whitespace after the command name is excluded. Any
	 * HTML formatting within the content is preserved.
	 * <p>
	 * @return the text or empty string if there is no text after the command
	 * name
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Converts any HTML formatting in the text that comes after the command
	 * name to Markdown syntax.
	 * @return the Markdown text or empty string if there is no text after the
	 * command name
	 */
	public String getContentMarkdown() {
		return ChatBuilder.toMarkdown(content, isFixedFont());
	}

	/**
	 * Determines whether the command is formatted in a monospace font.
	 * @return true if it's formatted in a monospace font, false if not
	 */
	public boolean isFixedFont() {
		return message.getContent().isFixedFont();
	}

	/**
	 * Parses the components of a chat command out of a chat message.
	 * @param message the chat message to parse
	 * @param trigger the command trigger or null to treat the first word in the
	 * message as the command name
	 * @return the chat command or null if the chat message is not a chat
	 * command
	 */
	public static ChatCommand fromMessage(ChatMessage message, String trigger) {
		Content contentObj = message.getContent();
		if (contentObj == null) {
			return null;
		}

		String content = contentObj.getContent();
		StringBuilder nameBuffer = new StringBuilder();
		List<String[]> openTags = new ArrayList<>();
		boolean inTag = false, inTagName = false, inClosingTag = false;
		boolean nonWhitespaceCharEncountered = false;
		int tagNameStart = -1;
		int startOfText = -1;
		String curTagName = null;
		char prev = 0;
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);

			if (Character.isWhitespace(c)) {
				if (!nonWhitespaceCharEncountered) {
					/*
					 * Ignore all whitespace at the beginning of the message.
					 */
					prev = c;
					continue;
				}

				if (inTag) {
					if (inTagName) {
						curTagName = content.substring(tagNameStart, i);
						openTags.add(new String[] { curTagName, null });
						inTagName = false;
					}
					prev = c;
					continue;
				}

				/*
				 * The first whitespace character signals the end of the command
				 * name.
				 */
				startOfText = i + 1;
				break;
			}

			nonWhitespaceCharEncountered = true;

			switch (c) {
			case '<':
				inTag = inTagName = true;
				inClosingTag = false;
				tagNameStart = i + 1;
				curTagName = null;
				break;
			case '>':
				if (curTagName == null) {
					curTagName = content.substring(tagNameStart, i);
					if (!inClosingTag) {
						openTags.add(new String[] { curTagName, null });
					}
				}

				if (inClosingTag) {
					while (true) {
						if (openTags.isEmpty()) {
							break;
						}

						String tag[] = openTags.remove(openTags.size() - 1);
						if (tag[0].equals(curTagName)) {
							break;
						}
					}
				} else {
					openTags.get(openTags.size() - 1)[1] = content.substring(tagNameStart - 1, i + 1);
				}

				inTag = inTagName = inClosingTag = false;
				break;
			case '/':
				if (prev == '<') {
					inClosingTag = true;
					tagNameStart = i + 1;
					break;
				}
				//break; don't break, go to default
			default:
				if (!inTag) {
					nameBuffer.append(c);
				}
				break;
			}

			prev = c;
		}

		String name = StringEscapeUtils.unescapeHtml4(nameBuffer.toString());
		if (trigger != null) {
			if (!name.startsWith(trigger)) {
				return null;
			}

			//remove the trigger
			name = name.substring(trigger.length());
		}

		if (name.isEmpty()) {
			return null;
		}

		String text;
		if (startOfText < 0) {
			text = "";
		} else {
			text = content.substring(startOfText);
			if (!contentObj.isFixedFont()) {
				text = text.trim();
			}
		}

		StringBuilder sb = new StringBuilder();
		for (String[] tag : openTags) {
			sb.append(tag[1]);
		}
		text = sb + text;

		return new ChatCommand(message, name, text);
	}

	@Override
	public String toString() {
		return "ChatCommand [message=" + message + ", commandName=" + commandName + ", content=" + content + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commandName == null) ? 0 : commandName.hashCode());
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ChatCommand other = (ChatCommand) obj;
		if (commandName == null) {
			if (other.commandName != null) return false;
		} else if (!commandName.equals(other.commandName)) return false;
		if (content == null) {
			if (other.content != null) return false;
		} else if (!content.equals(other.content)) return false;
		if (message == null) {
			if (other.message != null) return false;
		} else if (!message.equals(other.message)) return false;
		return true;
	}
}
