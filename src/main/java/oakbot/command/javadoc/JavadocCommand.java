package oakbot.command.javadoc;

import java.io.IOException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.listener.JavadocListener;
import oakbot.util.ChatBuilder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * The command class for the chat bot.
 * @author Michael Angstadt
 */
public class JavadocCommand implements Command {
	private final JavadocDao dao;

	private List<String> prevChoices = new ArrayList<>();
	private long prevChoicesPinged = 0;
	private final long choiceTimeout = TimeUnit.SECONDS.toMillis(30);

	/**
	 * The class modifiers to print in italics when outputting a class to the
	 * chat.
	 */
	private final Set<String> classModifiersItalic;
	{
		ImmutableSet.Builder<String> b = new ImmutableSet.Builder<>();
		b.add("abstract");
		b.add("final");
		classModifiersItalic = b.build();
	}

	/**
	 * The class modifiers to print when outputting a class to the chat.
	 */
	private final Set<String> classModifiers;
	{
		ImmutableSet.Builder<String> b = new ImmutableSet.Builder<>();
		b.add("annotation");
		b.add("class");
		b.add("enum");
		b.add("exception");
		b.add("interface");
		classModifiers = b.build();
	}

	/**
	 * The method modifiers to ignore when outputting a method to the chat.
	 */
	private final Set<String> methodModifiersToIgnore;
	{
		ImmutableSet.Builder<String> b = new ImmutableSet.Builder<>();
		b.add("private");
		b.add("protected");
		b.add("public");
		methodModifiersToIgnore = b.build();
	}

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
	public String description() {
		return "Displays class documentation from the Javadocs.";
	}

	@Override
	public String helpText() {
		return description(); //TODO finish
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		ChatBuilder cb = new ChatBuilder();
		cb.reply(message);

		String content = message.getContent();
		if (content.isEmpty()) {
			cb.append("Type the name of a Java class (e.g. \"java.lang.String\") or a method (e.g. \"Integer#parseInt\").");
			return new ChatResponse(cb.toString());
		}

		//parse the command
		CommandTextParser commandText = new CommandTextParser(content);

		ClassInfo info;
		try {
			info = dao.getClassInfo(commandText.className);
		} catch (MultipleClassesFoundException e) {
			if (commandText.methodName == null) {
				return printClassChoices(e.getClasses(), cb);
			}

			//search each class for a method that matches the given signature
			Map<ClassInfo, MethodInfo> exactMatches = new HashMap<>();
			Multimap<ClassInfo, MethodInfo> matchingNames = ArrayListMultimap.create();
			for (String className : e.getClasses()) {
				try {
					ClassInfo classInfo = dao.getClassInfo(className);
					MatchingMethods methods = getMatchingMethods(classInfo, commandText.methodName, commandText.parameters);
					if (methods.exactSignature != null) {
						exactMatches.put(classInfo, methods.exactSignature);
					}
					matchingNames.putAll(classInfo, methods.matchingName);
				} catch (IOException e2) {
					throw new RuntimeException("Problem getting Javadoc info.", e2);
				}
			}

			if (exactMatches.isEmpty() && matchingNames.isEmpty()) {
				//no matches found
				cb.append("Sorry, I can't find that method. :(");
				return new ChatResponse(cb.toString());
			}

			if (exactMatches.size() == 1) {
				//a single, exact match was found!
				MethodInfo method = exactMatches.values().iterator().next();
				ClassInfo classInfo = exactMatches.keySet().iterator().next();
				return printMethod(method, classInfo, commandText.paragraph, cb);
			}

			//multiple matches were found
			Multimap<ClassInfo, MethodInfo> map;
			if (exactMatches.size() > 1) {
				map = ArrayListMultimap.create();
				for (Map.Entry<ClassInfo, MethodInfo> entry : exactMatches.entrySet()) {
					map.put(entry.getKey(), entry.getValue());
				}
			} else {
				map = matchingNames;
			}
			return printMethodChoices(map, commandText.parameters, cb);
		} catch (IOException e) {
			throw new RuntimeException("Problem getting Javadoc info.", e);
		}

		if (info == null) {
			//couldn't find the class
			cb.append("Sorry, I never heard of that class. :(");
			return new ChatResponse(cb.toString());
		}

		if (commandText.methodName == null) {
			//method name not specified, so print the class docs
			return printClass(info, commandText.paragraph, cb);
		}

		//print the method docs
		MatchingMethods matchingMethods;
		try {
			matchingMethods = getMatchingMethods(info, commandText.methodName, commandText.parameters);
		} catch (IOException e) {
			throw new RuntimeException("Problem getting Javadoc info.", e);
		}

		if (matchingMethods.isEmpty()) {
			//no matches found
			cb.append("Sorry, I can't find that method. :(");
			return new ChatResponse(cb.toString());
		}

		if (matchingMethods.exactSignature != null) {
			//an exact match was found!
			return printMethod(matchingMethods.exactSignature, info, commandText.paragraph, cb);
		}

		if (matchingMethods.matchingName.size() == 1 && commandText.parameters == null) {
			return printMethod(matchingMethods.matchingName.get(0), info, commandText.paragraph, cb);
		}

		//print the methods with the same name
		Multimap<ClassInfo, MethodInfo> map = ArrayListMultimap.create();
		map.putAll(info, matchingMethods.matchingName);
		return printMethodChoices(map, commandText.parameters, cb);
	}

	/**
	 * Called when the {@link JavadocListener} picks up a choice someone typed
	 * into the chat.
	 * @param message the chat message
	 * @param num the number
	 * @return the chat response or null not to respond to the message
	 */
	public ChatResponse showChoice(ChatMessage message, int num) {
		if (prevChoicesPinged == 0) {
			return null;
		}

		boolean timedOut = System.currentTimeMillis() - prevChoicesPinged > choiceTimeout;
		if (timedOut) {
			return null;
		}

		//reset the time-out timer
		prevChoicesPinged = System.currentTimeMillis();

		int index = num - 1;
		if (index < 0 || index >= prevChoices.size()) {
			return new ChatResponse(new ChatBuilder().reply(message).append("That's not a valid choice.").toString());
		}

		message.setContent(prevChoices.get(index));
		return onMessage(message, false);
	}

	/**
	 * Prints the Javadoc info of a particular method.
	 * @param methodinfo the method
	 * @param classInfo the class that the method belongs to
	 * @param paragraph the paragraph to print
	 * @param cb the chat builder
	 * @return the chat response
	 */
	private ChatResponse printMethod(MethodInfo methodInfo, ClassInfo classInfo, int paragraph, ChatBuilder cb) {
		if (paragraph == 1) {
			//print library name
			LibraryZipFile zipFile = classInfo.getZipFile();
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
			for (String modifier : methodInfo.getModifiers()) {
				if (methodModifiersToIgnore.contains(modifier)) {
					continue;
				}

				if (deprecated) cb.strike();
				cb.tag(modifier);
				if (deprecated) cb.strike();
				cb.append(' ');
			}

			//print signature
			if (deprecated) cb.strike();
			String signature = methodInfo.getSignatureString();
			String url = classInfo.getUrl();
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
		Paragraphs paragraphs = new Paragraphs(description);
		paragraphs.append(paragraph, cb);

		return new ChatResponse(cb.toString(), SplitStrategy.WORD);
	}

	/**
	 * Prints the methods to choose from when multiple methods are found.
	 * @param matchingMethods the methods to choose from
	 * @param methodParams the parameters of the method or null if no parameters
	 * were specified
	 * @param cb the chat builder
	 * @return the chat response
	 */
	private ChatResponse printMethodChoices(Multimap<ClassInfo, MethodInfo> matchingMethods, List<String> methodParams, ChatBuilder cb) {
		prevChoices = new ArrayList<>();
		prevChoicesPinged = System.currentTimeMillis();

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
		for (Map.Entry<ClassInfo, MethodInfo> entry : matchingMethods.entries()) {
			ClassInfo classInfo = entry.getKey();
			MethodInfo methodInfo = entry.getValue();

			String signature;
			{
				StringBuilder sb = new StringBuilder();
				sb.append(classInfo.getName().getFull()).append("#").append(methodInfo.getName());

				List<String> paramList = new ArrayList<>();
				for (ParameterInfo param : methodInfo.getParameters()) {
					paramList.add(param.getType().getSimple() + (param.isArray() ? "[]" : ""));
				}
				sb.append('(').append(String.join(", ", paramList)).append(')');

				signature = sb.toString();
			}

			cb.nl().append(count + "").append(". ").append(signature);
			prevChoices.add(signature);
			count++;
		}
		return new ChatResponse(cb.toString(), SplitStrategy.NEWLINE);
	}

	/**
	 * Prints the classes to choose from when multiple class are found.
	 * @param classes the fully-qualified names of the classes
	 * @param cb the chat builder
	 * @return the chat response
	 */
	private ChatResponse printClassChoices(Collection<String> classes, ChatBuilder cb) {
		List<String> choices = new ArrayList<>(classes);
		Collections.sort(choices);
		prevChoices = choices;
		prevChoicesPinged = System.currentTimeMillis();

		cb.append("Which one do you mean? (type the number)");

		int count = 1;
		for (String name : choices) {
			cb.nl().append(count + "").append(". ").append(name);
			count++;
		}

		return new ChatResponse(cb.toString(), SplitStrategy.NEWLINE);
	}

	private ChatResponse printClass(ClassInfo info, int paragraph, ChatBuilder cb) {
		if (paragraph == 1) {
			//print the library name
			LibraryZipFile zipFile = info.getZipFile();
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
			for (String modifier : info.getModifiers()) {
				boolean italic = classModifiersItalic.contains(modifier);
				if (!italic && !classModifiers.contains(modifier)) {
					continue;
				}

				if (italic) cb.italic();
				if (deprecated) cb.strike();
				cb.tag(modifier);
				if (deprecated) cb.strike();
				if (italic) cb.italic();
				cb.append(' ');
			}

			//print class name
			if (deprecated) cb.strike();
			String fullName = info.getName().getFull();
			String url = info.getFrameUrl();
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
		Paragraphs paragraphs = new Paragraphs(description);
		paragraphs.append(paragraph, cb);

		return new ChatResponse(cb.toString(), SplitStrategy.WORD);
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
					String curParameterName = curParameter.getType().getSimple() + (curParameter.isArray() ? "[]" : "");

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
				ClassInfo superClassInfo = dao.getClassInfo(superClass.getFull());
				if (superClassInfo != null) {
					stack.add(superClassInfo);
				}
			}

			//add interfaces to the stack
			for (ClassName interfaceName : curInfo.getInterfaces()) {
				ClassInfo interfaceInfo = dao.getClassInfo(interfaceName.getFull());
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

		public Paragraphs(String text) {
			paragraphs = text.split("\n\n");
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
			if (count() > 1) {
				cb.append(" (").append(num + "").append("/").append(count() + "").append(")");
			}
		}
	}

	/**
	 * Parses a "=javadoc" chat command.
	 */
	static class CommandTextParser {
		private final static Pattern messageRegex = Pattern.compile("(.*?)(\\((.*?)\\))?(#(.*?)(\\((.*?)\\))?)?(\\s+(.*?))?$");

		private final String className, methodName;
		private final List<String> parameters;
		private final int paragraph;

		/**
		 * @param message the command text
		 */
		public CommandTextParser(String message) {
			Matcher m = messageRegex.matcher(message);
			m.find();

			className = m.group(1);

			if (m.group(2) != null) { //e.g. java.lang.string(string, string)
				int dot = className.lastIndexOf('.');
				String simpleName = (dot < 0) ? className : className.substring(dot + 1);
				methodName = simpleName;
			} else {
				methodName = m.group(5); //e.g. java.lang.string#indexOf(int)
			}

			String parametersStr = m.group(4); //e.g. java.lang.string(string, string)
			if (parametersStr == null || parametersStr.startsWith("#")) {
				parametersStr = m.group(7); //e.g. java.lang.string#string(string, string)
				if (parametersStr == null) {
					parametersStr = m.group(3);
				}
			}
			if (parametersStr == null || parametersStr.equals("*")) {
				parameters = null;
			} else if (parametersStr.isEmpty()) {
				parameters = Collections.emptyList();
			} else {
				parameters = Arrays.asList(parametersStr.split("\\s*,\\s*"));
			}

			int paragraph;
			try {
				paragraph = Integer.parseInt(m.group(9));
				if (paragraph < 1) {
					paragraph = 1;
				}
			} catch (NumberFormatException e) {
				paragraph = 1;
			}
			this.paragraph = paragraph;
		}

		public String getClassName() {
			return className;
		}

		public String getMethodName() {
			return methodName;
		}

		public List<String> getParameters() {
			return parameters;
		}

		public int getParagraph() {
			return paragraph;
		}
	}
}
