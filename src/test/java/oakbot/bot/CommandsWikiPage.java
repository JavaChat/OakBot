package oakbot.bot;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import oakbot.Database;
import oakbot.command.AboutCommand;
import oakbot.command.AfkCommand;
import oakbot.command.CatCommand;
import oakbot.command.Command;
import oakbot.command.DeleteCommand;
import oakbot.command.EightBallCommand;
import oakbot.command.FacepalmCommand;
import oakbot.command.FatCatCommand;
import oakbot.command.HelpCommand;
import oakbot.command.HelpDoc;
import oakbot.command.JuiceBoxCommand;
import oakbot.command.ReactCommand;
import oakbot.command.RemindCommand;
import oakbot.command.RollCommand;
import oakbot.command.ShrugCommand;
import oakbot.command.ShutdownCommand;
import oakbot.command.SummonCommand;
import oakbot.command.TagCommand;
import oakbot.command.UnsummonCommand;
import oakbot.command.WikiCommand;
import oakbot.command.aoc.AdventOfCode;
import oakbot.command.define.DefineCommand;
import oakbot.command.effective.EffectiveDebuggingCommand;
import oakbot.command.effective.EffectiveJavaCommand;
import oakbot.command.http.HttpCommand;
import oakbot.command.javadoc.JavadocCommand;
import oakbot.command.learn.LearnCommand;
import oakbot.command.learn.LearnedCommandsDao;
import oakbot.command.learn.UnlearnCommand;
import oakbot.command.stands4.AbbreviationCommand;
import oakbot.command.stands4.ConvertCommand;
import oakbot.command.stands4.ExplainCommand;
import oakbot.command.stands4.GrammarCommand;
import oakbot.command.urban.UrbanCommand;
import oakbot.filter.GrootFilter;
import oakbot.filter.UpsidedownTextFilter;
import oakbot.filter.WaduFilter;
import oakbot.listener.Listener;
import oakbot.listener.MentionListener;
import oakbot.listener.MornListener;
import oakbot.listener.WaveListener;
import oakbot.listener.WelcomeListener;
import oakbot.listener.chatgpt.ChatGPT;
import oakbot.listener.chatgpt.ImagineCommand;
import oakbot.listener.chatgpt.OpenAIClient;
import oakbot.task.FOTD;
import oakbot.task.LinuxHealthMonitor;
import oakbot.task.QOTD;
import oakbot.task.ScheduledTask;
import oakbot.task.XkcdExplained;
import oakbot.util.ChatBuilder;

/**
 * Regenerates the wiki page that lists all of OakBot's commands. Remember that
 * you must manually update this class whenever you add or remove commands from
 * the bot.
 * @see "https://github.com/JavaChat/OakBot/wiki/Commands"
 * @author Michael Angstadt
 */
public class CommandsWikiPage {
	public static void main(String args[]) {
		Database db = mock(Database.class);
		String trigger = "/";
		LearnedCommandsDao learnedCommands = new LearnedCommandsDao();

		List<ScheduledTask> tasks = new ArrayList<>();
		{
			tasks.add(new FOTD());
			tasks.add(new LinuxHealthMonitor(Collections.emptyList(), trigger));
			tasks.add(new QOTD());

			tasks.removeIf(t -> t.name() == null);
			tasks.sort(Comparator.comparing(ScheduledTask::name));
		}

		List<Listener> listeners = new ArrayList<>();
		{
			listeners.add(new MentionListener());
			listeners.add(new ChatGPT(new OpenAIClient(""), "", 0, "PT0S", 10, 0, Collections.emptyList()));
			listeners.add(new MornListener("PT1S", null));
			listeners.add(new WaveListener("PT1S", null));
			listeners.add(new WelcomeListener(db, 1000, Collections.emptyMap()));
			listeners.add(new XkcdExplained("PT0S"));

			listeners.removeIf(l -> l.name() == null);
			listeners.sort(Comparator.comparing(Listener::name));
		}

		List<Command> commands = new ArrayList<>();
		{
			commands.add(new AbbreviationCommand(null));
			commands.add(new AboutCommand(null, null));
			commands.add(new AdventOfCode("PT0S", Collections.emptyMap(), null));
			commands.add(new AfkCommand());
			commands.add(new CatCommand(null));
			commands.add(new ConvertCommand(null));
			commands.add(new DeleteCommand());
			commands.add(new DefineCommand(null));
			commands.add(new EffectiveDebuggingCommand());
			commands.add(new EffectiveJavaCommand());
			commands.add(new EightBallCommand());
			commands.add(new ExplainCommand(null));
			commands.add(new FacepalmCommand(""));
			commands.add(new FatCatCommand(db));
			commands.add(new GrammarCommand(null));
			commands.add(new GrootFilter());
			commands.add(new HelpCommand(commands, learnedCommands, listeners, tasks, ""));
			commands.add(new HttpCommand());
			commands.add(new ImagineCommand(new OpenAIClient(""), 1));
			commands.add(new JavadocCommand(null));
			commands.add(new JuiceBoxCommand());
			commands.add(new LearnCommand(commands, learnedCommands));
			commands.add(new ReactCommand(null));
			commands.add(new RemindCommand());
			commands.add(new RollCommand());
			commands.add(new ShrugCommand());
			commands.add(new ShutdownCommand());
			commands.add(new SummonCommand(2));
			commands.add(new TagCommand());
			commands.add(new UnlearnCommand(commands, learnedCommands));
			commands.add(new UnsummonCommand());
			commands.add(new UpsidedownTextFilter());
			commands.add(new UrbanCommand());
			commands.add(new WaduFilter());
			commands.add(new WikiCommand());

			commands.sort(Comparator.comparing(Command::name));
		}

		ChatBuilder cb = new ChatBuilder();

		cb.append("This page lists all of OakBot's commands and listeners.");
		cb.nl().nl().append("Type ").code("/help COMMAND").append(" to see this help documentation in chat.");

		cb.nl().nl().append("# Commands");
		if (commands.isEmpty()) {
			cb.nl().nl().italic("no commands defined");
		} else {
			for (Command command : commands) {
				cb.nl().nl().append("## ").append(trigger).append(command.name()).nl().nl();

				HelpDoc help = command.help();
				if (help.isIncludeSummaryWithDetail()) {
					cb.append(help.getSummary());
				}
				if (help.getDetail() != null) {
					cb.append(" ").append(help.getDetail());
				}

				List<String[]> examples = help.getExamples();
				if (!examples.isEmpty()) {
					cb.nl().nl().bold("Examples:").nl();

					for (String[] example : examples) {
						String parameters = example[0];
						String description = example[1];

						cb.nl().append(" * ").code().append(trigger).append(command.name());
						if (!parameters.isEmpty()) {
							cb.append(" ").append(parameters);
						}
						cb.code();
						if (!description.isEmpty()) {
							cb.append(" - ").append(description);
						}
					}
				}

				Collection<String> aliases = command.aliases();
				if (!aliases.isEmpty()) {
					cb.nl().nl().bold("Aliases:").append(" ");

					boolean first = true;
					for (String alias : aliases) {
						if (first) {
							first = false;
						} else {
							cb.append(", ");
						}

						cb.code(alias);
					}
				}
			}
		}

		cb.nl().nl().append("# Listeners");
		if (listeners.isEmpty()) {
			cb.nl().nl().italic("no listeners defined");
		} else {
			for (Listener listener : listeners) {
				HelpDoc help = listener.help();

				cb.nl().nl().append("## ").append(listener.name()).nl().nl();
				if (help.isIncludeSummaryWithDetail()) {
					cb.append(help.getSummary());
				}
				if (help.getDetail() != null) {
					cb.append(" ").append(help.getDetail());
				}
			}
		}

		cb.nl().nl().append("# Tasks");
		if (tasks.isEmpty()) {
			cb.nl().nl().italic("no tasks defined");
		} else {
			for (ScheduledTask task : tasks) {
				HelpDoc help = task.help();

				cb.nl().nl().append("## ").append(task.name()).nl().nl();
				if (help.isIncludeSummaryWithDetail()) {
					cb.append(help.getSummary());
				}
				if (help.getDetail() != null) {
					cb.append(" ").append(help.getDetail());
				}
			}
		}

		System.out.println(cb);
	}
}
