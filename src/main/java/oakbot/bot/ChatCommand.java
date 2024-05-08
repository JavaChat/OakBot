package oakbot.bot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.util.CharIterator;
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
	 * <p>
	 * Gets the text that comes after the command name, parsed as
	 * whitespace-delimited arguments. Arguments that are surrounded in
	 * double-quotes can contain whitespace. Text is converted to markdown.
	 * </p>
	 * <p>
	 * For example, given the chat message:<br>
	 * {@code /define <b>java</b> "foo bar" 2}<br>
	 * this method would return the array<br>
	 * {@code ["**java**", "foo bar", "2"]}
	 * </p>
	 * @return the arguments or empty list if there is no text after the command
	 * name
	 */
	public List<String> getContentAsArgs() {
		if (getContent().trim().isEmpty()) {
			return List.of();
		}

		var md = getContentMarkdown().trim();
		var args = new ArrayList<String>();

		var inQuotes = false;
		var escapeNext = false;
		var sb = new StringBuilder();
		var it = new CharIterator(md);
		while (it.hasNext()) {
			var c = it.next();

			if (escapeNext) {
				sb.append(c);
				escapeNext = false;
				continue;
			}

			if (Character.isWhitespace(c) && !inQuotes) {
				if (!sb.isEmpty()) {
					args.add(sb.toString());
					sb.setLength(0);
				}
				continue;
			}

			if (c == '"') {
				if (inQuotes) {
					args.add(sb.toString());
					sb.setLength(0);
				}
				inQuotes = !inQuotes;
				continue;
			}

			if (c == '\\') {
				escapeNext = true;
				continue;
			}

			sb.append(c);
		}

		if (!sb.isEmpty()) {
			args.add(sb.toString());
		}

		return args;
	}

	/**
	 * Converts any HTML formatting in the text that comes after the command
	 * name to Markdown syntax.
	 * @return the Markdown text or empty string if there is no text after the
	 * command name
	 */
	public String getContentMarkdown() {
		return ChatBuilder.toMarkdown(content, isFixedWidthFont());
	}

	/**
	 * Determines whether the command is formatted in a monospace font.
	 * @return true if it's formatted in a monospace font, false if not
	 */
	public boolean isFixedWidthFont() {
		return message.getContent().isFixedWidthFont();
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
		var contentObj = message.getContent();
		if (contentObj == null) {
			return null;
		}

		var content = contentObj.getContent();
		var parts = extractParts(content);

		var name = StringEscapeUtils.unescapeHtml4(parts.firstWord);
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

		String commandContent;
		if (parts.startOfContent < 0) {
			commandContent = "";
		} else {
			commandContent = content.substring(parts.startOfContent);
			if (!contentObj.isFixedWidthFont()) {
				commandContent = commandContent.trim();
			}
		}

		var openTags = String.join("", parts.openTagsBeforeFirstWord);
		commandContent = openTags + commandContent;

		return new ChatCommand(message, name, commandContent);
	}

	private static CommandStringParts extractParts(String content) {
		var firstWordBuffer = new StringBuilder();
		var inTag = false;
		var inTagName = false;
		var inClosingTag = false;
		var firstNonWhitespaceCharEncountered = false;
		var tagNameStart = -1;
		var startOfContent = -1;
		String curTagName = null;

		/*
		 * Index 0: The tag name.
		 * Index 1: The entire opening tag, including the <> characters and
		 * attributes.
		 */
		var openTags = new LinkedList<String[]>();

		var it = new CharIterator(content);
		while (it.hasNext()) {
			var c = it.next();

			if (Character.isWhitespace(c)) {
				if (!firstNonWhitespaceCharEncountered) {
					/*
					 * Ignore all whitespace at the beginning of the message.
					 */
					continue;
				}

				if (inTag) {
					/*
					 * If we're inside of a tag name, this marks the end of the
					 * tag name and the start of the tag's attributes.
					 */
					if (inTagName) {
						curTagName = content.substring(tagNameStart, it.index());
						openTags.add(new String[] { curTagName, null });
						inTagName = false;
					}
					continue;
				}

				/*
				 * The first whitespace character signals the end of the command
				 * name.
				 */
				startOfContent = it.index() + 1;
				break;
			}

			firstNonWhitespaceCharEncountered = true;

			if (c == '<') {
				inTag = inTagName = true;
				inClosingTag = false;
				tagNameStart = it.index() + 1;
				curTagName = null;
				continue;
			}

			if (c == '>') {
				if (curTagName == null) {
					curTagName = content.substring(tagNameStart, it.index());
					if (!inClosingTag) {
						openTags.add(new String[] { curTagName, null });
					}
				}

				if (inClosingTag) {
					/*
					 * Remove any tags from the list that start and end before
					 * the command name is reached.
					 */
					while (!openTags.isEmpty()) {
						var tag = openTags.removeLast();
						if (tag[0].equals(curTagName)) {
							break;
						}
					}
				} else {
					/*
					 * Save the entire opening tag, including the <> characters
					 * and attributes.
					 */
					var tag = openTags.getLast();
					var entireOpenTag = content.substring(tagNameStart - 1, it.index() + 1);
					tag[1] = entireOpenTag;
				}

				inTag = inTagName = inClosingTag = false;
				continue;
			}

			if (c == '/' && it.prev() == '<') {
				inClosingTag = true;
				tagNameStart = it.index() + 1;
				continue;
			}

			if (!inTag) {
				firstWordBuffer.append(c);
				continue;
			}
		}

		//@formatter:off
		var openTagsBeforeFirstWord = openTags.stream()
			.map(tag -> tag[1])
		.toList();
		//@formatter:on

		var firstWord = firstWordBuffer.toString();

		return new CommandStringParts(openTagsBeforeFirstWord, firstWord, startOfContent);
	}

	private record CommandStringParts(List<String> openTagsBeforeFirstWord, String firstWord, int startOfContent) {
	}

	@Override
	public String toString() {
		return "ChatCommand [message=" + message + ", commandName=" + commandName + ", content=" + content + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(commandName, content, message);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		var other = (ChatCommand) obj;
		return Objects.equals(commandName, other.commandName) && Objects.equals(content, other.content) && Objects.equals(message, other.message);
	}
}
