package oakbot;

import java.io.BufferedReader;
import java.io.IOException;
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
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static final String VERSION;
	public static final String URL;
	public static final Instant BUILT;

	static {
		var props = new Properties();
		try (var in = Main.class.getResourceAsStream("/info.properties")) {
			props.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		VERSION = props.getProperty("version");
		URL = props.getProperty("url");

		Instant built;
		var builtStr = props.getProperty("built");
		var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
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
		var arguments = new CliArguments(args);

		if (arguments.help()) {
			var help = arguments.printHelp(defaultContextPath);
			System.out.println(help);
			return;
		}

		if (arguments.version()) {
			System.out.println(Main.VERSION);
			return;
		}

		var mock = arguments.mock();
		var contextPath = (arguments.context() == null) ? defaultContextPath : arguments.context();

		BotProperties botProperties;
		Database database;
		Statistics stats;

		List<Command> commands;
		List<Listener> listeners;
		List<ChatResponseFilter> filters;
		List<ScheduledTask> tasks;
		List<InactivityTask> inactivityTasks;

		try (var context = new FileSystemXmlApplicationContext(contextPath)) {
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
		listeners.sort((a, b) -> {
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

			var learnCommand = new LearnCommand(commands, learnedCommands);
			commands.add(learnCommand);
			var unlearnCommand = new UnlearnCommand(commands, learnedCommands);
			commands.add(unlearnCommand);
		} else {
			learnedCommands = new LearnedCommandsDao();
		}

		var helpCommand = new HelpCommand(commands, learnedCommands, listeners, tasks, botProperties.getHelpWebpage());
		commands.add(helpCommand);

		var commandListener = new CommandListener(commands, learnedCommands);
		listeners.add(commandListener);

		var duplicateNames = commandListener.checkForDuplicateNames();
		outputDuplicateNamesWarning(duplicateNames, botProperties.getTrigger());

		IChatClient connection;
		if (mock) {
			connection = new FileChatClient(botProperties.getBotUserId(), botProperties.getBotUserName(), botProperties.getAdmins().get(0), "Michael", "https://www.gravatar.com/avatar/29d7c49f6f174710788c79011219bae1");
		} else {
			var site = getSite(botProperties);
			var email = botProperties.getLoginEmail();
			var password = botProperties.getLoginPassword();
			var webSocketRefreshInterval = botProperties.getWebSocketRefreshInterval();

			System.out.println("Logging in as " + email + "...");
			connection = ChatClient.connect(site, email, password, webSocketRefreshInterval);
		}

		//@formatter:off
		var bot = new Bot.Builder()
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
		var socketThread = createSocket(botProperties.getSocketPort(), bot);

		System.out.println("Joining rooms...");

		var t = bot.connect(arguments.quiet());

		System.out.println("Bot has launched successfully. To move this process to the background, press Ctrl+Z then type \"bg\".");

		/*
		 * Don't catch unhandled exceptions until the bot has started. Any
		 * exceptions that are thrown during the initial boot up process should
		 * be dumped to the console.
		 */
		createDefaultExceptionHandler();

		t.join();
		socketThread.closeSocket();

		logger.atInfo().log(() -> "Terminating.");
	}

	private static void setupLogging(Path config) throws IOException {
		if (!Files.exists(config)) {
			return;
		}

		try (var in = Files.newInputStream(config)) {
			LogManager.getLogManager().readConfiguration(in);
		}
	}

	private static void outputDuplicateNamesWarning(Multimap<String, Command> duplicateNames, String trigger) {
		if (duplicateNames.isEmpty()) {
			return;
		}

		System.out.println("Warning: Commands that share the same names/aliases have been found.");
		for (var entry : duplicateNames.asMap().entrySet()) {
			var name = entry.getKey();
			var commandsWithSameName = entry.getValue();

			//@formatter:off
			var list = commandsWithSameName.stream()
				.map(c -> c.getClass().getName())
			.collect(Collectors.joining(", "));
            //@formatter:on

			System.out.println("  " + trigger + name + ": " + list);
		}
	}

	private static void createDefaultExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler((thread, thrown) -> {
			logger.atError().setCause(thrown).log(() -> "Uncaught exception thrown.");
		});
	}

	private static void createShutdownHook(Bot bot) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.atInfo().log(() -> "Running shutdown hook.");
			bot.stop();
		}));
	}

	private static SocketThread createSocket(int port, Bot bot) throws IOException {
		var serverSocket = new ServerSocket(port);
		if (port == 0) {
			port = serverSocket.getLocalPort();
		}

		var t = new SocketThread(serverSocket, bot);
		t.start();

		System.out.println("Listening for socket commands on port " + port + ".");

		return t;
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
					if (serverSocket.isClosed()) {
						return;
					}
					logger.atError().setCause(e).log(() -> "Problem accepting new socket connection or reading from socket.");
				}
			}
		}

		private void closeSocket() {
			try {
				serverSocket.close();
			} catch (IOException e) {
				logger.atError().setCause(e).log(() -> "Problem closing server socket.");
			}
		}
	}

	private static Site getSite(BotProperties props) {
		String domain = props.getSite();
		if (domain == null || domain.trim().isEmpty()) {
			return Site.STACKOVERFLOW;
		}

		for (Site site : Site.values()) {
			if (site.getDomain().equalsIgnoreCase(domain)) {
				return site;
			}
		}

		throw new IllegalArgumentException("Unrecognized site: " + domain);
	}

	private Main() {
		//hide
	}
}
