package oakbot.listener.chatgpt;

import static java.util.function.Predicate.not;
import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.impl.client.CloseableHttpClient;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.Content;
import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.HelpDoc;
import oakbot.listener.CatchAllMentionListener;
import oakbot.task.ScheduledTask;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;

/**
 * Periodically comment on the chat room's latest messages using ChatGPT. Posts
 * a response when a user mentions the bot in chat.
 * @author Michael Angstadt
 */
public class ChatGPT implements ScheduledTask, CatchAllMentionListener {
	private static final Logger logger = Logger.getLogger(ChatGPT.class.getName());

	private final OpenAIClient openAIClient;
	private final String prompt;
	private final Duration timeBetweenSpontaneousPosts;
	private final int completionMaxTokens, numLatestMessagesToIncludeInRequest, latestMessageCharacterLimit;
	private final Map<Integer, Instant> spontaneousPostTimes = new HashMap<>();
	private boolean ignoreNextMessage;
	private boolean firstRun = true;

	/**
	 * @param openAIClient the OpenAI client
	 * @param prompt one or more sentences that define the bot's personality
	 * (e.g. "You are a helpful assistant"). This counts against your usage
	 * quota. Each word costs around 1.33 tokens.
	 * @param completionMaxTokens places a limit on the length of ChatGPT's
	 * completion (response). If this number is too short, then the completion
	 * may end abruptly (e.g. in an unfinished sentence). Each word costs
	 * around 1.33 tokens.
	 * @param timeBetweenSpontaneousPosts the amount of time to wait before
	 * posting a message (duration string)
	 * @param numLatestMessagesToIncludeInRequest the number of chat room
	 * messages to include in the ChatGPT request to give the bot context of the
	 * conversation (each message counts against the usage quota)
	 * @param latestMessageCharacterLimit each chat message that is sent to
	 * ChatGPT will not exceed this number of characters (includes markdown
	 * syntax). Chat messages that do will be truncated (without cutting off
	 * words). 0 to disable truncation. Each message counts against the usage
	 * quota. Each word costs around 1.33 tokens.
	 */
	public ChatGPT(OpenAIClient openAIClient, String prompt, int completionMaxTokens, String timeBetweenSpontaneousPosts, int numLatestMessagesToIncludeInRequest, int latestMessageCharacterLimit) {
		this.openAIClient = openAIClient;
		this.prompt = prompt;
		this.completionMaxTokens = completionMaxTokens;
		this.timeBetweenSpontaneousPosts = Duration.parse(timeBetweenSpontaneousPosts);
		this.numLatestMessagesToIncludeInRequest = numLatestMessagesToIncludeInRequest;
		this.latestMessageCharacterLimit = latestMessageCharacterLimit;
	}

	@Override
	public long nextRun() {
		if (spontaneousPostTimes.isEmpty()) {
			if (firstRun) {
				firstRun = false;
				return 1;
			}

			return timeBetweenSpontaneousPosts.toMillis();
		}

		Instant now = Instant.now();

		//@formatter:off
		long lowest = spontaneousPostTimes.values().stream()
			.map(runTime -> Duration.between(now, runTime))
			.mapToLong(Duration::toMillis)
		.min().getAsLong();
		//@formatter:on

		return (lowest < 1) ? 1 : lowest;
	}

	@Override
	public void run(IBot bot) throws Exception {
		removeRoomsBotIsNotIn(bot);
		startTimerForNewRooms(bot);
		List<Integer> roomsToPostTo = findRoomsToSpontaneouslyPostTo();

		for (Integer roomId : roomsToPostTo) {
			resetSpontaneousPostTimer(roomId);

			/*
			 * Do not post anything if the room has no messages.
			 */
			List<ChatMessage> messages = bot.getLatestMessages(roomId, numLatestMessagesToIncludeInRequest);
			if (messages.isEmpty()) {
				continue;
			}

			/*
			 * Do not post anything if the latest message was not authored by a
			 * human.
			 */
			ChatMessage latestMessage = messages.get(messages.size() - 1);
			boolean latestMsgPostedBySystemBot = (latestMessage.getUserId() < 1);
			if (latestMsgPostedBySystemBot) {
				continue;
			}
			boolean latestMsgPostedByThisBot = (latestMessage.getUserId() == bot.getUserId());
			if (latestMsgPostedByThisBot) {
				continue;
			}

			ChatCompletionRequest request = buildChatCompletionRequest(messages, bot);
			String response = sendChatCompletionRequest(request);

			PostMessage postMessage = new PostMessage(response).splitStrategy(SplitStrategy.WORD);
			bot.sendMessage(roomId, postMessage);
		}
	}

	@Override
	public String name() {
		return "chatgpt";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((CatchAllMentionListener)this)
			.summary("Allows the user to have a conversation with ChatGPT.")
			.detail("Mentioning the bot will invoke a response from ChatGPT. The last " + numLatestMessagesToIncludeInRequest + " messages in the chat room are sent to the ChatGPT servers for context. Under certain conditions, the bot will also periodically post a message from ChatGPT on its own.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		/*
		 * Ignore this message if another listener already responded to it.
		 */
		if (ignoreNextMessage) {
			ignoreNextMessage = false;
			return doNothing();
		}

		/*
		 * Ignore this message if the bot isn't mentioned.
		 */
		if (!message.getContent().isMentioned(bot.getUsername())) {
			return doNothing();
		}

		try {
			List<ChatMessage> prevMessages = bot.getLatestMessages(message.getRoomId(), numLatestMessagesToIncludeInRequest);

			ChatCompletionRequest request = buildChatCompletionRequest(prevMessages, bot);
			String response = sendChatCompletionRequest(request);

			resetSpontaneousPostTimer(message.getRoomId());

			return reply(response, message);
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem communicating with ChatGPT.");
			return reply("I don't really feel like talking right now.", message);
		}
	}

	@Override
	public void ignoreNextMessage() {
		ignoreNextMessage = true;
	}

	private void removeRoomsBotIsNotIn(IBot bot) {
		List<Integer> roomsBotIsIn = bot.getRooms();
		spontaneousPostTimes.keySet().removeIf(not(roomsBotIsIn::contains));
	}

	private void startTimerForNewRooms(IBot bot) {
		//@formatter:off
		bot.getRooms().stream()
			.filter(not(spontaneousPostTimes::containsKey))
		.forEach(this::resetSpontaneousPostTimer);
		//@formatter:on
	}

	private List<Integer> findRoomsToSpontaneouslyPostTo() {
		List<Integer> roomIds = new ArrayList<>();

		for (Map.Entry<Integer, Instant> entry : spontaneousPostTimes.entrySet()) {
			Integer roomId = entry.getKey();
			Instant runTime = entry.getValue();

			if (runTime.isAfter(Instant.now())) {
				continue;
			}

			roomIds.add(roomId);
		}

		return roomIds;
	}

	private ChatCompletionRequest buildChatCompletionRequest(List<ChatMessage> messages, IBot bot) {
		ChatCompletionRequest request = new ChatCompletionRequest(prompt);
		request.setMaxTokensForCompletion(completionMaxTokens);

		for (ChatMessage message : messages) {
			Content content = message.getContent();

			boolean messageWasDeleted = (content == null);
			if (messageWasDeleted) {
				continue;
			}

			String contentStr = content.getContent();
			boolean fixedWidthFont = content.isFixedWidthFont();
			String contentMd = ChatBuilder.toMarkdown(contentStr, fixedWidthFont);

			String truncatedContentMd;
			if (latestMessageCharacterLimit > 0) {
				truncatedContentMd = SplitStrategy.WORD.split(contentMd, latestMessageCharacterLimit).get(0);
			} else {
				truncatedContentMd = contentMd;
			}

			boolean messagePostedByOak = (message.getUserId() == bot.getUserId());
			if (messagePostedByOak) {
				request.addBotMessage(truncatedContentMd);
			} else {
				request.addHumanMessage(truncatedContentMd);
			}
		}

		return request;
	}

	private String sendChatCompletionRequest(ChatCompletionRequest request) throws IOException {
		try (CloseableHttpClient client = HttpFactory.connect().getClient()) {
			String response = openAIClient.chatCompletion(request);
			response = removeMentionsFromBeginningOfMessage(response);
			return formatMessagesWithCodeBlocks(response);
		} catch (OpenAIException e) {
			//@formatter:off
			return new ChatBuilder()
				.code()
				.append("ERROR BEEP BOOP: ")
				.append(e.getMessage())
				.code()
			.toString();
			//@formatter:on
		}
	}

	/**
	 * <p>
	 * Remove all mentions from the beginning of the message.
	 * </p>
	 * <p>
	 * ChatGPT will sometimes put mentions at the beginning of the response,
	 * probably because the chat messages that we send to it have mentions in
	 * them. These mentions duplicate and build up over time for some reason, so
	 * the first response might have one mention, the next response might have
	 * two, and so on.
	 * </p>
	 * @param message
	 * @return the message without mentions at the beginning
	 */
	private String removeMentionsFromBeginningOfMessage(String message) {
		String orig = message;
		while (true) {
			String result = orig.replaceAll("^@.+?\\s+", "");
			if (result.equals(orig)) {
				return result;
			}
			orig = result;
		}
	}

	/**
	 * <p>
	 * Converts responses that contain code samples to fixed-width font, and
	 * removes the code block markdown syntax.
	 * </p>
	 * <p>
	 * SO Chat does not support code block markdown syntax. The entire message
	 * is converted to fixed-width font because SO Chat does not support
	 * formatting part of a message in fixed-width, only the entire message.
	 * </p>
	 * @param message the message
	 * @return the reformatted message
	 */
	private String formatMessagesWithCodeBlocks(String message) {
		if (!message.contains("```")) {
			return message;
		}

		//@formatter:off
		return "    " + message
			.replaceAll("(?m)^```[^\\n]++\\n", "")
			.replaceAll("(?m)^```\\n?", "")
			.replace("\n", "\n    ");
		//@formatter:on
	}

	/**
	 * Resets the chat timer for the given room.
	 * @param roomId the room ID
	 */
	private void resetSpontaneousPostTimer(int roomId) {
		Instant nextRunTime = Instant.now().plus(timeBetweenSpontaneousPosts);
		spontaneousPostTimes.put(roomId, nextRunTime);
	}
}
