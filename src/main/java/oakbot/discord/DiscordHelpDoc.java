package oakbot.discord;

import static oakbot.util.StringUtils.plural;

import java.util.List;

import oakbot.util.ChatBuilder;

/**
 * Contains all of the help documentation for a command or listener.
 * @author Michael Angstadt
 */
public class DiscordHelpDoc extends oakbot.command.HelpDoc {
	private DiscordHelpDoc(Builder builder) {
		super(builder);
	}

	/**
	 * Builds the help message to post when the user requests more detail about
	 * the command/listener.
	 * @param trigger the bot's command trigger (only applicable for commands)
	 * @return the help message
	 */
	@Override
	public String getHelpText(String trigger) {
		var cb = new ChatBuilder();
		cb.code().append(trigger).append(name).code().nl();

		if (includeSummaryWithDetail) {
			cb.append(summary);
		}
		if (detail != null) {
			cb.append(' ').append(detail);
		}

		if (!examples.isEmpty()) {
			cb.nl().nl().bold(plural("Example", examples.size())).append(":");
			for (var example : examples) {
				cb.nl().code().append(trigger).append(name);
				if (!example.parameters().isEmpty()) {
					cb.append(' ').append(example.parameters());
				}
				cb.code();
				if (!example.description().isEmpty()) {
					cb.append("  ").append(example.description());
				}
			}
		}

		if (!aliases.isEmpty()) {
			cb.nl().nl().bold(plural("Alias", aliases.size())).append(": ").append(String.join(",", aliases));
		}

		return cb.toString();
	}

	/**
	 * Creates instances of {@link DiscordHelpDoc}.
	 * @author Michael Angstadt
	 */
	public static class Builder extends oakbot.command.HelpDoc.Builder {
		/**
		 * @param command the command this documentation is for
		 */
		public Builder(DiscordCommand command) {
			super(command.name(), List.of());
		}

		/**
		 * @param listener the listener this documentation is for
		 */
		public Builder(DiscordListener listener) {
			super(listener.name(), List.of());
		}

		/**
		 * Builds the {@link DiscordHelpDoc} instance.
		 * @return the instance
		 */
		@Override
		public DiscordHelpDoc build() {
			return new DiscordHelpDoc(this);
		}
	}
}
