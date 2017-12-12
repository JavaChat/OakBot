package oakbot.local;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;

import oakbot.BotProperties;
import oakbot.Database;
import oakbot.JsonDatabase;
import oakbot.Rooms;
import oakbot.Statistics;
import oakbot.bot.Bot;
import oakbot.command.AboutCommand;
import oakbot.command.AdventOfCodeApi;
import oakbot.command.AdventOfCodeCommand;
import oakbot.command.AfkCommand;
import oakbot.command.CatCommand;
import oakbot.command.Command;
import oakbot.command.EightBallCommand;
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
import oakbot.command.WikiCommand;
import oakbot.command.define.DefineCommand;
import oakbot.command.http.HttpCommand;
import oakbot.command.javadoc.JavadocCommand;
import oakbot.command.javadoc.JavadocDao;
import oakbot.command.javadoc.JavadocDaoCached;
import oakbot.command.javadoc.JavadocDaoUncached;
import oakbot.command.learn.LearnCommand;
import oakbot.command.learn.LearnedCommands;
import oakbot.command.learn.UnlearnCommand;
import oakbot.command.urban.UrbanCommand;
import oakbot.filter.ChatResponseFilter;
import oakbot.filter.GrootFilter;
import oakbot.filter.UpsidedownTextFilter;
import oakbot.listener.AfkListener;
import oakbot.listener.FatCatListener;
import oakbot.listener.JavadocListener;
import oakbot.listener.Listener;
import oakbot.listener.MentionListener;
import oakbot.listener.WaveListener;
import oakbot.listener.WelcomeListener;

/**
 * <p>
 * Runs the chat bot locally using a mock chat connection for testing purposes.
 * </p>
 * <p>
 * When this class is run, a file named "local.input.txt" will be created at the
 * root of the project. This file is used to enter messages into the chat room.
 * Messages are entered one per line. Multi-line messages can be entered by
 * ending each line with a backslash until you reach the last line.
 * </p>
 * <p>
 * Saving the "local.input.text" file causes the message(s) to be sent to the
 * chat room. Do not delete anything from this file while the program is
 * running, as this will mess it up. This file is re-created every time the
 * program runs.
 * </p>
 * <p>
 * All chat messages are sent to stdout. Both your messages and the bot's are
 * displayed there.
 * </p>
 * <p>
 * If you don't want to use the "bot.properties" file to initialize the bot, you
 * can specify the path to a different file as a CLI argument.
 * </p>
 * @author Michael Angstadt
 */
public class Main {
	private static final Path db = Paths.get("local.db.json");

	public static void main(String[] args) throws Exception {
		LogManager.getLogManager().getLogger("").setLevel(Level.WARNING);

		Path settings = Paths.get((args.length == 0) ? "bot.properties" : args[0]);
		BotProperties props = loadProperties(settings);

		Database database = new JsonDatabase(db);
		Statistics stats = new Statistics(database);
		Rooms rooms = new Rooms(database, props.getHomeRooms(), props.getQuietRooms());
		LearnedCommands learnedCommands = new LearnedCommands(database);

		JavadocCommand javadocCommand = createJavadocCommand(props);
		AfkCommand afkCommand = new AfkCommand();
		FatCatCommand fatCatCommand = new FatCatCommand(database);

		UpsidedownTextFilter upsidedownTextFilter = new UpsidedownTextFilter();
		GrootFilter grootFilter = new GrootFilter();

		List<Listener> listeners = new ArrayList<>();
		{
			MentionListener mentionListener = new MentionListener(props.getBotUserName());

			if (javadocCommand != null) {
				listeners.add(new JavadocListener(javadocCommand));
			}
			listeners.add(new AfkListener(afkCommand));
			listeners.add(new WaveListener(props.getBotUserName(), 1000, mentionListener));
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
			commands.add(new CatCommand(props.getCatKey()));
			commands.add(fatCatCommand);

			String adventSession = props.getAdventOfCodeSession();
			if (adventSession != null) {
				AdventOfCodeApi api = new AdventOfCodeApi(adventSession);
				commands.add(new AdventOfCodeCommand(props.getAdventOfCodeLeaderboards(), api));
			}

			String reactKey = props.getReactKey();
			if (reactKey != null) {
				commands.add(new ReactCommand(reactKey));
			}
		}

		List<ChatResponseFilter> filters = new ArrayList<>();
		{
			filters.add(grootFilter);
			filters.add(upsidedownTextFilter); //should be last
		}

		//@formatter:off
		FileChatClient connection = new FileChatClient(
			props.getBotUserId(), props.getBotUserName(),
			props.getAdmins().get(0), "Michael"
		);
		//@formatter:on

		//@formatter:off
		Bot bot = new Bot.Builder()
			.login(props.getLoginEmail(), props.getLoginPassword())
			.commands(commands)
			.learnedCommands(learnedCommands)
			.listeners(listeners)
			.responseFilters(filters)
			.connection(connection)
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

		bot.connect(false).join();
	}

	private static BotProperties loadProperties(Path file) throws IOException {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
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

	private Main() {
		//hide
	}
}
