package oakbot;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.container.jdk.client.JdkClientContainer;

import oakbot.bot.Bot;
import oakbot.chat.ChatClient;
import oakbot.chat.IChatClient;
import oakbot.chat.Site;
import oakbot.chat.mock.FileChatClient;
import oakbot.command.AboutCommand;
import oakbot.command.AdventOfCodeApi;
import oakbot.command.AdventOfCodeCommand;
import oakbot.command.AfkCommand;
import oakbot.command.CatCommand;
import oakbot.command.Command;
import oakbot.command.EightBallCommand;
import oakbot.command.FacepalmCommand;
import oakbot.command.FatCatCommand;
import oakbot.command.GrootCommand;
import oakbot.command.HelpCommand;
import oakbot.command.ReactCommand;
import oakbot.command.RollCommand;
import oakbot.command.RolloverCommand;
import oakbot.command.ShrugCommand;
import oakbot.command.ShutdownCommand;
import oakbot.command.SummonCommand;
import oakbot.command.TagCommand;
import oakbot.command.UnsummonCommand;
import oakbot.command.WaduCommand;
import oakbot.command.WikiCommand;
import oakbot.command.define.DefineCommand;
import oakbot.command.effective.EffectiveJavaCommand;
import oakbot.command.http.HttpCommand;
import oakbot.command.javadoc.JavadocCommand;
import oakbot.command.javadoc.JavadocDao;
import oakbot.command.javadoc.JavadocDaoCached;
import oakbot.command.javadoc.JavadocDaoUncached;
import oakbot.command.learn.LearnCommand;
import oakbot.command.learn.LearnedCommandsDao;
import oakbot.command.learn.UnlearnCommand;
import oakbot.command.urban.UrbanCommand;
import oakbot.filter.ChatResponseFilter;
import oakbot.filter.GrootFilter;
import oakbot.filter.UpsidedownTextFilter;
import oakbot.filter.WaduFilter;
import oakbot.listener.AfkListener;
import oakbot.listener.FatCatListener;
import oakbot.listener.JavadocListener;
import oakbot.listener.Listener;
import oakbot.listener.MentionListener;
import oakbot.listener.MornListener;
import oakbot.listener.WaveListener;
import oakbot.listener.WelcomeListener;
import oakbot.task.AdventOfCodeTask;
import oakbot.task.FOTD;
import oakbot.task.HealthMonitor;
import oakbot.task.QOTD;
import oakbot.task.ScheduledTask;

/**
 * @author Michael Angstadt
 */
public class Main {
	private static final Logger logger = Logger.getLogger(Main.class.getName());

	public static final String VERSION, URL;
	public static final Instant BUILT;
	static {
		Properties props = new Properties();
		try (InputStream in = Main.class.getResourceAsStream("/info.properties")) {
			props.load(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		VERSION = props.getProperty("version");
		URL = props.getProperty("url");

		Instant built;
		String builtStr = props.getProperty("built");
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
		try {
			built = OffsetDateTime.parse(builtStr, formatter).toInstant();
		} catch (DateTimeParseException e) {
			/*
			 * If the project is run from a development environment, the
			 * properties file might not have been filtered, so just set the
			 * build date to now.
			 */
			built = Instant.now();
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

		boolean mock = arguments.mock();

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
		Rooms rooms = new Rooms(database, props.getHomeRooms(), props.getQuietRooms());
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao(database);

		JavadocCommand javadocCommand = createJavadocCommand(props);
		AfkCommand afkCommand = new AfkCommand();
		FatCatCommand fatCatCommand = new FatCatCommand(database);

		String aocSession = props.getAdventOfCodeSession();
		Map<Integer, String> aocDefaultLeaderboards = props.getAdventOfCodeLeaderboards();
		AdventOfCodeApi aocApi = (aocSession == null) ? null : new AdventOfCodeApi(aocSession);

		UpsidedownTextFilter upsidedownTextFilter = new UpsidedownTextFilter();
		GrootFilter grootFilter = new GrootFilter();
		WaduFilter waduFilter = new WaduFilter();

		List<Listener> listeners = new ArrayList<>();
		{
			MentionListener mentionListener = new MentionListener(props.getBotUserName());

			if (javadocCommand != null) {
				listeners.add(new JavadocListener(javadocCommand));
			}
			listeners.add(new AfkListener(afkCommand));
			listeners.add(new WaveListener(props.getBotUserName(), 1000, mentionListener));
			listeners.add(new MornListener(props.getBotUserName(), 1000, mentionListener));
			listeners.add(new WelcomeListener(database, props.getWelcomeMessages()));
			listeners.add(new FatCatListener(fatCatCommand));

			/*
			 * Put mention listener at the bottom so the other listeners have a
			 * chance to override it.
			 */
			listeners.add(mentionListener);
		}

		List<Command> commands = new ArrayList<>();
		{
			commands.add(new AboutCommand(stats, props.getAboutHost()));
			commands.add(new HelpCommand(commands, learnedCommands, listeners));

			if (javadocCommand != null) {
				commands.add(javadocCommand);
			}

			commands.add(new HttpCommand());
			commands.add(new EffectiveJavaCommand());
			commands.add(new WikiCommand());
			commands.add(new TagCommand());
			commands.add(new UrbanCommand());

			String dictionaryKey = props.getDictionaryKey();
			if (dictionaryKey != null) {
				commands.add(new DefineCommand(dictionaryKey));
			}

			commands.add(new RollCommand());
			commands.add(new EightBallCommand());
			commands.add(new SummonCommand(2));
			commands.add(new UnsummonCommand());
			commands.add(new ShutdownCommand());
			commands.add(new LearnCommand(commands, learnedCommands));
			commands.add(new UnlearnCommand(commands, learnedCommands));
			commands.add(new ShrugCommand());
			commands.add(afkCommand);
			commands.add(new RolloverCommand(upsidedownTextFilter));
			commands.add(new GrootCommand(grootFilter));
			commands.add(new WaduCommand(waduFilter));
			commands.add(new CatCommand(props.getCatKey()));
			commands.add(fatCatCommand);

			if (aocApi != null) {
				commands.add(new AdventOfCodeCommand(aocDefaultLeaderboards, aocApi));
			}

			String reactKey = props.getReactKey();
			if (reactKey != null) {
				commands.add(new ReactCommand(reactKey));
			}

			String tenorKey = props.getTenorKey();
			if (tenorKey != null) {
				commands.add(new FacepalmCommand(tenorKey));
			}
		}

		List<ScheduledTask> tasks = new ArrayList<>();
		{
			tasks.add(new QOTD());
			tasks.add(new FOTD());

			List<Integer> healthMonitor = props.getHealthMonitor();
			if (!healthMonitor.isEmpty()) {
				tasks.add(new HealthMonitor(props.getHealthMonitor()));
			}

			if (aocApi != null && !aocDefaultLeaderboards.isEmpty()) {
				tasks.add(new AdventOfCodeTask(aocDefaultLeaderboards, aocApi));
			}
		}

		List<ChatResponseFilter> filters = new ArrayList<>();
		{
			filters.add(grootFilter);
			filters.add(waduFilter);
			filters.add(upsidedownTextFilter); //should be last
		}

		IChatClient connection;
		if (mock) {
			connection = new FileChatClient(props.getBotUserId(), props.getBotUserName(), props.getAdmins().get(0), "Michael");
		} else {
			Site site = getSite(props);

			CloseableHttpClient httpClient = HttpClients.createDefault();

			ClientManager websocketClient = ClientManager.createClient(JdkClientContainer.class.getName());
			websocketClient.setDefaultMaxSessionIdleTimeout(0);
			websocketClient.getProperties().put(ClientProperties.RETRY_AFTER_SERVICE_UNAVAILABLE, true);

			System.out.println("Logging in as " + props.getLoginEmail() + "...");
			connection = new ChatClient(httpClient, websocketClient, site);
			connection.login(props.getLoginEmail(), props.getLoginPassword());
		}

		//@formatter:off
		Bot bot = new Bot.Builder()
			.connection(connection)
			.commands(commands)
			.learnedCommands(learnedCommands)
			.listeners(listeners)
			.tasks(tasks)
			.responseFilters(filters)
			.admins(props.getAdmins())
			.bannedUsers(props.getBannedUsers())
			.user(props.getBotUserName(), props.getBotUserId())
			.trigger(props.getTrigger())
			.greeting(props.getGreeting())
			.rooms(rooms)
			.stats(stats)
			.database(database)
			.hideOneboxesAfter(props.getHideOneboxesAfter())
		.build();
		//@formatter:on

		/*
		 * Don't catch unhandled exceptions until the bot has started. Any
		 * exceptions that are thrown during the initial boot up process should
		 * be dumped to the console.
		 */
		Thread.setDefaultUncaughtExceptionHandler((thread, thrown) -> {
			logger.log(Level.SEVERE, "Uncaught exception thrown.", thrown);
		});

		System.out.println("Joining rooms...");

		Thread t = bot.connect(arguments.quiet());

		System.out.println("Bot has launched successfully. To move this process to the background, press Ctrl+Z then type \"bg\".");

		t.join();

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
	}

	private static BotProperties loadProperties(Path file) throws IOException {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(file)) {
			properties.load(reader);
		}
		return new BotProperties(properties);
	}

	private static JavadocCommand createJavadocCommand(BotProperties props) throws IOException {
		Path javadocPath = props.getJavadocPath();
		if (javadocPath == null) {
			return null;
		}

		boolean javadocCache = props.getJavadocCache();
		JavadocDao dao = javadocCache ? new JavadocDaoCached(javadocPath) : new JavadocDaoUncached(javadocPath);
		return new JavadocCommand(dao);
	}

	private static Site getSite(BotProperties props) {
		String domain = props.getSite();
		if (domain == null || domain.trim().isEmpty()) {
			return Site.STACKOVERFLOW;
		}

		Site sites[] = new Site[] { Site.STACKOVERFLOW, Site.STACKEXCHANGE, Site.META };
		for (Site site : sites) {
			if (site.getDomain().equalsIgnoreCase(domain)) {
				return site;
			}
		}

		return null;
	}

	private Main() {
		//hide
	}
}
