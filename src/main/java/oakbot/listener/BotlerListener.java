package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.post;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.Now;

/**
 * Allows users to make the bot send commands to Botler, a bot by Captain
 * Obvious. Also, responds to Botler's startup message.
 * @author Michael Angstadt
 * @see "https://stackoverflow.com/users/13750349/botler"
 * @see "https://github.com/butler1233/stackoverflow-chatbot"
 */
public class BotlerListener implements Command, Listener {
	private final String botlerTrigger;
	private final int botlerUserId;
	private final String botlerStartupMessage;
	private final String response;
	private final Duration timeBetweenResponses;
	private final Map<Integer, Instant> responseTimesPerRoom = new HashMap<>();

	/**
	 * @param botlerTrigger Botler's trigger
	 * @param botlerUserId Botler's user ID
	 * @param botlerStartupMessage the Botler message to respond to
	 * @param response the response
	 * @param timeBetweenResponses cooldown time between responses
	 */
	public BotlerListener(String botlerTrigger, int botlerUserId, String botlerStartupMessage, String response, Duration timeBetweenResponses) {
		this.botlerTrigger = botlerTrigger;
		this.botlerUserId = botlerUserId;
		this.botlerStartupMessage = botlerStartupMessage;
		this.response = response;
		this.timeBetweenResponses = timeBetweenResponses;
	}

	@Override
	public String name() {
		return "botler";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((Command)this)
			.summary("Makes Oak send commands to Botler, a bot by Captain Obvious (be nice).")
			.detail("Also, responds to Botler's startup message. Botler's GitHub: https://github.com/butler1233/stackoverflow-chatbot")
			.example("help", "Sends the help command to Botler.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContentMarkdown();
		if (content.isEmpty()) {
			return doNothing();
		}

		return post(botlerTrigger + " " + content);
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		if (message.userId() != botlerUserId) {
			return doNothing();
		}

		if (!botlerStartupMessage.equals(message.content().getContent())) {
			return doNothing();
		}

		var now = Now.instant();
		var roomId = message.roomId();
		var lastResponse = responseTimesPerRoom.get(roomId);

		if (lastResponse != null) {
			var sinceLastResponse = Duration.between(lastResponse, now);
			if (sinceLastResponse.compareTo(timeBetweenResponses) < 0) {
				return doNothing();
			}
		}

		responseTimesPerRoom.put(roomId, now);
		return post(response);
	}
}
