package oakbot;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.http.impl.client.HttpClientBuilder;

import oakbot.bot.Bot;
import oakbot.chat.ChatConnection;
import oakbot.chat.StackoverflowChat;
import oakbot.command.AboutCommand;
import oakbot.command.Command;
import oakbot.command.EightBallCommand;
import oakbot.command.HelpCommand;
import oakbot.command.RollCommand;
import oakbot.command.ShutdownCommand;
import oakbot.command.SummonCommand;
import oakbot.command.TagCommand;
import oakbot.command.WikiCommand;
import oakbot.command.define.DefineCommand;
import oakbot.command.http.HttpCommand;
import oakbot.command.javadoc.JavadocCommand;
import oakbot.command.javadoc.JavadocDao;
import oakbot.command.urban.UrbanCommand;
import oakbot.listener.JavadocListener;
import oakbot.listener.Listener;
import oakbot.listener.MentionListener;

/**
 * @author Michael Angstadt
 */
public class Main {
	private static final Logger logger = Logger.getLogger(Main.class.getName());

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
		boolean quiet = args.length > 0 && args[0].equals("-q");

		setupLogging();
		BotProperties props = loadProperties();

		JavadocCommand javadocCommand = createJavadocCommand(props.getJavadocPath());

		//@formatter:off
		List<Listener> listeners = Arrays.asList(
			new MentionListener(props.getBotname(), props.getTrigger()),
			new JavadocListener(javadocCommand)
		);
		//@formatter:on

		Statistics stats = new Statistics(Paths.get("statistics.properties"));

		List<Command> commands = new ArrayList<>();
		commands.add(new AboutCommand(stats));
		commands.add(new HelpCommand(commands, listeners, props.getTrigger()));
		commands.add(javadocCommand);
		commands.add(new HttpCommand());
		commands.add(new WikiCommand());
		commands.add(new TagCommand());
		commands.add(new UrbanCommand());
		String dictionaryKey = props.getDictionaryKey();
		if (dictionaryKey != null) {
			commands.add(new DefineCommand(dictionaryKey));
		}
		commands.add(new RollCommand());
		commands.add(new EightBallCommand());
		commands.add(new SummonCommand());
		commands.add(new ShutdownCommand());

		ChatConnection connection = new StackoverflowChat(HttpClientBuilder.create().build());

		//@formatter:off
		Bot bot = new Bot.Builder()
		.login(props.getLoginEmail(), props.getLoginPassword())
		.commands(commands)
		.listeners(listeners)
		.connection(connection)
		.heartbeat(props.getHeartbeat())
		.admins(props.getAdmins())
		.name(props.getBotname())
		.trigger(props.getTrigger())
		.rooms(props.getRooms())
		.stats(stats)
		.build();
		//@formatter:on

		bot.connect(quiet);

		logger.info("Terminating.");
	}

	private static void setupLogging() throws IOException {
		Path file = Paths.get("logging.properties");
		if (!Files.exists(file)) {
			return;
		}

		try (InputStream in = Files.newInputStream(file)) {
			LogManager.getLogManager().readConfiguration(in);
		}

		//log uncaught exceptions
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable thrown) {
				logger.log(Level.SEVERE, "Uncaught exception thrown.", thrown);
			}
		});
	}

	private static BotProperties loadProperties() throws IOException {
		Path file = Paths.get("bot.properties");
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
			properties.load(reader);
		}
		return new BotProperties(properties);
	}

	private static JavadocCommand createJavadocCommand(Path dir) throws IOException {
		JavadocDao dao = new JavadocDao(dir);
		return new JavadocCommand(dao);
	}

	private Main() {
		//hide
	}
}
