package oakbot.command.javadoc;

import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.SplitStrategy;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;

/**
 * The command class for the chat bot.
 * @author Michael Angstadt
 */
public class JavadocCommand implements Command, Listener {
	private static final Logger logger = LoggerFactory.getLogger(JavadocCommand.class);

	/**
	 * Stop responding to numeric choices the user enters after this amount of
	 * time.
	 */
	private static final Duration choiceTimeout = Duration.ofSeconds(30);

	/**
	 * "Flags" that a class can have. They are defined in a List because, if a
	 * class has multiple modifiers, I want them to be displayed in a consistent
	 * order.
	 */
	private static final List<String> classModifiers = List.of("abstract", "final");

	/**
	 * The list of possible "class types". Each class *should* have exactly one
	 * type, but there's no explicit check for this (things shouldn't break if a
	 * class does not have exactly one).
	 */
	private static final Set<String> classTypes = Set.of("annotation", "class", "enum", "exception", "interface");

	/**
	 * The method modifiers to ignore when outputting a method to the chat.
	 */
	private static final Set<String> methodModifiersToIgnore = Set.of("private", "protected", "public");

	/**
	 * Used for accessing the Javadoc information.
	 */
	private final JavadocDao dao;

	/**
	 * <p>
	 * Persists the state of the interactions the bot is having with its users.
	 * Each room is kept separate so they don't interfere with each other.
	 * </p>
	 * <p>
	 * Key = room ID<br>
	 * Value = state information
	 * </p>
	 */
	private final Map<Integer, Conversation> conversations = new HashMap<>();

	/**
	 * @param dao the Javadoc DAO
	 */
	public JavadocCommand(JavadocDao dao) {
		this.dao = dao;
	}

	@Override
	public String name() {
		return "javadoc";
	}

	@Override
	public Collection<String> aliases() {
		return List.of("javadocs");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((Command)this)
			.summary("Displays class documentation from the Javadocs.")
			.detail("If more than one class or method matches the query, then a list of choices is displayed. Queries are case-insensitive.")
			.example("String", "Searches for all classes named \"String\".")
			.example("java.lang.String#substring", "Searches for all methods in the \"java.lang.String\" class called \"substring\".")
			.example("java.lang.String#substring(int)", "Searches for a method in the \"java.lang.String\" class called \"substring\" that has a single \"int\" parameter.")
			.example("java.lang.String#substring(int) JonSkeet", "Same as above, but directs the response to a specific user.")
			.example("java.lang.String#substring(int) 2", "Displays the second paragraph of the javadoc description.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContent();
		if (content.isEmpty()) {
			return reply("Type the name of a Java class (e.g. \"java.lang.String\") or a method (e.g. \"Integer#parseInt\").", chatCommand);
		}

		var arguments = JavadocCommandArguments.parse(content);

		try {
			var fullyQualifiedNames = dao.search(arguments.className());

			if (fullyQualifiedNames.isEmpty()) {
				return handleNoMatch(chatCommand);
			}

			if (fullyQualifiedNames.size() > 1) {
				return handleMultipleMatches(arguments, fullyQualifiedNames, chatCommand);
			}

			var fullyQualifiedName = fullyQualifiedNames.iterator().next();
			var info = dao.getClassInfo(fullyQualifiedName);
			if (info != null) {
				return handleSingleMatch(arguments, info, chatCommand);
			}

			/*
			 * We should never get here, since we got the fully-qualified name
			 * from "dao.search()".
			 */
			return handleNoMatch(chatCommand);
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem getting Javadoc info from the DAO.");
			return error("Error getting Javadoc info: ", e, chatCommand);
		}
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		var waitingForChoice = conversations.containsKey(message.roomId());
		if (!waitingForChoice) {
			return doNothing();
		}

		var content = message.content().getContent();

		int num;
		try {
			num = Integer.parseInt(content);
		} catch (NumberFormatException e) {
			return doNothing();
		}

		return showChoice(message, num);
	}

	private ChatActions handleNoMatch(ChatCommand message) {
		return reply("Sorry, I never heard of that class. :(", message);
	}

	private ChatActions handleSingleMatch(JavadocCommandArguments arguments, ClassInfo info, ChatCommand message) throws IOException {
		if (arguments.methodName() == null) {
			//method name not specified, so print the class docs
			return printClass(info, arguments, message);
		}

		//print the method docs
		var matchingMethods = findMatchingMethods(info, arguments.methodName(), arguments.parameters());

		if (matchingMethods.isEmpty()) {
			//no matches found
			return reply("Sorry, I can't find that method. :(", message);
		}

		if (matchingMethods.exactSignature != null) {
			//an exact match was found!
			return printMethod(matchingMethods.exactSignature, arguments, message);
		}

		if (matchingMethods.matchingName.size() == 1 && arguments.parameters() == null) {
			return printMethod(matchingMethods.matchingName.get(0), arguments, message);
		}

		//print the methods with the same name
		Multimap<ClassInfo, MethodInfo> map = ArrayListMultimap.create();
		map.putAll(info, matchingMethods.matchingName);
		return printMethodChoices(map, arguments, message);
	}

	private ChatActions handleMultipleMatches(JavadocCommandArguments arguments, Collection<String> matches, ChatCommand message) throws IOException {
		if (arguments.methodName() == null) {
			//user is just querying for a class, so print the class choices
			return printClassChoices(matches, arguments, message);
		}

		//user is querying for a method

		//search each class for a method that matches the given signature
		var exactMatches = new HashMap<ClassInfo, MethodInfo>();
		Multimap<ClassInfo, MethodInfo> matchingNames = ArrayListMultimap.create();
		for (var className : matches) {
			var classInfo = dao.getClassInfo(className);
			var methods = findMatchingMethods(classInfo, arguments.methodName(), arguments.parameters());
			if (methods.exactSignature != null) {
				exactMatches.put(classInfo, methods.exactSignature);
			}
			matchingNames.putAll(classInfo, methods.matchingName);
		}

		if (exactMatches.isEmpty() && matchingNames.isEmpty()) {
			//no matches found
			return reply("Sorry, I can't find that method. :(", message);
		}

		if (exactMatches.size() == 1) {
			//a single, exact match was found!
			var method = exactMatches.values().iterator().next();
			return printMethod(method, arguments, message);
		}

		if (matchingNames.size() == 1 && arguments.parameters() == null) {
			//user did not specify parameters and there is method with a matching name
			var method = matchingNames.values().iterator().next();
			return printMethod(method, arguments, message);
		}

		//multiple matches were found
		Multimap<ClassInfo, MethodInfo> methodsToPrint;
		if (exactMatches.size() > 1) {
			methodsToPrint = ArrayListMultimap.create();
			for (var entry : exactMatches.entrySet()) {
				methodsToPrint.put(entry.getKey(), entry.getValue());
			}
		} else {
			methodsToPrint = matchingNames;
		}
		return printMethodChoices(methodsToPrint, arguments, message);
	}

	/**
	 * Called when someone types a numeric choice into the chat.
	 * @param message the chat message
	 * @param num the number
	 * @return the chat response
	 */
	private ChatActions showChoice(ChatMessage message, int num) {
		var conversation = conversations.get(message.roomId());

		if (conversation == null) {
			//no choices were ever printed to the chat in this room, so ignore
			return doNothing();
		}

		var age = Duration.between(conversation.timeLastTouched, Instant.now());
		var timedOut = age.compareTo(choiceTimeout) > 0;
		if (timedOut) {
			//it's been a while since the choices were printed to the chat in this room, so ignore
			return doNothing();
		}

		//reset the time-out timer
		conversation.timeLastTouched = Instant.now();

		var index = num - 1;
		if (index < 0 || index >= conversation.choices.size()) {
			//check to make sure the number corresponds to a choice
			return reply("That's not a valid choice.", message);
		}

		//valid choice entered, print the info
		var cb = new ChatBuilder();
		cb.append(conversation.choices.get(index));
		if (conversation.targetUser != null) {
			cb.append(' ').mention(conversation.targetUser);
		}
		var newCommand = new ChatCommand(message, name(), cb.toString());
		return onMessage(newCommand, null);
	}

	/**
	 * Prints the Javadoc info of a particular method.
	 * @param info the method
	 * @param arguments the command arguments
	 * @param message the chat message
	 * @return the chat response
	 */
	private ChatActions printMethod(MethodInfo info, JavadocCommandArguments arguments, ChatCommand message) {
		var cb = new ChatBuilder();

		var username = arguments.targetUser();
		if (username != null) {
			cb.mention(username).append(' ');
		}

		var paragraph = arguments.paragraph();
		if (paragraph == 1) {
			appendPreamble(info, cb);
		}

		appendDescription(info, paragraph, cb);

		var condensedMessage = buildCondensedMessage(info);

		//@formatter:off
		var reply = new PostMessage(cb)
			.splitStrategy(SplitStrategy.WORD)
			.condensedMessage(condensedMessage);
		//@formatter:on

		if (username == null) {
			reply.parentId(message.getMessage().id());
		}

		return create(reply);
	}

	private void appendPreamble(MethodInfo info, ChatBuilder cb) {
		appendLibraryName(info.getClassInfo(), cb);
		appendModifiers(info, cb);
		appendMethodSignature(info, cb);
		cb.append(": ");
	}

	private void appendMethodSignature(MethodInfo info, ChatBuilder cb) {
		var deprecated = info.isDeprecated();
		if (deprecated) cb.strike();

		var signature = info.getSignatureString();
		var url = info.getClassInfo().getUrlWithoutFrames();
		if (url == null) {
			cb.bold().code(signature).bold();
		} else {
			url += "#" + info.getUrlAnchor();
			cb.link(new ChatBuilder().bold().code(signature).bold().toString(), url);
		}

		if (deprecated) cb.strike();
	}

	private void appendDescription(MethodInfo info, int paragraph, ChatBuilder cb) {
		var description = info.getDescription();
		if (description == null || description.isEmpty()) {
			description = new ChatBuilder().italic("no description").toString();
		}
		var since = info.getSince();
		var paragraphs = new Paragraphs(description, since);
		paragraphs.append(paragraph, cb);
	}

	private String buildCondensedMessage(MethodInfo info) {
		var cb = new ChatBuilder();

		if (info.isDeprecated()) cb.strike();
		var signature = info.getSignatureString();
		var url = info.getClassInfo().getUrlWithoutFrames();
		if (url == null) {
			cb.bold().code(signature).bold();
		} else {
			url += "#" + info.getUrlAnchor();
			cb.link(new ChatBuilder().bold().code(signature).bold().toString(), url);
		}
		if (info.isDeprecated()) cb.strike();

		return cb.toString();
	}

	/**
	 * Prints the methods to choose from when multiple methods are found.
	 * @param matchingMethods the methods to choose from
	 * @param arguments the command arguments
	 * @param message the chat message
	 * @return the chat response
	 */
	private ChatActions printMethodChoices(Multimap<ClassInfo, MethodInfo> matchingMethods, JavadocCommandArguments arguments, ChatCommand message) {
		var cb = new ChatBuilder();

		var methodParams = arguments.parameters();
		cb.append(buildChoicesQuestion(matchingMethods, methodParams));

		var count = 1;
		List<String> choices = new ArrayList<>();
		for (var entry : matchingMethods.entries()) {
			var classInfo = entry.getKey();
			var methodInfo = entry.getValue();

			var signature = buildSignature(classInfo, methodInfo);

			cb.nl().append(count).append(". ").append(signature);
			choices.add(signature);
			count++;
		}

		var conversation = new Conversation(choices, arguments.targetUser());
		var roomId = message.getMessage().roomId();
		conversations.put(roomId, conversation);

		//@formatter:off
		return ChatActions.create(
			new PostMessage(cb)
				.splitStrategy(SplitStrategy.NEWLINE)
				.ephemeral(true)
				.parentId(message.getMessage().id())
		);
		//@formatter:on
	}

	private String buildChoicesQuestion(Multimap<ClassInfo, MethodInfo> matchingMethods, List<String> methodParams) {
		var oneMethodFound = (matchingMethods.size() == 1);

		String one;
		if (methodParams == null) {
			one = "";
		} else {
			if (methodParams.isEmpty()) {
				one = "I couldn't find a zero-arg signature for that method. ";
			} else {
				var thatParameter = oneMethodFound ? "that parameter" : "those parameters";
				one = "I couldn't find a signature with " + thatParameter + ". ";
			}
		}

		//@formatter:off
		var two = oneMethodFound ?
			"Did you mean this one? (type the number)" :
			"Did you mean one of these? (type the number)";
		//@formatter:on

		return one + two;
	}

	private String buildSignature(ClassInfo classInfo, MethodInfo methodInfo) {
		var sb = new StringBuilder();
		sb.append(classInfo.getName().getFullyQualifiedName()).append("#").append(methodInfo.getName());

		//@formatter:off
		var paramList = methodInfo.getParameters().stream()
			.map(param -> param.type().getSimpleName() + (param.array() ? "[]" : ""))
		.collect(Collectors.joining(", "));
		//@formatter:on

		sb.append('(').append(paramList).append(')');

		return sb.toString();
	}

	/**
	 * Prints the classes to choose from when multiple class are found.
	 * @param classes the fully-qualified names of the classes
	 * @param arguments the command arguments
	 * @param message the original message
	 * @return the chat response
	 */
	private ChatActions printClassChoices(Collection<String> classes, JavadocCommandArguments arguments, ChatCommand message) {
		var choices = new ArrayList<String>(classes);
		choices.sort(null);

		var cb = new ChatBuilder();
		cb.append("Which one do you mean? (type the number)");

		var count = 1;
		for (var name : choices) {
			cb.nl().append(count).append(". ").append(name);
			count++;
		}

		var conversation = new Conversation(choices, arguments.targetUser());
		var roomId = message.getMessage().roomId();
		conversations.put(roomId, conversation);

		//@formatter:off
		return ChatActions.create(
			new PostMessage(cb)
				.splitStrategy(SplitStrategy.NEWLINE)
				.ephemeral(true)
				.parentId(message.getMessage().id())
		);
		//@formatter:on
	}

	private ChatActions printClass(ClassInfo info, JavadocCommandArguments arguments, ChatCommand message) {
		var cb = new ChatBuilder();

		var username = arguments.targetUser();
		if (username != null) {
			cb.mention(username).append(' ');
		}

		var paragraph = arguments.paragraph();
		if (paragraph == 1) {
			appendPreamble(info, cb);
		}

		appendDescription(info, paragraph, cb);

		//@formatter:off
		var reply = new PostMessage(cb)
			.splitStrategy(SplitStrategy.WORD)
			.condensedMessage(buildCondensedMessage(info));
		//@formatter:on

		if (username == null) {
			reply.parentId(message.getMessage().id());
		}

		return create(reply);
	}

	private void appendPreamble(ClassInfo info, ChatBuilder cb) {
		appendLibraryName(info, cb);
		appendModifiers(info, cb);
		appendClassName(info, cb);
		cb.append(": ");
	}

	private void appendLibraryName(ClassInfo info, ChatBuilder cb) {
		var zipFile = info.getZipFile();
		if (zipFile == null) {
			return;
		}

		var name = zipFile.getName();
		if (name == null || name.equalsIgnoreCase("java")) {
			return;
		}

		name = name.replace(" ", "-");
		cb.bold();
		cb.tag(name);
		cb.bold();
		cb.append(' ');
	}

	private void appendModifiers(ClassInfo info, ChatBuilder cb) {
		var deprecated = info.isDeprecated();
		var infoModifiers = info.getModifiers();
		var modifiersToPrint = new ArrayList<String>(classModifiers);
		modifiersToPrint.retainAll(infoModifiers);

		//add class modifiers
		for (var classModifier : modifiersToPrint) {
			cb.italic();
			if (deprecated) cb.strike();
			cb.tag(classModifier);
			if (deprecated) cb.strike();
			cb.italic();
			cb.append(' ');
		}

		var classType = new HashSet<String>(classTypes);
		classType.retainAll(infoModifiers);
		//there should be only one remaining element in the collection, but use a foreach loop just incase
		for (var modifier : classType) {
			if (deprecated) cb.strike();
			cb.tag(modifier);
			if (deprecated) cb.strike();
			cb.append(' ');
		}
	}

	private void appendModifiers(MethodInfo info, ChatBuilder cb) {
		var deprecated = info.isDeprecated();
		var modifiersToPrint = new ArrayList<String>(info.getModifiers());
		modifiersToPrint.removeAll(methodModifiersToIgnore);

		for (var modifier : modifiersToPrint) {
			if (deprecated) cb.strike();
			cb.tag(modifier);
			if (deprecated) cb.strike();
			cb.append(' ');
		}
	}

	private void appendClassName(ClassInfo info, ChatBuilder cb) {
		var deprecated = info.isDeprecated();

		if (deprecated) cb.strike();
		var fullName = info.getName().getFullyQualifiedName();
		var url = info.getUrlWithFrames();
		if (url == null) {
			cb.bold().code(fullName).bold();
		} else {
			cb.link(new ChatBuilder().bold().code(fullName).bold().toString(), url);
		}
		if (deprecated) cb.strike();
	}

	private void appendDescription(ClassInfo info, int paragraph, ChatBuilder cb) {
		var description = info.getDescription();
		if (description == null || description.isEmpty()) {
			description = new ChatBuilder().italic("no description").toString();
		}
		var since = info.getSince();
		var paragraphs = new Paragraphs(description, since);
		paragraphs.append(paragraph, cb);
	}

	private String buildCondensedMessage(ClassInfo info) {
		var cb = new ChatBuilder();

		if (info.isDeprecated()) cb.strike();
		var fullName = info.getName().getFullyQualifiedName();
		var url = info.getUrlWithFrames();
		if (url == null) {
			cb.bold().code(fullName).bold();
		} else {
			cb.link(new ChatBuilder().bold().code(fullName).bold().toString(), url);
		}
		if (info.isDeprecated()) cb.strike();

		return cb.toString();
	}

	/**
	 * Finds the methods in a given class that matches the given method
	 * signature.
	 * @param info the class to search
	 * @param searchMethodName the name of the method to search for
	 * @param searchMethodParameters the parameters that the method should have,
	 * or null not to look at the parameters.
	 * @return the matching methods
	 * @throws IOException if there's a problem loading Javadoc info from the
	 * DAO
	 */
	private MatchingMethods findMatchingMethods(ClassInfo info, String searchMethodName, List<String> searchMethodParameters) throws IOException {
		var searchResult = new MatchingMethods();
		var matchingNameSignatures = new HashSet<String>();

		/*
		 * Search the class, all of its parent classes, and all of its
		 * interfaces, and the interfaces of its super classes.
		 */
		var stack = new LinkedList<ClassInfo>();
		stack.add(info);

		while (!stack.isEmpty()) {
			var curInfo = stack.removeLast();

			for (var curMethod : curInfo.getMethods()) {
				checkForMatch(curMethod, searchMethodName, searchMethodParameters, matchingNameSignatures, searchResult);
			}

			//add parent class to the stack
			var superClass = curInfo.getSuperClass();
			if (superClass != null) {
				var superClassInfo = dao.getClassInfo(superClass.getFullyQualifiedName());
				if (superClassInfo != null) {
					stack.add(superClassInfo);
				}
			}

			//add interfaces to the stack
			for (var interfaceName : curInfo.getInterfaces()) {
				var interfaceInfo = dao.getClassInfo(interfaceName.getFullyQualifiedName());
				if (interfaceInfo != null) {
					stack.add(interfaceInfo);
				}
			}
		}

		return searchResult;
	}

	private void checkForMatch(MethodInfo info, String searchMethodName, List<String> searchMethodParameters, Set<String> matchingNameSignatures, MatchingMethods searchResult) {
		if (!info.getName().equalsIgnoreCase(searchMethodName)) {
			//name doesn't match
			return;
		}

		var signature = info.getSignature();
		if (matchingNameSignatures.contains(signature)) {
			//this method is already in the matching name list
			return;
		}

		matchingNameSignatures.add(signature);
		searchResult.matchingName.add(info);

		if (searchMethodParameters == null) {
			//user is not searching based on parameters
			return;
		}

		var curParameters = info.getParameters();
		if (curParameters.size() != searchMethodParameters.size()) {
			//parameter size doesn't match
			return;
		}

		//check the parameters
		var exactMatch = true;
		for (var i = 0; i < curParameters.size(); i++) {
			var curParameter = curParameters.get(i);
			var curParameterName = curParameter.type().getSimpleName() + (curParameter.array() ? "[]" : "");

			var methodParameter = searchMethodParameters.get(i);

			if (!curParameterName.equalsIgnoreCase(methodParameter)) {
				//parameter types don't match
				exactMatch = false;
				break;
			}
		}
		if (exactMatch) {
			searchResult.exactSignature = info;
		}
	}

	private static class MatchingMethods {
		private MethodInfo exactSignature;
		private final List<MethodInfo> matchingName = new ArrayList<>();

		public boolean isEmpty() {
			return exactSignature == null && matchingName.isEmpty();
		}
	}

	private static class Paragraphs {
		private final String[] paragraphText;
		private final String since;

		public Paragraphs(String text, String since) {
			paragraphText = text.split("\n\n");
			this.since = since;
		}

		public int count() {
			return paragraphText.length;
		}

		public String get(int num) {
			return paragraphText[num - 1];
		}

		/**
		 * Appends a paragraph to a {@link ChatBuilder}.
		 * @param num the paragraph number
		 * @param cb the chat builder
		 */
		public void append(int num, ChatBuilder cb) {
			if (num > count()) {
				num = count();
			}

			cb.append(get(num));
			if (num == 1 && since != null) {
				cb.append(' ').italic().append("@since ").append(since).italic();
			}
			if (count() > 1) {
				cb.append(" (").append(num).append('/').append(count()).append(')');
			}
		}
	}

	private static class Conversation {
		/**
		 * The list of suggestions that were posted to the chat room.
		 */
		private final List<String> choices;

		/**
		 * The user that the javadoc response should be directed towards or null
		 * if not specified.
		 */
		private final String targetUser;

		/**
		 * The last time the list of choices were accessed in some way.
		 */
		private Instant timeLastTouched = Instant.now();

		/**
		 * @param choices the list of suggestions that were posted to the chat
		 * room
		 * @param targetUser the user that the javadoc response should be
		 * directed towards or null if not specified
		 */
		public Conversation(List<String> choices, String targetUser) {
			this.choices = choices;
			this.targetUser = targetUser;
		}
	}
}
