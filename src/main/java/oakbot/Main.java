package oakbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.github.mangstadt.sochat4j.ChatClient;
import com.github.mangstadt.sochat4j.IChatClient;
import com.github.mangstadt.sochat4j.Site;
import com.google.common.collect.Multimap;

import oakbot.bot.Bot;
import oakbot.chat.mock.FileChatClient;
import oakbot.command.Command;
import oakbot.command.HelpCommand;
import oakbot.command.learn.LearnCommand;
import oakbot.command.learn.LearnedCommandsDao;
import oakbot.command.learn.UnlearnCommand;
import oakbot.filter.ChatResponseFilter;
import oakbot.inactivity.InactivityTask;
import oakbot.listener.CatchAllMentionListener;
import oakbot.listener.CommandListener;
import oakbot.listener.Listener;
import oakbot.task.ScheduledTask;

/**
 * @author Michael Angstadt
 */
public final class Main {
	private static final Logger logger = Logger.getLogger(Main.class.getName());

	public static final String VERSION;
	public static final String URL;
	public static final Instant BUILT;
	static {
		Properties props = new Properties();
		try (InputStream in = Main.class.getResourceAsStream("/info.properties")) {
			props.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
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

		setupLogging(botProperties.getLoggingConfig());

		/*
		 * Put the "catch all" listeners at the end so they won't run if another
		 * listener tells them not to run.
		 */
		Collections.sort(listeners, (a, b) -> {
			if (a instanceof CatchAllMentionListener) {
				return 1;
			}
			if (b instanceof CatchAllMentionListener) {
				return -1;
			}
			return 0;
		});

		LearnedCommandsDao learnedCommands;
		if (botProperties.isEnableLearnedCommands()) {
			learnedCommands = new LearnedCommandsDao(database);

			LearnCommand learnCommand = new LearnCommand(commands, learnedCommands);
			commands.add(learnCommand);
			UnlearnCommand unlearnCommand = new UnlearnCommand(commands, learnedCommands);
			commands.add(unlearnCommand);
		} else {
			learnedCommands = new LearnedCommandsDao();
		}

		HelpCommand helpCommand = new HelpCommand(commands, learnedCommands, listeners, tasks, botProperties.getHelpWebpage());
		commands.add(helpCommand);

		CommandListener commandListener = new CommandListener(commands, learnedCommands);
		listeners.add(commandListener);

		Multimap<String, Command> duplicateNames = commandListener.checkForDuplicateNames();
		outputDuplicateNamesWarning(duplicateNames, botProperties.getTrigger());

		IChatClient connection;
		if (mock) {
			connection = new FileChatClient(botProperties.getBotUserId(), botProperties.getBotUserName(), botProperties.getAdmins().get(0), "Michael", "https://www.gravatar.com/avatar/29d7c49f6f174710788c79011219bae1");
		} else {
			Site site = getSite(botProperties);

			System.out.println("Logging in as " + botProperties.getLoginEmail() + "...");
			connection = ChatClient.connect(site, botProperties.getLoginEmail(), botProperties.getLoginPassword());
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
			.roomsHome(botProperties.getHomeRooms())
			.roomsQuiet(botProperties.getQuietRooms())
			.stats(stats)
			.database(database)
			.hideOneboxesAfter(botProperties.getHideOneboxesAfter()) //TODO more generic name for this property
		.build();
		//@formatter:on

		createShutdownHook(bot);
		createSocket(botProperties.getSocketPort(), bot);

		System.out.println("Joining rooms...");

		Thread t = bot.connect(arguments.quiet());

		System.out.println("Bot has launched successfully. To move this process to the background, press Ctrl+Z then type \"bg\".");

		/*
		 * Don't catch unhandled exceptions until the bot has started. Any
		 * exceptions that are thrown during the initial boot up process should
		 * be dumped to the console.
		 */
		createDefaultExceptionHandler();

		t.join();

		logger.info(() -> "Terminating.");
	}

	private static void setupLogging(Path config) throws IOException {
		if (!Files.exists(config)) {
			return;
		}

		try (InputStream in = Files.newInputStream(config)) {
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

			String list = commandsWithSameName
					.stream() //@formatter:off
				.map(c -> c.getClass().getName())
			.collect(Collectors.joining(", ")); //@formatter:on

			System.out.println("  " + trigger + name + ": " + list);
		}
	}

	private static void createDefaultExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler((thread, thrown) -> {
			logger.log(Level.SEVERE, thrown, () -> "Uncaught exception thrown.");
		});
	}

	private static void createShutdownHook(Bot bot) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info(() -> "Running shutdown hook.");
			bot.stop();
		}));
	}

	private static void createSocket(int port, Bot bot) throws IOException {
		ServerSocket serverSocket = new ServerSocket(port);
		if (port == 0) {
			port = serverSocket.getLocalPort();
		}

		Thread t = new SocketThread(serverSocket, bot);
		t.start();

		System.out.println("Listening for socket commands on port " + port + ".");
	}

	private static class SocketThread extends Thread {
		private final ServerSocket serverSocket;
		private final Bot bot;

		public SocketThread(ServerSocket serverSocket, Bot bot) {
			this.serverSocket = serverSocket;
			this.bot = bot;
			setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				try (Socket socket = serverSocket.accept(); BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if ("shutdown".equalsIgnoreCase(line)) {
							bot.stop();
							closeSocket();
							return;
						}
					}
				} catch (IOException e) {
					logger.log(Level.SEVERE, e, () -> "Problem accepting new socket connection or reading from socket.");
				}
			}
		}

		private void closeSocket() {
			try {
				serverSocket.close();
			} catch (IOException e) {
				logger.log(Level.SEVERE, e, () -> "Problem closing server socket.");
			}
		}
	}

	private static Site getSite(BotProperties props) {
		String domain = props.getSite();
		if (domain == null || domain.trim().isEmpty()) {
			return Site.STACKOVERFLOW;
		}

		Site[] sites = { Site.STACKOVERFLOW, Site.STACKEXCHANGE, Site.META };
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
