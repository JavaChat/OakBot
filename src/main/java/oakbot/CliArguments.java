package oakbot;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Parses the command-line arguments.
 * @author Michael Angstadt
 */
public class CliArguments {
	private final OptionSet options;

	public CliArguments(String[] args) {
		var parser = new OptionParser();
		parser.accepts("context").withRequiredArg();
		parser.accepts("mock");
		parser.accepts("quiet");
		parser.accepts("version");
		parser.accepts("help");

		options = parser.parse(args);
	}

	public String context() {
		return (String) options.valueOf("context");
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

	public String printHelp(String defaultContext) {
		return """
		OakBot v%s
		by Michael Angstadt
		%s
		
		Arguments
		================================================
		--context=PATH
		  The path to the Spring application context XML file that contains the bot's
		  configuration settings and commands (defaults to "%s").
		  Note: Absolute paths must be prefixed with "file:".
		
		--mock
		  Runs the bot using a mock chat connection for testing purposes.
		  A text file will be created in the root of the project for each chat room the
		  bot is configured to connect to. These files are used to "send" messages
		  to the mock chat rooms. To send a message, type your message into the text
		  file and save it.
		  Messages are entered one per line. Multi-line messages can be entered by
		  ending each line with a backslash until you reach the last line. You should
		  only append onto the end of the file; do not delete anything. These files are
		  re-created every time the program runs.
		  All messages that are sent to the mock chat room are displayed in stdout (this
		  includes your messages and the bot's responses).
		
		--quiet
		  If specified, the bot will not output a greeting message when it starts up.
		
		--version
		  Prints the version of this program.
		
		--help
		  Prints this help message.""".formatted(Main.VERSION, Main.URL, defaultContext);
	}
}
