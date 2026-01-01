package oakbot.bot;

import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import oakbot.Database;
import oakbot.ai.openai.OpenAIClient;
import oakbot.ai.stabilityai.StabilityAIClient;
import oakbot.command.AboutCommand;
import oakbot.command.AfkCommand;
import oakbot.command.CatCommand;
import oakbot.command.CoffeeCommand;
import oakbot.command.Command;
import oakbot.command.DeleteCommand;
import oakbot.command.DogCommand;
import oakbot.command.EightBallCommand;
import oakbot.command.FacepalmCommand;
import oakbot.command.FatCatCommand;
import oakbot.command.FishCommand;
import oakbot.command.HelpCommand;
import oakbot.command.JuiceBoxCommand;
import oakbot.command.PhishCommand;
import oakbot.command.ReactGiphyCommand;
import oakbot.command.RemindCommand;
import oakbot.command.RollCommand;
import oakbot.command.ShrugCommand;
import oakbot.command.ShutdownCommand;
import oakbot.command.SummonCommand;
import oakbot.command.TagCommand;
import oakbot.command.TimeoutCommand;
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
import oakbot.command.stands4.RhymeCommand;
import oakbot.command.urban.UrbanCommand;
import oakbot.filter.GrootFilter;
import oakbot.filter.UpsidedownTextFilter;
import oakbot.filter.WaduFilter;
import oakbot.listener.DadJokeListener;
import oakbot.listener.Listener;
import oakbot.listener.MentionListener;
import oakbot.listener.MornListener;
import oakbot.listener.UnitConversionListener;
import oakbot.listener.WaveListener;
import oakbot.listener.WelcomeListener;
import oakbot.listener.chatgpt.ChatGPT;
import oakbot.listener.chatgpt.ImagineCommand;
import oakbot.listener.chatgpt.ImagineCore;
import oakbot.listener.chatgpt.ImagineExactCommand;
import oakbot.listener.chatgpt.MoodCommand;
import oakbot.listener.chatgpt.QuotaCommand;
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
		var db = mock(Database.class);
		var trigger = "/";
		var learnedCommands = new LearnedCommandsDao();

		var tasks = new ArrayList<ScheduledTask>();
		{
			tasks.add(new FOTD());
			tasks.add(new LinuxHealthMonitor(List.of(), trigger));
			tasks.add(new QOTD());

			tasks.removeIf(t -> t.name() == null);
			tasks.sort(Comparator.comparing(ScheduledTask::name));
		}

		var listeners = new ArrayList<Listener>();
		{
			listeners.add(new MentionListener());
			listeners.add(new DadJokeListener("Oak", null));
			listeners.add(new ChatGPT(new OpenAIClient(""), null, "", "", Map.of(), 0, null, Duration.ZERO, 10, 0, 0));
			listeners.add(new MornListener(Duration.ZERO, null));
			listeners.add(new UnitConversionListener());
			listeners.add(new WaveListener(Duration.ZERO, null));
			listeners.add(new WelcomeListener(db, 1000, Map.of()));
			listeners.add(new XkcdExplained(Duration.ZERO));

			listeners.removeIf(l -> l.name() == null);
			listeners.sort(Comparator.comparing(Listener::name));
		}

		var commands = new ArrayList<Command>();
		{
			commands.add(new AbbreviationCommand(null));
			commands.add(new AboutCommand(null));
			commands.add(new AdventOfCode(null, Duration.ZERO, Map.of()));
			commands.add(new AfkCommand());
			commands.add(new CatCommand(null));
			commands.add(new CoffeeCommand());
			commands.add(new ConvertCommand(null));
			commands.add(new DeleteCommand(null));
			commands.add(new DefineCommand(null));
			commands.add(new DogCommand(null));
			commands.add(new EffectiveDebuggingCommand());
			commands.add(new EffectiveJavaCommand());
			commands.add(new EightBallCommand());
			commands.add(new ExplainCommand(null));
			commands.add(new FacepalmCommand(""));
			commands.add(new FatCatCommand(db));
			commands.add(new FishCommand(db, Duration.ZERO, Duration.ZERO, Duration.ZERO));
			commands.add(new GrammarCommand(null));
			commands.add(new GrootFilter());
			commands.add(new HelpCommand(commands, learnedCommands, listeners, tasks, ""));
			commands.add(new HttpCommand());
			commands.add(new ImagineCommand(new ImagineCore(new OpenAIClient(""), new StabilityAIClient(""), 1)));
			commands.add(new ImagineExactCommand(new ImagineCore(new OpenAIClient(""), new StabilityAIClient(""), 1)));
			commands.add(new JavadocCommand(null));
			commands.add(new JuiceBoxCommand());
			commands.add(new LearnCommand(commands, learnedCommands));
			commands.add(new MoodCommand(db, null));
			commands.add(new PhishCommand(db, Duration.ZERO, Duration.ZERO));
			commands.add(new QuotaCommand(null, null));
			commands.add(new ReactGiphyCommand(null));
			commands.add(new RemindCommand());
			commands.add(new RhymeCommand(null));
			commands.add(new RollCommand());
			commands.add(new ShrugCommand());
			commands.add(new ShutdownCommand());
			commands.add(new SummonCommand());
			commands.add(new TagCommand());
			commands.add(new TimeoutCommand());
			commands.add(new UnlearnCommand(commands, learnedCommands));
			commands.add(new UnsummonCommand());
			commands.add(new UpsidedownTextFilter());
			commands.add(new UrbanCommand());
			commands.add(new WaduFilter());
			commands.add(new WikiCommand());

			commands.sort(Comparator.comparing(Command::name));
		}

		var cb = new ChatBuilder();

		cb.append("This page lists all of OakBot's commands and listeners.");
		cb.nl().nl().append("Type ").code("/help COMMAND").append(" to see this help documentation in chat.");

		cb.nl().nl().append("# Commands");
		if (commands.isEmpty()) {
			cb.nl().nl().italic("no commands defined");
		} else {
			for (var command : commands) {
				cb.nl().nl().append("## ").append(trigger).append(command.name()).nl().nl();

				var help = command.help();
				if (help.isIncludeSummaryWithDetail()) {
					cb.append(escape(help.getSummary()));
				}
				if (help.getDetail() != null) {
					cb.append(" ").append(escape(help.getDetail()));
				}

				var examples = help.getExamples();
				if (!examples.isEmpty()) {
					cb.nl().nl().bold("Examples:").nl();

					for (var example : examples) {
						cb.nl().append(" * ").code().append(trigger).append(command.name());
						if (!example.parameters().isEmpty()) {
							cb.append(" ").append(escape(example.parameters()));
						}
						cb.code();
						if (!example.description().isEmpty()) {
							cb.append(" - ").append(escape(example.description()));
						}
					}
				}

				var aliases = command.aliases();
				if (!aliases.isEmpty()) {
					cb.nl().nl().bold("Aliases:").append(" ");

					var first = true;
					for (var alias : aliases) {
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
			for (var listener : listeners) {
				var help = listener.help();

				cb.nl().nl().append("## ").append(listener.name()).nl().nl();
				if (help.isIncludeSummaryWithDetail()) {
					cb.append(escape(help.getSummary()));
				}
				if (help.getDetail() != null) {
					cb.append(" ").append(escape(help.getDetail()));
				}
			}
		}

		cb.nl().nl().append("# Tasks");
		if (tasks.isEmpty()) {
			cb.nl().nl().italic("no tasks defined");
		} else {
			for (var task : tasks) {
				var help = task.help();

				cb.nl().nl().append("## ").append(task.name()).nl().nl();
				if (help.isIncludeSummaryWithDetail()) {
					cb.append(escape(help.getSummary()));
				}
				if (help.getDetail() != null) {
					cb.append(" ").append(escape(help.getDetail()));
				}
			}
		}

		System.out.println(cb);
	}

	private static String escape(String s) {
		return s.replace("<", "&lt;").replace(">", "&gt;");
	}
}
