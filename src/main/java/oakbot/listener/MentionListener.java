package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Responds when a user mentions the bot's name.
 * @author Michael Angstadt
 */
public class MentionListener implements CatchAllMentionListener {
	private final Duration cooldownTimeBetweenResponses = Duration.ofMinutes(1);
	private final Map<Integer, Instant> timeOfLastResponseByRoom = new HashMap<>();

	private final Map<String, String> responses;
	{
		String response = "You're welcome.";

		//@formatter:off
		responses = Map.of(
			"thank you", response,
			"thank u", response,
			"thanks", response,
			"thx", response,
			"ty", response
		);
		//@formatter:on
	}

	private boolean ignore = false;

	@Override
	public String name() {
		return "mention";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Sends a reply message when someone mentions the bot's name.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		if (ignore) {
			ignore = false;
			return doNothing();
		}

		if (!message.getContent().isMentioned(bot.getUsername())) {
			return doNothing();
		}

		Instant prevResponse = timeOfLastResponseByRoom.get(message.getRoomId());
		Instant now = Instant.now();
		if (prevResponse != null) {
			Duration elapsed = Duration.between(prevResponse, now);
			if (elapsed.compareTo(cooldownTimeBetweenResponses) < 0) {
				return doNothing();
			}
		}

		timeOfLastResponseByRoom.put(message.getRoomId(), now);

		String response = respond(message.getContent().getContent());
		if (response != null) {
			return reply(response, message);
		}

		//@formatter:off
		return reply(new ChatBuilder()
			.append("Type ").code().append(bot.getTrigger()).append("help").code().append(" to see all my commands."),
			message
		);
		//@formatter:on
	}

	@Override
	public void ignoreNextMessage() {
		ignore = true;
	}

	private String respond(String content) {
		String strippedContent = content //@formatter:off
		.replaceAll("@\\w+", "") //remove mentions
		.replaceAll("[^a-zA-Z ]", "") //remove everything but letters and spaces
		.replaceAll("\\s{2,}", " ") //remove duplicate spaces
		.trim()
		.toLowerCase(); //@formatter:on

		return responses.get(strippedContent);
	}
}
