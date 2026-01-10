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
		if (getContent().isBlank()) {
			return List.of();
		}

		var md = getContentMarkdown().trim();
		return parseArguments(md);
	}

	/**
	 * Parses a markdown string into whitespace-delimited arguments. Handles
	 * quoted strings and escape sequences.
	 * @param text the text to parse
	 * @return the list of arguments
	 */
	private List<String> parseArguments(String text) {
		var args = new ArrayList<String>();
		var parser = new ArgumentParser();
		var it = new CharIterator(text);

		while (it.hasNext()) {
			var c = it.next();
			parser.processCharacter(c, args);
		}

		parser.finalizeCurrentArgument(args);
		return args;
	}

	/**
	 * Helper class to parse command arguments with quote and escape handling.
	 */
	private static class ArgumentParser {
		private final StringBuilder currentArg = new StringBuilder();
		private boolean inQuotes = false;
		private boolean escapeNext = false;

		void processCharacter(char c, List<String> args) {
			if (escapeNext) {
				handleEscapedCharacter(c);
				return;
			}

			if (c == '\\') {
				escapeNext = true;
				return;
			}

			if (c == '"') {
				handleQuote(args);
				return;
			}

			if (Character.isWhitespace(c) && !inQuotes) {
				handleWhitespace(args);
				return;
			}

			currentArg.append(c);
		}

		private void handleEscapedCharacter(char c) {
			currentArg.append(c);
			escapeNext = false;
		}

		private void handleQuote(List<String> args) {
			if (inQuotes) {
				addCurrentArgument(args);
			}
			inQuotes = !inQuotes;
		}

		private void handleWhitespace(List<String> args) {
			if (!currentArg.isEmpty()) {
				addCurrentArgument(args);
			}
		}

		private void addCurrentArgument(List<String> args) {
			args.add(currentArg.toString());
			currentArg.setLength(0);
		}

		private void finalizeCurrentArgument(List<String> args) {
			if (!currentArg.isEmpty()) {
				addCurrentArgument(args);
			}
		}
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
		return message.content().isFixedWidthFont();
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
		var contentObj = message.content();
		if (contentObj == null) {
			return null;
		}

		var content = contentObj.getContent();
		var parts = new CommandStringParser(content).parse();

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

	private static class CommandStringParser {
		private final String content;
		private final CharIterator it;
		private final StringBuilder firstWordBuffer = new StringBuilder();
		private final LinkedList<OpenTag> openTags = new LinkedList<>();

		private boolean inTag = false;
		private boolean inTagName = false;
		private boolean inClosingTag = false;
		private boolean firstNonWhitespaceCharEncountered = false;
		private int tagNameStart = -1;
		private int startOfContent = -1;
		private String curTagName = null;
		private boolean done = false;

		public CommandStringParser(String content) {
			this.content = content;
			it = new CharIterator(content);
		}

		public CommandStringParts parse() {
			while (!done && it.hasNext()) {
				var c = it.next();
				processCharacter(c);
			}

			//@formatter:off
			var openTagsBeforeFirstWord = openTags.stream()
				.map(tag -> tag.entireOpenTag)
			.toList();
			//@formatter:on

			var firstWord = firstWordBuffer.toString();

			return new CommandStringParts(openTagsBeforeFirstWord, firstWord, startOfContent);
		}

		private void processCharacter(char c) {
			if (Character.isWhitespace(c)) {
				handleWhitespace();
				return;
			}

			firstNonWhitespaceCharEncountered = true;

			if (c == '<') {
				handleOpenBracket();
				return;
			}

			if (c == '>') {
				handleCloseBracket();
				return;
			}

			if (c == '/' && it.prev() == '<') {
				handleStartOfCloseTag();
				return;
			}

			if (!inTag) {
				firstWordBuffer.append(c);
				return;
			}
		}

		private void handleWhitespace() {
			if (!firstNonWhitespaceCharEncountered) {
				/*
				 * Ignore all whitespace at the beginning of the message.
				 */
				return;
			}

			if (inTag) {
				/*
				 * If we're inside of a tag name, this marks the end of the tag
				 * name and the start of the tag's attributes.
				 */
				if (inTagName) {
					curTagName = content.substring(tagNameStart, it.index());
					openTags.add(new OpenTag(curTagName));
					inTagName = false;
				}
				return;
			}

			/*
			 * The first whitespace character signals the end of the command
			 * name.
			 */
			startOfContent = it.index() + 1;
			done = true;
		}

		private void handleOpenBracket() {
			inTag = inTagName = true;
			inClosingTag = false;
			tagNameStart = it.index() + 1;
			curTagName = null;
		}

		private void handleCloseBracket() {
			if (curTagName == null) {
				curTagName = content.substring(tagNameStart, it.index());
				if (!inClosingTag) {
					openTags.add(new OpenTag(curTagName));
				}
			}

			if (inClosingTag) {
				/*
				 * Remove any tags from the list that start and end before the
				 * command name is reached.
				 */
				while (!openTags.isEmpty()) {
					var tag = openTags.removeLast();
					if (tag.name.equals(curTagName)) {
						break;
					}
				}
			} else {
				/*
				 * Save the entire opening tag, including the <> characters and
				 * attributes.
				 */
				var tag = openTags.getLast();
				tag.entireOpenTag = content.substring(tagNameStart - 1, it.index() + 1);
			}

			inTag = inTagName = inClosingTag = false;
		}

		void handleStartOfCloseTag() {
			inClosingTag = true;
			tagNameStart = it.index() + 1;
		}
	}

	private static class OpenTag {
		/**
		 * The tag name (e.g. "p").
		 */
		private final String name;

		/**
		 * The entire opening tag, including the &lt;&gt; characters and
		 * attributes (e.g. <code>&lt;p align="center"&gt;</code>)
		 */
		private String entireOpenTag;

		public OpenTag(String name) {
			this.name = name;
		}
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