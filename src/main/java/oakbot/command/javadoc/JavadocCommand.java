package oakbot.command.javadoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;

/**
 * The command class for the chat bot.
 * @author Michael Angstadt
 */
public class JavadocCommand implements Command {
	private final JavadocDao dao;

	private List<String> prevChoices = new ArrayList<>();
	private Date prevChoicesDate;
	private final long choiceTimeout = 1000 * 30;

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
			cb.append("Type the name of a Java class.");
			return new ChatResponse(cb.toString());
		}

		CommandTextParser commandText = new CommandTextParser(content);
		ClassInfo info;
		try {
			info = dao.getClassInfo(commandText.className);
		} catch (MultipleClassesFoundException e) {
			List<String> choices = new ArrayList<>(e.getClasses());
			Collections.sort(choices);
			prevChoices = choices;
			prevChoicesDate = new Date();

			cb.append("Which one do you mean? (type the number)");

			int count = 1;
			for (String name : choices) {
				cb.nl().append((count++) + "").append(". ").append(name);
			}

			return new ChatResponse(cb.toString(), SplitStrategy.NEWLINE);
		} catch (IOException e) {
			throw new RuntimeException("Problem getting Javadoc info.", e);
		}

		if (info == null) {
			cb.append("Sorry, I never heard of that class. :(");
			return new ChatResponse(cb.toString());
		}

		if (commandText.methodName == null) {
			getClassDocs(info, commandText.paragraph, cb);
			return new ChatResponse(cb.toString(), SplitStrategy.WORD);
		}

		try {
			return getMethodDocs(info, commandText.methodName, commandText.parameters, commandText.paragraph, cb);
		} catch (IOException e) {
			throw new RuntimeException("Problem getting Javadoc info.", e);
		}
	}

	public ChatResponse showChoice(ChatMessage message, int num) {
		if (prevChoicesDate == null || System.currentTimeMillis() - prevChoicesDate.getTime() > choiceTimeout) {
			return null;
		}

		prevChoicesDate = new Date();

		int index = num - 1;
		if (index < 0 || index >= prevChoices.size()) {
			return new ChatResponse(new ChatBuilder().reply(message).append("That's not a valid choice.").toString());
		}

		message.setContent(prevChoices.get(index));
		return onMessage(message, false);
	}

	private ChatBuilder getClassDocs(ClassInfo info, int paragraph, ChatBuilder cb) {
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
				boolean italic = false;
				switch (modifier) {
				case "abstract":
				case "final":
					italic = true;
					break;
				case "annotation":
				case "class":
				case "enum":
				case "exception":
				case "interface":
					italic = false;
					break;
				default:
					//ignore all the rest
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
			String url = info.getUrl();
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

		return cb;
	}

	private ChatResponse getMethodDocs(ClassInfo info, String methodName, List<String> methodParams, int paragraph, ChatBuilder cb) throws IOException {
		MethodInfo matchingMethod = null;
		List<MethodInfo> matchingNames = new ArrayList<>();
		Set<String> matchingNameSignatures = new HashSet<>();

		//search the class, all its parent classes, and all its interfaces and the interfaces of its super classes
		LinkedList<ClassInfo> stack = new LinkedList<>();
		stack.add(info);

		while (!stack.isEmpty()) {
			ClassInfo curInfo = stack.removeLast();
			//find method matches
			for (MethodInfo method : curInfo.getMethods()) {
				if (!method.getName().equalsIgnoreCase(methodName)) {
					continue;
				}

				String signature = method.getSignature();
				if (matchingNameSignatures.contains(signature)) {
					continue;
				}

				matchingNameSignatures.add(signature);
				matchingNames.add(method);

				if (methodParams == null || methodParams.size() != method.getParameters().size()) {
					continue;
				}

				boolean match = true;
				for (int i = 0; i < methodParams.size(); i++) {
					String param1 = methodParams.get(i);
					String param2 = method.getParameters().get(i).getType().getSimple();
					param2 += (method.getParameters().get(i).isArray() ? "[]" : "");
					if (!param1.equalsIgnoreCase(param2)) {
						match = false;
						break;
					}
				}
				if (match) {
					matchingMethod = method;
					break;
				}
			}

			if (matchingMethod != null) {
				break;
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

		if (matchingMethod == null) {
			if (matchingNames.isEmpty()) {
				cb.append("I can't find that method anywhere.");
				return new ChatResponse(cb.toString());
			}

			if (matchingNames.size() > 1 || methodParams != null) {
				prevChoices = new ArrayList<>();
				prevChoicesDate = new Date();

				if (methodParams == null) {
					cb.append("Which one do you mean? (type the number)");
				} else {
					if (methodParams.isEmpty()) {
						cb.append("I couldn't find a zero-arg signature for that method.");
					} else {
						cb.append("I couldn't find a signature with ");
						if (methodParams.size() == 1) {
							cb.append("that parameter.");
						} else {
							cb.append("those parameters.");
						}
					}
					cb.append(" Did you mean one of these? (type the number)");
				}
				int count = 1;
				for (MethodInfo matchingName : matchingNames) {
					StringBuilder choicesSb = new StringBuilder();
					choicesSb.append(info.getName().getFull()).append("#").append(matchingName.getName()).append("(");

					cb.nl().append((count++) + "").append(". #").append(matchingName.getName()).append('(');
					boolean first = true;
					for (ParameterInfo param : matchingName.getParameters()) {
						if (first) {
							first = false;
						} else {
							choicesSb.append(",");
							cb.append(", ");
						}
						choicesSb.append(param.getType().getSimple());
						if (param.isArray()) {
							choicesSb.append("[]");
						}

						cb.append(param.getType().getSimple());
						if (param.isArray()) {
							cb.append("[]");
						}
					}
					choicesSb.append(')');
					cb.append(')');
					prevChoices.add(choicesSb.toString());
				}
				return new ChatResponse(cb.toString(), SplitStrategy.NEWLINE);
			}

			matchingMethod = matchingNames.get(0);
		}

		if (paragraph == 1) {
			//print library name
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

			//print modifieres
			boolean deprecated = matchingMethod.isDeprecated();
			for (String modifier : matchingMethod.getModifiers()) {
				if (deprecated) cb.strike();
				cb.tag(modifier);
				if (deprecated) cb.strike();
				cb.append(' ');
			}

			//print signature
			if (deprecated) cb.strike();
			String signature = matchingMethod.getSignatureString();
			String url = info.getUrl();
			if (url == null) {
				cb.bold().code(signature).bold();
			} else {
				cb.link(new ChatBuilder().bold().code(signature).bold().toString(), url);
			}
			if (deprecated) cb.strike();
			cb.append(": ");
		}

		//print the method description
		String description = matchingMethod.getDescription();
		Paragraphs paragraphs = new Paragraphs(description);
		paragraphs.append(paragraph, cb);

		return new ChatResponse(cb.toString(), SplitStrategy.WORD);
	}

	private class Paragraphs {
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

	static class CommandTextParser {
		private final static Pattern messageRegex = Pattern.compile("(.*?)(\\((.*?)\\))?(#(.*?)(\\((.*?)\\))?)?(\\s+(.*?))?$");

		private final String className, methodName;
		private final List<String> parameters;
		private final int paragraph;

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
