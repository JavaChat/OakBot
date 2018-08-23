package oakbot.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;

/**
 * Contains all of the help documentation for a command or listener.
 * @author Michael Angstadt
 */
public class HelpDoc {
	private final Command command;
	private final Listener listener;
	private final String summary, detail;
	private final boolean includeSummaryWithDetail;
	private final List<String[]> examples;

	private HelpDoc(Builder builder) {
		command = builder.command;
		listener = builder.listener;
		summary = Objects.requireNonNull(builder.summary);
		detail = builder.detail;
		includeSummaryWithDetail = builder.includeSummaryWithDetail;
		examples = builder.examples;
	}

	/**
	 * Gets the short summary of the command/listener.
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * Gets the detailed description of the command/listener.
	 * @return the detailed description
	 */
	public String getDetail() {
		return detail;
	}

	/**
	 * Gets whether or not to include the summary when the user requests the
	 * full help documentation of the command/listener.
	 * @return true to include the summary, false not to
	 */
	public boolean isIncludeSummaryWithDetail() {
		return includeSummaryWithDetail;
	}

	/**
	 * Gets the examples for how the command can be invoked (only applicable for
	 * commands).
	 * @return the examples (index 0 = command parameters, index 1 =
	 * description)
	 */
	public List<String[]> getExamples() {
		return examples;
	}

	/**
	 * Builds the help message to post when the user requests more detail about
	 * the command/listener.
	 * @param trigger the bot's command trigger (only applicable for commands)
	 * @return the help message
	 */
	public String getHelpText(String trigger) {
		if (command != null) {
			ChatBuilder cb = new ChatBuilder();
			cb.append(command.name()).append(':').nl();

			if (includeSummaryWithDetail) {
				cb.append(summary);
			}
			if (detail != null) {
				cb.append(' ').append(detail);
			}

			if (!examples.isEmpty()) {
				cb.nl().nl().append(plural("Example", examples.size())).append(":");
				for (String[] example : examples) {
					String parameters = example[0];
					String description = example[1];

					cb.nl().append(trigger).append(command.name());
					if (!parameters.isEmpty()) {
						cb.append(' ').append(parameters);
					}
					if (!description.isEmpty()) {
						cb.append(" : ").append(description);
					}
				}
			}

			Collection<String> aliases = command.aliases();
			if (!aliases.isEmpty()) {
				cb.nl().nl().append(plural("Alias", aliases.size())).append(": ").append(String.join(",", aliases));
			}

			return cb.toString();
		}

		if (listener != null) {
			ChatBuilder cb = new ChatBuilder();
			cb.append(listener.name()).append(':').nl();

			if (includeSummaryWithDetail) {
				cb.append(summary);
			}
			if (detail != null) {
				cb.append(' ').append(detail);
			}

			return cb.toString();
		}

		return null;
	}

	/**
	 * Determines if a word should be plural.
	 * @param word the singular version of the word
	 * @param number the number
	 * @return the plural or singular version of the word, depending on the
	 * provided number
	 */
	private static String plural(String word, int number) {
		if (number == 1) {
			return word;
		}

		return word + (word.endsWith("s") ? "es" : "s");
	}

	/**
	 * Creates instances of {@link HelpDoc}.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private Command command;
		private Listener listener;
		private String summary, detail;
		private boolean includeSummaryWithDetail = true;
		private final List<String[]> examples = new ArrayList<>();

		/**
		 * @param command the command this documentation is for
		 */
		public Builder(Command command) {
			this.command = command;
			listener = null;
		}

		/**
		 * @param listener the listener this documentation is for
		 */
		public Builder(Listener listener) {
			command = null;
			this.listener = listener;
		}

		/**
		 * A short, one sentence description of the command/listener.
		 * @param summary the summary (no Markdown allowed)
		 * @return this
		 */
		public Builder summary(String summary) {
			this.summary = summary;
			return this;
		}

		/**
		 * Any additional detail to include about the command/listener. This
		 * will be displayed when the user requests the full help documentation.
		 * This will be prefixed by whatever was passed into {@link #summary()}
		 * (unless {@code false} was passed into
		 * {@link includeSummaryWithDetail()}).
		 * @param detail the additional details (no Markdown allowed)
		 * @return this
		 */
		public Builder detail(String detail) {
			this.detail = detail;
			return this;
		}

		/**
		 * Sets whether or not to include the summary when the user requests the
		 * full help documentation of the command/listener.
		 * @param includeSummaryWithDetail true to include the summary (default)
		 * false not to
		 * @return this
		 */
		public Builder includeSummaryWithDetail(boolean includeSummaryWithDetail) {
			this.includeSummaryWithDetail = includeSummaryWithDetail;
			return this;
		}

		/**
		 * Adds an example for how the command can be invoked (only applicable
		 * for commands).
		 * @param parameters the command's parameters or empty string for no
		 * parameters
		 * @param description the description of this example (should be short,
		 * no Markdown allowed)
		 * @return this
		 */
		public Builder example(String parameters, String description) {
			examples.add(new String[] { parameters, description });
			return this;
		}

		/**
		 * Builds the {@link HelpDoc} instance.
		 * @return the instance
		 */
		public HelpDoc build() {
			return new HelpDoc(this);
		}
	}
}
