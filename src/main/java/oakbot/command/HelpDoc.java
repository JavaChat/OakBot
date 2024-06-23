package oakbot.command;

import static oakbot.util.StringUtils.plural;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import oakbot.listener.Listener;
import oakbot.task.ScheduledTask;
import oakbot.util.ChatBuilder;

/**
 * Contains all of the help documentation for a command or listener.
 * @author Michael Angstadt
 */
public class HelpDoc {
	protected final String name;
	protected final Collection<String> aliases;
	protected final String summary;
	protected final String detail;
	protected final boolean includeSummaryWithDetail;
	protected final List<Example> examples;

	protected HelpDoc(Builder builder) {
		name = builder.name;
		aliases = builder.aliases;
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
	public List<Example> getExamples() {
		return examples;
	}

	/**
	 * Builds the help message to post when the user requests more detail about
	 * the command/listener.
	 * @param trigger the bot's command trigger (only applicable for commands)
	 * @return the help message
	 */
	public String getHelpText(String trigger) {
		var cb = new ChatBuilder();
		cb.append(name).append(':').nl();

		if (includeSummaryWithDetail) {
			cb.append(summary);
		}
		if (detail != null) {
			cb.append(' ').append(detail);
		}

		if (!examples.isEmpty()) {
			cb.nl().nl().append(plural("Example", examples.size())).append(":");
			for (var example : examples) {
				cb.nl().append(trigger).append(name);
				if (!example.parameters().isEmpty()) {
					cb.append(' ').append(example.parameters());
				}
				if (!example.description().isEmpty()) {
					cb.append(" : ").append(example.description());
				}
			}
		}

		if (!aliases.isEmpty()) {
			cb.nl().nl().append(plural("Alias", aliases.size())).append(": ").append(String.join(",", aliases));
		}

		return cb.toString();
	}

	/**
	 * Creates instances of {@link HelpDoc}.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private String name;
		private Collection<String> aliases;
		private String summary;
		private String detail;
		private boolean includeSummaryWithDetail = true;
		private final List<Example> examples = new ArrayList<>();

		/**
		 * @param command the command this documentation is for
		 */
		public Builder(Command command) {
			this(command.name(), command.aliases());
		}

		/**
		 * @param listener the listener this documentation is for
		 */
		public Builder(Listener listener) {
			this(listener.name(), List.of());
		}

		/**
		 * @param task the task this documentation is for
		 */
		public Builder(ScheduledTask task) {
			this(task.name(), List.of());
		}

		public Builder(String name, Collection<String> aliases) {
			this.name = name;
			this.aliases = aliases;
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
		 * This will be prefixed by whatever was passed into
		 * {@link #summary(String)}* (unless {@code false} was passed into
		 * {@link #includeSummaryWithDetail(boolean)}).
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
			examples.add(new Example(parameters, description));
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

	public record Example(String parameters, String description) {
	}
}
