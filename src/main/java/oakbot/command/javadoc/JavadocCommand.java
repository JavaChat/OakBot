package oakbot.command.javadoc;

import static oakbot.command.Command.reply;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.listener.JavadocListener;
import oakbot.util.ChatBuilder;

/**
 * The command class for the chat bot.
 * @author Michael Angstadt
 */
public class JavadocCommand implements Command {
	/**
	 * Stop responding to numeric choices the user enters after this amount of
	 * time.
	 */
	private static final long choiceTimeout = TimeUnit.SECONDS.toMillis(30);

	/**
	 * "Flags" that a class can have. They are defined in a List because, if a
	 * class has multiple modifiers, I want them to be displayed in a consistent
	 * order.
	 */
	private static final List<String> classModifiers;
	static {
		ImmutableList.Builder<String> b = new ImmutableList.Builder<>();
		b.add("abstract", "final");
		classModifiers = b.build();
	}

	/**
	 * The list of possible "class types". Each class *should* have exactly one
	 * type, but there's no explicit check for this (things shouldn't break if a
	 * class does not have exactly one).
	 */
	private static final Set<String> classTypes;
	static {
		ImmutableSet.Builder<String> b = new ImmutableSet.Builder<>();
		b.add("annotation", "class", "enum", "exception", "interface");
		classTypes = b.build();
	}

	/**
	 * The method modifiers to ignore when outputting a method to the chat.
	 */
	private static final Set<String> methodModifiersToIgnore;
	static {
		ImmutableSet.Builder<String> b = new ImmutableSet.Builder<>();
		b.add("private", "protected", "public");
		methodModifiersToIgnore = b.build();
	}

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
		return Arrays.asList("javadocs");
	}

	@Override
	public String description() {
		return "Displays class documentation from the Javadocs.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append("Displays class documentation from the Javadocs.  ")
			.append("If more than one class or method matches the query, then a list of choices is displayed.  Queries are case-insensitive.").nl()
			.append("Usage: ").append(trigger).append(name()).append(" CLASS_NAME[#METHOD_NAME[(METHOD_PARAMS)]] [PARAGRAPH_NUM] [[@]TARGET_USER]").nl()
			.append("Examples:").nl()
			.append(trigger).append(name()).append(" String").nl()
			.append(trigger).append(name()).append(" java.lang.String#indexOf").nl()
			.append(trigger).append(name()).append(" java.lang.String#indexOf 2").nl()
			.append(trigger).append(name()).append(" java.lang.String#indexOf NoobUser").nl()
			.append(trigger).append(name()).append(" java.lang.String#substring(int)").nl()
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String content = chatCommand.getContent();
		if (content.isEmpty()) {
			return reply("Type the name of a Java class (e.g. \"java.lang.String\") or a method (e.g. \"Integer#parseInt\").", chatCommand);
		}

		//parse the command arguments
		JavadocCommandArguments arguments = JavadocCommandArguments.parse(content);

		//search for the class
		Collection<String> fullyQualifiedNames;
		try {
			fullyQualifiedNames = dao.search(arguments.getClassName());
		} catch (IOException e) {
			throw new RuntimeException("Problem searching for fully-qualified name.", e);
		}

		if (fullyQualifiedNames.isEmpty()) {
			return handleNoMatch(chatCommand);
		}

		if (fullyQualifiedNames.size() > 1) {
			return handleMultipleMatches(arguments, fullyQualifiedNames, chatCommand);
		}

		String fullyQualifiedName = fullyQualifiedNames.iterator().next();
		ClassInfo info;
		try {
			info = dao.getClassInfo(fullyQualifiedName);
		} catch (IOException e) {
			throw new RuntimeException("Problem getting Javadoc info.", e);
		}

		if (info == null) {
			//this should never happen, since we got the fully-qualified name from "dao.search()"
			return handleNoMatch(chatCommand);
		}
		return handleSingleMatch(arguments, info, chatCommand);
	}

	private ChatResponse handleNoMatch(ChatCommand message) {
		return reply("Sorry, I never heard of that class. :(", message);
	}

	private ChatResponse handleSingleMatch(JavadocCommandArguments arguments, ClassInfo info, ChatCommand message) {
		if (arguments.getMethodName() == null) {
			//method name not specified, so print the class docs
			return printClass(info, arguments, message);
		}

		//print the method docs
		MatchingMethods matchingMethods;
		try {
			matchingMethods = getMatchingMethods(info, arguments.getMethodName(), arguments.getParameters());
		} catch (IOException e) {
			throw new RuntimeException("Problem getting Javadoc info.", e);
		}

		if (matchingMethods.isEmpty()) {
			//no matches found
			return reply("Sorry, I can't find that method. :(", message);
		}

		if (matchingMethods.exactSignature != null) {
			//an exact match was found!
			return printMethod(matchingMethods.exactSignature, info, arguments, message);
		}

		if (matchingMethods.matchingName.size() == 1 && arguments.getParameters() == null) {
			return printMethod(matchingMethods.matchingName.get(0), info, arguments, message);
		}

		//print the methods with the same name
		Multimap<ClassInfo, MethodInfo> map = ArrayListMultimap.create();
		map.putAll(info, matchingMethods.matchingName);
		return printMethodChoices(map, arguments, message);
	}

	private ChatResponse handleMultipleMatches(JavadocCommandArguments arguments, Collection<String> matches, ChatCommand message) {
		if (arguments.getMethodName() == null) {
			//user is just querying for a class, so print the class choices
			return printClassChoices(matches, arguments, message);
		}

		//user is querying for a method

		//search each class for a method that matches the given signature
		Map<ClassInfo, MethodInfo> exactMatches = new HashMap<>();
		Multimap<ClassInfo, MethodInfo> matchingNames = ArrayListMultimap.create();
		for (String className : matches) {
			try {
				ClassInfo classInfo = dao.getClassInfo(className);
				MatchingMethods methods = getMatchingMethods(classInfo, arguments.getMethodName(), arguments.getParameters());
				if (methods.exactSignature != null) {
					exactMatches.put(classInfo, methods.exactSignature);
				}
				matchingNames.putAll(classInfo, methods.matchingName);
			} catch (IOException e) {
				throw new RuntimeException("Problem getting Javadoc info.", e);
			}
		}

		if (exactMatches.isEmpty() && matchingNames.isEmpty()) {
			//no matches found
			return reply("Sorry, I can't find that method. :(", message);
		}

		if (exactMatches.size() == 1) {
			//a single, exact match was found!
			MethodInfo method = exactMatches.values().iterator().next();
			ClassInfo classInfo = exactMatches.keySet().iterator().next();
			return printMethod(method, classInfo, arguments, message);
		}

		if (matchingNames.size() == 1 && arguments.getParameters() == null) {
			//user did not specify parameters and there is method with a matching name
			MethodInfo method = matchingNames.values().iterator().next();
			ClassInfo classInfo = matchingNames.keySet().iterator().next();
			return printMethod(method, classInfo, arguments, message);
		}

		//multiple matches were found
		Multimap<ClassInfo, MethodInfo> methodsToPrint;
		if (exactMatches.size() > 1) {
			methodsToPrint = ArrayListMultimap.create();
			for (Map.Entry<ClassInfo, MethodInfo> entry : exactMatches.entrySet()) {
				methodsToPrint.put(entry.getKey(), entry.getValue());
			}
		} else {
			methodsToPrint = matchingNames;
		}
		return printMethodChoices(methodsToPrint, arguments, message);
	}

	/**
	 * Called when the {@link JavadocListener} picks up a choice someone typed
	 * into the chat.
	 * @param message the chat message
	 * @param num the number
	 * @return the chat response or null not to respond to the message
	 */
	public ChatResponse showChoice(ChatMessage message, int num) {
		Conversation conversation = conversations.get(message.getRoomId());

		if (conversation == null) {
			//no choices were ever printed to the chat in this room, so ignore
			return null;
		}

		boolean timedOut = ChronoUnit.MILLIS.between(conversation.timeLastTouched, Instant.now()) > choiceTimeout;
		if (timedOut) {
			//it's been a while since the choices were printed to the chat in this room, so ignore
			return null;
		}

		//reset the time-out timer
		conversation.timeLastTouched = Instant.now();

		int index = num - 1;
		if (index < 0 || index >= conversation.choices.size()) {
			//check to make sure the number corresponds to a choice
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("That's not a valid choice.")
			);
			//@formatter:on
		}

		//valid choice entered, print the info
		ChatBuilder cb = new ChatBuilder();
		cb.append(conversation.choices.get(index));
		if (conversation.targetUser != null) {
			cb.append(' ').mention(conversation.targetUser);
		}
		ChatCommand newCommand = new ChatCommand(message, name(), cb.toString());
		return onMessage(newCommand, null);
	}

	/**
	 * Prints the Javadoc info of a particular method.
	 * @param methodinfo the method
	 * @param classInfo the class that the method belongs to
	 * @param arguments the command arguments
	 * @param cb the chat builder
	 * @return the chat response
	 */
	private ChatResponse printMethod(MethodInfo methodInfo, ClassInfo classInfo, JavadocCommandArguments arguments, ChatCommand message) {
		ChatBuilder cb = new ChatBuilder();

		String username = arguments.getTargetUser();
		if (username == null) {
			cb.reply(message);
		} else {
			cb.mention(username).append(' ');
		}

		int paragraph = arguments.getParagraph();
		if (paragraph == 1) {
			//print library name
			JavadocZipFile zipFile = classInfo.getZipFile();
			if (zipFile != null) {
				String name = zipFile.getName();
				if (name != null && !name.equalsIgnoreCase("java")) {
					name = name.replace(' ', '-');
					cb.bold();
					cb.tag(name);
					cb.bold();
					cb.append(' ');
				}
			}

			//print modifiers
			boolean deprecated = methodInfo.isDeprecated();
			Collection<String> modifiersToPrint = new ArrayList<String>(methodInfo.getModifiers());
			modifiersToPrint.removeAll(methodModifiersToIgnore);
			for (String modifier : modifiersToPrint) {
				if (deprecated) cb.strike();
				cb.tag(modifier);
				if (deprecated) cb.strike();
				cb.append(' ');
			}

			//print signature
			if (deprecated) cb.strike();
			String signature = methodInfo.getSignatureString();
			String url = classInfo.getUrl(false);
			if (url == null) {
				cb.bold().code(signature).bold();
			} else {
				url += "#" + methodInfo.getUrlAnchor();
				cb.link(new ChatBuilder().bold().code(signature).bold().toString(), url);
			}
			if (deprecated) cb.strike();
			cb.append(": ");
		}

		//print the method description
		String description = methodInfo.getDescription();
		if (description == null || description.isEmpty()) {
			description = new ChatBuilder().italic("no description").toString();
		}
		String since = methodInfo.getSince();
		Paragraphs paragraphs = new Paragraphs(description, since);
		paragraphs.append(paragraph, cb);

		return new ChatResponse(cb, SplitStrategy.WORD, false, buildHideMessage(classInfo, methodInfo));
	}

	private String buildHideMessage(ClassInfo classInfo, MethodInfo methodInfo) {
		ChatBuilder sig = new ChatBuilder();

		if (methodInfo.isDeprecated()) sig.strike();
		String signature = methodInfo.getSignatureString();
		String url = classInfo.getUrl(false);
		if (url == null) {
			sig.bold().code(signature).bold();
		} else {
			url += "#" + methodInfo.getUrlAnchor();
			sig.link(new ChatBuilder().bold().code(signature).bold().toString(), url);
		}
		if (methodInfo.isDeprecated()) sig.strike();

		return sig.toString();
	}

	/**
	 * Prints the methods to choose from when multiple methods are found.
	 * @param matchingMethods the methods to choose from
	 * @param arguments the command arguments
	 * @param cb the chat builder
	 * @return the chat response
	 */
	private ChatResponse printMethodChoices(Multimap<ClassInfo, MethodInfo> matchingMethods, JavadocCommandArguments arguments, ChatCommand message) {
		ChatBuilder cb = new ChatBuilder();
		cb.reply(message);

		List<String> methodParams = arguments.getParameters();

		if (matchingMethods.size() == 1) {
			if (methodParams == null) {
				cb.append("Did you mean this one? (type the number)");
			} else {
				if (methodParams.isEmpty()) {
					cb.append("I couldn't find a zero-arg signature for that method.");
				} else {
					cb.append("I couldn't find a signature with ");
					cb.append((methodParams.size() == 1) ? "that parameter." : "those parameters.");
				}
				cb.append(" Did you mean this one? (type the number)");
			}
		} else {
			if (methodParams == null) {
				cb.append("Which one do you mean? (type the number)");
			} else {
				if (methodParams.isEmpty()) {
					cb.append("I couldn't find a zero-arg signature for that method.");
				} else {
					cb.append("I couldn't find a signature with ");
					cb.append((methodParams.size() == 1) ? "that parameter." : "those parameters.");
				}
				cb.append(" Did you mean one of these? (type the number)");
			}
		}

		int count = 1;
		List<String> choices = new ArrayList<>();
		for (Map.Entry<ClassInfo, MethodInfo> entry : matchingMethods.entries()) {
			ClassInfo classInfo = entry.getKey();
			MethodInfo methodInfo = entry.getValue();

			String signature;
			{
				StringBuilder sb = new StringBuilder();
				sb.append(classInfo.getName().getFullyQualifiedName()).append("#").append(methodInfo.getName());

				List<String> paramList = new ArrayList<>();
				for (ParameterInfo param : methodInfo.getParameters()) {
					paramList.add(param.getType().getSimpleName() + (param.isArray() ? "[]" : ""));
				}
				sb.append('(').append(String.join(", ", paramList)).append(')');

				signature = sb.toString();
			}

			cb.nl().append(count).append(". ").append(signature);
			choices.add(signature);
			count++;
		}

		Conversation conversation = new Conversation(choices, arguments.getTargetUser());
		int roomId = message.getMessage().getRoomId();
		conversations.put(roomId, conversation);

		return new ChatResponse(cb, SplitStrategy.NEWLINE);
	}

	/**
	 * Prints the classes to choose from when multiple class are found.
	 * @param classes the fully-qualified names of the classes
	 * @param arguments the command arguments
	 * @param message the original message
	 * @return the chat response
	 */
	private ChatResponse printClassChoices(Collection<String> classes, JavadocCommandArguments arguments, ChatCommand message) {
		List<String> choices = new ArrayList<>(classes);
		Collections.sort(choices);

		ChatBuilder cb = new ChatBuilder();
		cb.reply(message);
		cb.append("Which one do you mean? (type the number)");

		int count = 1;
		for (String name : choices) {
			cb.nl().append(count).append(". ").append(name);
			count++;
		}

		Conversation conversation = new Conversation(choices, arguments.getTargetUser());
		int roomId = message.getMessage().getRoomId();
		conversations.put(roomId, conversation);

		return new ChatResponse(cb, SplitStrategy.NEWLINE);
	}

	private ChatResponse printClass(ClassInfo info, JavadocCommandArguments arguments, ChatCommand message) {
		ChatBuilder cb = new ChatBuilder();

		String username = arguments.getTargetUser();
		if (username == null) {
			cb.reply(message);
		} else {
			cb.mention(username).append(' ');
		}

		int paragraph = arguments.getParagraph();
		if (paragraph == 1) {
			//print the library name
			JavadocZipFile zipFile = info.getZipFile();
			if (zipFile != null) {
				String name = zipFile.getName();
				if (name != null && !name.equalsIgnoreCase("Java")) {
					name = name.replace(" ", "-");
					cb.bold();
					cb.tag(name);
					cb.bold();
					cb.append(' ');
				}
			}

			//print modifiers
			boolean deprecated = info.isDeprecated();
			Collection<String> infoModifiers = info.getModifiers();
			List<String> modifiersToPrint = new ArrayList<>(classModifiers);
			modifiersToPrint.retainAll(infoModifiers);

			//add class modifiers
			for (String classModifier : modifiersToPrint) {
				cb.italic();
				if (deprecated) cb.strike();
				cb.tag(classModifier);
				if (deprecated) cb.strike();
				cb.italic();
				cb.append(' ');
			}

			Collection<String> classType = new HashSet<>(classTypes);
			classType.retainAll(infoModifiers);
			//there should be only one remaining element in the collection, but use a foreach loop just incase
			for (String modifier : classType) {
				if (deprecated) cb.strike();
				cb.tag(modifier);
				if (deprecated) cb.strike();
				cb.append(' ');
			}

			//print class name
			if (deprecated) cb.strike();
			String fullName = info.getName().getFullyQualifiedName();
			String url = info.getUrl(true);
			if (url == null) {
				cb.bold().code(fullName).bold();
			} else {
				cb.link(new ChatBuilder().bold().code(fullName).bold().toString(), url);
			}
			if (deprecated) cb.strike();
			cb.append(": ");
		}

		//print the class description
		String description = info.getDescription();
		if (description == null || description.isEmpty()) {
			description = new ChatBuilder().italic("no description").toString();
		}
		String since = info.getSince();
		Paragraphs paragraphs = new Paragraphs(description, since);
		paragraphs.append(paragraph, cb);
		return new ChatResponse(cb, SplitStrategy.WORD, false, buildHideMessage(info));
	}

	private String buildHideMessage(ClassInfo info) {
		ChatBuilder cb = new ChatBuilder();

		if (info.isDeprecated()) cb.strike();
		String fullName = info.getName().getFullyQualifiedName();
		String url = info.getUrl(true);
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
	 * @param methodName the name of the method to search for
	 * @param methodParameters the parameters that the method should have, or
	 * null not to look at the parameters.
	 * @return the matching methods
	 * @throws IOException if there's a problem loading Javadoc info from the
	 * DAO
	 */
	private MatchingMethods getMatchingMethods(ClassInfo info, String methodName, List<String> methodParameters) throws IOException {
		MatchingMethods matchingMethods = new MatchingMethods();
		Set<String> matchingNameSignatures = new HashSet<>();

		//search the class, all its parent classes, and all its interfaces and the interfaces of its super classes
		LinkedList<ClassInfo> stack = new LinkedList<>();
		stack.add(info);

		while (!stack.isEmpty()) {
			ClassInfo curInfo = stack.removeLast();
			for (MethodInfo curMethod : curInfo.getMethods()) {
				if (!curMethod.getName().equalsIgnoreCase(methodName)) {
					//name doesn't match
					continue;
				}

				String signature = curMethod.getSignature();
				if (matchingNameSignatures.contains(signature)) {
					//this method is already in the matching name list
					continue;
				}

				matchingNameSignatures.add(signature);
				matchingMethods.matchingName.add(curMethod);

				if (methodParameters == null) {
					//user is not searching based on parameters
					continue;
				}

				List<ParameterInfo> curParameters = curMethod.getParameters();
				if (curParameters.size() != methodParameters.size()) {
					//parameter size doesn't match
					continue;
				}

				//check the parameters
				boolean exactMatch = true;
				for (int i = 0; i < curParameters.size(); i++) {
					ParameterInfo curParameter = curParameters.get(i);
					String curParameterName = curParameter.getType().getSimpleName() + (curParameter.isArray() ? "[]" : "");

					String methodParameter = methodParameters.get(i);

					if (!curParameterName.equalsIgnoreCase(methodParameter)) {
						//parameter types don't match
						exactMatch = false;
						break;
					}
				}
				if (exactMatch) {
					matchingMethods.exactSignature = curMethod;
				}
			}

			//add parent class to the stack
			ClassName superClass = curInfo.getSuperClass();
			if (superClass != null) {
				ClassInfo superClassInfo = dao.getClassInfo(superClass.getFullyQualifiedName());
				if (superClassInfo != null) {
					stack.add(superClassInfo);
				}
			}

			//add interfaces to the stack
			for (ClassName interfaceName : curInfo.getInterfaces()) {
				ClassInfo interfaceInfo = dao.getClassInfo(interfaceName.getFullyQualifiedName());
				if (interfaceInfo != null) {
					stack.add(interfaceInfo);
				}
			}
		}

		return matchingMethods;
	}

	private static class MatchingMethods {
		private MethodInfo exactSignature;
		private final List<MethodInfo> matchingName = new ArrayList<>();

		public boolean isEmpty() {
			return exactSignature == null && matchingName.isEmpty();
		}
	}

	private static class Paragraphs {
		private final String paragraphs[];
		private final String since;

		public Paragraphs(String text, String since) {
			paragraphs = text.split("\n\n");
			this.since = since;
		}

		public int count() {
			return paragraphs.length;
		}

		public String get(int num) {
			return paragraphs[num - 1];
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

	private class Conversation {
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
