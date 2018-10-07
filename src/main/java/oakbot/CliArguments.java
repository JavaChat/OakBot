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
		parser.accepts("mock");
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

	public boolean mock() {
		return options.has("mock");
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
		"--mock" + nl +
		"  Runs the bot using a mock chat connection for testing purposes." + nl +
		"  A text file will be created in the root of the project for each chat room the" + nl +
		"  bot is configured to connect to. These files are used to \"send\" messages" + nl +
		"  to the mock chat rooms. To send a message, type your message into the text" + nl +
		"  file and save it." + nl +
		"  Messages are entered one per line. Multi-line messages can be entered by" + nl +
		"  ending each line with a backslash until you reach the last line. You should" + nl +
		"  only append onto the end of the file; do not delete anything. These files are" + nl +
		"  re-created every time the program runs." + nl +
		"  All messages that are sent to the mock chat room are displayed in stdout (this" + nl +
		"  includes your messages and the bot's responses)." + nl +
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
