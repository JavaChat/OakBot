package oakbot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import oakbot.inactivity.InactivityTask;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.container.jdk.client.JdkClientContainer;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.google.common.collect.Multimap;

import oakbot.bot.Bot;
import oakbot.chat.ChatClient;
import oakbot.chat.IChatClient;
import oakbot.chat.Site;
import oakbot.chat.mock.FileChatClient;
import oakbot.command.Command;
import oakbot.command.HelpCommand;
import oakbot.command.learn.LearnCommand;
import oakbot.command.learn.LearnedCommandsDao;
import oakbot.command.learn.UnlearnCommand;
import oakbot.filter.ChatResponseFilter;
import oakbot.listener.CommandListener;
import oakbot.listener.Listener;
import oakbot.task.ScheduledTask;

/**
 * @author Michael Angstadt
 */
public final class Main {
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

	private static final String defaultContextPath = "bot-context.xml";

	public static void main(String[] args) throws Exception {
		CliArguments arguments = new CliArguments(args);

		if (arguments.help()) {
			String help = arguments.printHelp(defaultContextPath);
			System.out.println(help);
			return;
		}

		if (arguments.version()) {
			System.out.println(Main.VERSION);
			return;
		}

		boolean mock = arguments.mock();

		String contextPath = arguments.context();
		if (contextPath == null) {
			contextPath = defaultContextPath;
		}

		setupLogging();

		BotProperties botProperties;
		Database database;
		Statistics stats;

		List<Command> commands;
		List<Listener> listeners;
		List<ChatResponseFilter> filters;
		List<ScheduledTask> tasks;
		List<InactivityTask> inactivityTasks;

		try (FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(contextPath)) {
			botProperties = new BotProperties((Properties) context.getBean("settings"));
			database = context.getBean(Database.class);
			stats = context.getBean(Statistics.class);

			commands = new ArrayList<>(context.getBeansOfType(Command.class).values());
			listeners = new ArrayList<>(context.getBeansOfType(Listener.class).values());
			filters = new ArrayList<>(context.getBeansOfType(ChatResponseFilter.class).values());
			tasks = new ArrayList<>(context.getBeansOfType(ScheduledTask.class).values());
			inactivityTasks = new ArrayList<>(context.getBeansOfType(InactivityTask.class).values());
		}

		LearnedCommandsDao learnedCommands;
		if (botProperties.isEnableLearnedCommands()) {
			learnedCommands = new LearnedCommandsDao(database);

			LearnCommand learnCommand = new LearnCommand(commands, learnedCommands);
			commands.add(learnCommand);
			UnlearnCommand unlearnCommand = new UnlearnCommand(commands, learnedCommands);
			commands.add(unlearnCommand);
		} else {
			learnedCommands = new LearnedCommandsDao(new MemoryDatabase());
		}

		HelpCommand helpCommand = new HelpCommand(commands, learnedCommands, listeners);
		commands.add(helpCommand);

		CommandListener commandListener = new CommandListener(commands, learnedCommands);
		listeners.add(commandListener);

		Multimap<String, Command> duplicateNames = commandListener.checkForDuplicateNames();
		outputDuplicateNamesWarning(duplicateNames, botProperties.getTrigger());

		Rooms rooms = new Rooms(database, botProperties.getHomeRooms(), botProperties.getQuietRooms());

		IChatClient connection;
		if (mock) {
			connection = new FileChatClient(botProperties.getBotUserId(), botProperties.getBotUserName(), botProperties.getAdmins().get(0), "Michael", "https://www.gravatar.com/avatar/29d7c49f6f174710788c79011219bae1");
		} else {
			Site site = getSite(botProperties);

			/*
			 * 4/2/2020: You cannot just call HttpClients.createDefault(). When
			 * this method is used, Apache's HTTP library does not properly
			 * parse the cookies it receives when logging into StackOverflow,
			 * and thus cannot join any chat rooms.
			 * 
			 * Solution found here: https://stackoverflow.com/q/36473478/13379
			 */
			CloseableHttpClient httpClient = HttpClients.custom() //@formatter:off
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
			.build(); //@formatter:on

			ClientManager websocketClient = ClientManager.createClient(JdkClientContainer.class.getName());
			websocketClient.setDefaultMaxSessionIdleTimeout(0);
			websocketClient.getProperties().put(ClientProperties.RETRY_AFTER_SERVICE_UNAVAILABLE, true);

			System.out.println("Logging in as " + botProperties.getLoginEmail() + "...");
			connection = new ChatClient(httpClient, websocketClient, site);
			connection.login(botProperties.getLoginEmail(), botProperties.getLoginPassword());
		}

		//@formatter:off
		Bot bot = new Bot.Builder()
			.connection(connection)
			.listeners(listeners)
			.tasks(tasks)
			.inactivityTasks(inactivityTasks)
			.responseFilters(filters)
			.admins(botProperties.getAdmins())
			.bannedUsers(botProperties.getBannedUsers())
			.user(botProperties.getBotUserName(), botProperties.getBotUserId())
			.trigger(botProperties.getTrigger())
			.greeting(botProperties.getGreeting())
			.rooms(rooms)
			.stats(stats)
			.database(database)
			.hideOneboxesAfter(botProperties.getHideOneboxesAfter()) //TODO more generic name for this property
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

	private static void outputDuplicateNamesWarning(Multimap<String, Command> duplicateNames, String trigger) {
		if (duplicateNames.isEmpty()) {
			return;
		}

		System.out.println("Warning: Commands that share the same names/aliases have been found.");
		for (Map.Entry<String, Collection<Command>> entry : duplicateNames.asMap().entrySet()) {
			String name = entry.getKey();
			Collection<Command> commandsWithSameName = entry.getValue();

			String list = commandsWithSameName.stream() //@formatter:off
				.map(c -> c.getClass().getName())
			.collect(Collectors.joining(", ")); //@formatter:on

			System.out.println("  " + trigger + name + ": " + list);
		}
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
