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
import oakbot.command.AfkCommand;
import oakbot.command.Command;
import oakbot.command.EightBallCommand;
import oakbot.command.HelpCommand;
import oakbot.command.RollCommand;
import oakbot.command.RolloverCommand;
import oakbot.command.ShrugCommand;
import oakbot.command.ShutdownCommand;
import oakbot.command.SummonCommand;
import oakbot.command.TagCommand;
import oakbot.command.UnsummonCommand;
import oakbot.command.WikiCommand;
import oakbot.command.define.DefineCommand;
import oakbot.command.http.HttpCommand;
import oakbot.command.javadoc.JavadocCommand;
import oakbot.command.javadoc.JavadocDao;
import oakbot.command.learn.LearnCommand;
import oakbot.command.learn.LearnedCommands;
import oakbot.command.learn.UnlearnCommand;
import oakbot.command.urban.UrbanCommand;
import oakbot.filter.ChatResponseFilter;
import oakbot.filter.UpsidedownTextFilter;
import oakbot.listener.AfkListener;
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

	public static final Path defaultSettings = Paths.get("bot.properties");
	public static final Path defaultDb = Paths.get("db.json");

	public static void main(String[] args) throws Exception {
		CliArguments arguments = new CliArguments(args);

		if (arguments.help()) {
			String help = arguments.printHelp(defaultSettings, defaultDb);
			System.out.println(help);
			return;
		}

		if (arguments.version()) {
			System.out.println(Main.VERSION);
			return;
		}

		Path settings = arguments.settings();
		if (settings == null) {
			settings = defaultSettings;
		}

		Path db = arguments.db();
		if (db == null) {
			db = defaultDb;
		}

		setupLogging();
		BotProperties props = loadProperties(settings);

		Database database = new JsonDatabase(db);
		Statistics stats = new Statistics(database);
		Rooms rooms = new Rooms(database, props.getHomeRooms());
		LearnedCommands learnedCommands = new LearnedCommands(database);

		Path javadocPath = props.getJavadocPath();
		JavadocCommand javadocCommand = (javadocPath == null) ? null : createJavadocCommand(javadocPath);

		AfkCommand afkCommand = new AfkCommand();

		UpsidedownTextFilter upsidedownTextFilter = new UpsidedownTextFilter();

		List<Listener> listeners = new ArrayList<>();
		{
			listeners.add(new MentionListener(props.getBotUserName(), props.getTrigger()));
			if (javadocCommand != null) {
				listeners.add(new JavadocListener(javadocCommand));
			}
			listeners.add(new AfkListener(afkCommand, props.getTrigger()));
		}

		List<Command> commands = new ArrayList<>();
		{
			commands.add(new AboutCommand(stats, props.getAboutHost()));
			commands.add(new HelpCommand(commands, learnedCommands, listeners));

			if (javadocCommand != null) {
				commands.add(javadocCommand);
			}

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
			commands.add(new UnsummonCommand());
			commands.add(new ShutdownCommand());
			commands.add(new LearnCommand(commands, learnedCommands));
			commands.add(new UnlearnCommand(commands, learnedCommands));
			commands.add(new ShrugCommand());
			commands.add(afkCommand);
			commands.add(new RolloverCommand(upsidedownTextFilter));
		}

		List<ChatResponseFilter> filters = new ArrayList<>();
		{
			filters.add(upsidedownTextFilter);
		}

		ChatConnection connection = new StackoverflowChat(HttpClientBuilder.create().build());

		//@formatter:off
		Bot bot = new Bot.Builder()
			.login(props.getLoginEmail(), props.getLoginPassword())
			.commands(commands)
			.learnedCommands(learnedCommands)
			.listeners(listeners)
			.responseFilters(filters)
			.connection(connection)
			.heartbeat(props.getHeartbeat())
			.admins(props.getAdmins())
			.bannedUsers(props.getBannedUsers())
			.user(props.getBotUserName(), props.getBotUserId())
			.trigger(props.getTrigger())
			.greeting(props.getGreeting())
			.rooms(rooms)
			.stats(stats)
			.database(database)
		.build();
		//@formatter:on

		bot.connect(arguments.quiet());

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

	private static BotProperties loadProperties(Path file) throws IOException {
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
