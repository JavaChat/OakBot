package oakbot.local;

import java.io.BufferedReader;
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
	private static final Path input = Paths.get("local.input.txt");

	public static void main(String[] args) throws Exception {
		LogManager.getLogManager().getLogger("").setLevel(Level.WARNING);

		Path settings = Paths.get((args.length == 0) ? "bot.properties" : args[0]);
		BotProperties props = loadProperties(settings);

		Files.deleteIfExists(input);
		Files.createFile(input);

		Database database = new JsonDatabase(db);
		Statistics stats = new Statistics(database);
		Rooms rooms = new Rooms(database, props.getHomeRooms());
		LearnedCommands learnedCommands = new LearnedCommands(database);

		Path javadocPath = props.getJavadocPath();
		JavadocCommand javadocCommand = (javadocPath == null) ? null : createJavadocCommand(javadocPath);

		AfkCommand afkCommand = new AfkCommand();

		UpsidedownTextFilter upsidedownTextFilter = new UpsidedownTextFilter();
		upsidedownTextFilter.setEnabled(false);

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

		final MockChatConnection connection = new MockChatConnection(1, props.getBotUserName());

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
			.user(props.getBotUserName(), props.getBotUserId())
			.trigger(props.getTrigger())
			.greeting(props.getGreeting())
			.rooms(rooms)
			.stats(stats)
			.database(database)
		.build();
		//@formatter:on

		monitorInputFile(props.getAdmins().get(0), connection);

		bot.connect(false);
	}

	private static void monitorInputFile(int adminId, MockChatConnection connection) {
		Thread t = new Thread(() -> {
			try (BufferedReader reader = Files.newBufferedReader(input)) {
				while (true) {
					Thread.sleep(1000);

					String line = reader.readLine();
					if (line == null) {
						continue;
					}

					StringBuilder sb = new StringBuilder(line.length());
					while (line != null) {
						boolean multiline = line.endsWith("\\");
						if (!multiline) {
							sb.append(line);
							break;
						}

						sb.append(line, 0, line.length() - 1).append('\n');
						line = reader.readLine();
					}

					connection.postMessage(1, adminId, "Michael", sb.toString());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				return;
			}
		});
		t.setDaemon(true);
		t.start();
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
