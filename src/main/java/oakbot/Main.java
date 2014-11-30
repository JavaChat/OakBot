package oakbot;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;

import oakbot.bot.AboutCommand;
import oakbot.bot.Bot;
import oakbot.bot.Command;
import oakbot.bot.HelpCommand;
import oakbot.bot.ShutdownCommand;
import oakbot.javadoc.Java8PageParser;
import oakbot.javadoc.JavadocCommand;
import oakbot.javadoc.JsoupPageParser;
import oakbot.javadoc.PageLoader;
import oakbot.javadoc.PageParser;
import oakbot.javadoc.ZipPageLoader;

/**
 * @author Michael Angstadt
 */
public class Main {
	public static final String VERSION, URL;
	public static final Date BUILT;
	static {
		Properties props = new Properties();
		try (InputStream in = Main.class.getResourceAsStream("/info.properties")) {
			props.load(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		VERSION = props.getProperty("version");
		URL = props.getProperty("url");

		Date built;
		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
			built = df.parse(props.getProperty("built"));
		} catch (ParseException e) {
			//this could happen during development if the properties file is not filtered by Maven
			built = new Date();
		}
		BUILT = built;
	}

	public static void main(String[] args) throws Exception {
		setupLogging();
		BotProperties props = loadProperties();

		List<Command> commands = new ArrayList<>();
		DateFormat builtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");
		commands.add(new AboutCommand("**Oak Bot** v" + VERSION + " by `Michael` | [source code](" + URL + ") | Built: " + builtFormat.format(BUILT)));
		commands.add(new HelpCommand(commands));
		commands.add(createJavadocCommand());
		commands.add(new ShutdownCommand());

		//TODO add max message size limit
		//TODO re-download fkeys after 1 hour of inactivity
		//TODO google command
		//TODO wiki command
		//TODO lib command (displays descriptions of libraries, or you can search for libraries that do certain things)
		//TODO javadocs: class hierarchy
		//TODO javadocs: method docs
		//TODO detect edited commands--and the chat bot should edit *its* reply in response
		//TODO http command
		//@formatter:off
		Bot bot = new Bot.Builder(props.getLoginEmail(), props.getLoginPassword())
		.commands(commands.toArray(new Command[0]))
		.heartbeat(props.getHeartbeat())
		.admins(props.getAdmins().toArray(new Integer[0]))
		.name(props.getBotname())
		.trigger(props.getTrigger())
		.rooms(props.getRooms().toArray(new Integer[0]))
		.build();
		//@formatter:on

		bot.connect();
	}

	private static void setupLogging() throws IOException {
		Path file = Paths.get("logging.properties");
		if (!Files.exists(file)) {
			return;
		}

		try (InputStream in = Files.newInputStream(file)) {
			LogManager.getLogManager().readConfiguration(in);
		}
	}

	private static BotProperties loadProperties() throws IOException {
		Path file = Paths.get("bot.properties");
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
			properties.load(reader);
		}
		return new BotProperties(properties);
	}

	private static Command createJavadocCommand() throws IOException {
		JavadocCommand javadocCommand = new JavadocCommand();

		Path dir = Paths.get("javadocs");
		Path java8Api = dir.resolve("java8.zip");
		if (Files.exists(java8Api)) {
			PageLoader loader = new ZipPageLoader(java8Api);
			PageParser parser = new Java8PageParser();
			javadocCommand.addLibrary(loader, parser);
		} else {
			//for testing purposes
			//this ZIP only has the "java.lang.String" class
			Path sample = dir.resolve("sample.zip");
			if (Files.exists(sample)) {
				PageLoader loader = new ZipPageLoader(sample);
				PageParser parser = new Java8PageParser();
				javadocCommand.addLibrary(loader, parser);
			}
		}

		Path jsoup = dir.resolve("jsoup-1.8.1.jar");
		if (Files.exists(jsoup)) {
			PageLoader loader = new ZipPageLoader(jsoup);
			PageParser parser = new JsoupPageParser();
			javadocCommand.addLibrary(loader, parser);
		}

		return javadocCommand;
	}

	private static Command createHttpCommand() {
		//TODO
		return null;
	}
}
