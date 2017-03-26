package oakbot;

import java.nio.file.Path;
import java.nio.file.Paths;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Parses the command-line arguments.
 * @author Michael Angstadt
 */
public class CliArguments {
	private final OptionSet options;

	public CliArguments(String[] args) {
		OptionParser parser = new OptionParser();
		parser.accepts("settings").withRequiredArg();
		parser.accepts("db").withRequiredArg();
		parser.accepts("quiet");
		parser.accepts("version");
		parser.accepts("help");

		options = parser.parse(args);
	}

	public Path settings() {
		return path("settings");
	}

	public Path db() {
		return path("db");
	}

	public boolean version() {
		return options.has("version");
	}

	public boolean help() {
		return options.has("help");
	}

	public boolean quiet() {
		return options.has("quiet");
	}

	private Path path(String name) {
		String value = (String) options.valueOf(name);
		return (value == null) ? null : Paths.get(value);
	}

	public String printHelp(Path defaultSettings, Path defaultDb) {
		final String nl = System.getProperty("line.separator");

		//@formatter:off
		return
		"OakBot v" + Main.VERSION + nl +
		"by Michael Angstadt" + nl +
		Main.URL + nl +
		nl +
		"Arguments" + nl +
		"================================================" + nl +
		"--settings=PATH" + nl +
		"  The properties file that contains the bot's configuration settings, such as " + nl +
		"  login credentials (defaults to \"" + defaultSettings + "\")." + nl +
		nl +
		"--db=PATH" + nl +
		"  The path to a JSON file for storing all persistant data." + nl +
		"  (defaults to \"" + defaultDb + "\")." + nl +
		nl +
		"--quiet" + nl +
		"  If specified, the bot will not output a greeting message when it starts up." + nl +
		nl +
		"--version" + nl +
		"  Prints the version of this program." + nl +
		nl +
		"--help" + nl +
		"  Prints this help message.";
		//@formatter:on
	}
}
