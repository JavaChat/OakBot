package oakbot.task;

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
import java.util.stream.Collectors;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.chat.ChatMessage;
import oakbot.chat.Content;
import oakbot.chat.SplitStrategy;
import oakbot.command.HelpDoc;
import oakbot.listener.CatchAllMentionListener;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;

/**
 * Periodically comment on the chat room's latest messages using ChatGPT. Posts
 * a response when a user mentions the bot in chat.
 * @author Michael Angstadt
 */
public class ChatGPT implements ScheduledTask, CatchAllMentionListener {
	private static final Logger logger = Logger.getLogger(ChatGPT.class.getName());

	private final String apiKey, prompt;
	private final Duration timeBetweenSpontaneousPosts;
	private final int completionMaxTokens, numLatestMessagesToIncludeInRequest, latestMessageCharacterLimit;
	private final Map<Integer, Instant> spontaneousPostTimes;
	private boolean ignoreNextMessage;

	/**
	 * @param apiKey the ChatGPT API key
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
	 * @param roomIds the rooms in which to start the spontaneous post
	 * timer when the bot first starts up. Mentioning the bot in a room causes
	 * the spontaneous post timer to start/reset in that room.
	 */
	public ChatGPT(String apiKey, String prompt, int completionMaxTokens, String timeBetweenSpontaneousPosts, int numLatestMessagesToIncludeInRequest, int latestMessageCharacterLimit, List<Integer> roomIds) {
		this.apiKey = apiKey;
		this.prompt = prompt;
		this.completionMaxTokens = completionMaxTokens;
		this.timeBetweenSpontaneousPosts = Duration.parse(timeBetweenSpontaneousPosts);
		this.numLatestMessagesToIncludeInRequest = numLatestMessagesToIncludeInRequest;
		this.latestMessageCharacterLimit = latestMessageCharacterLimit;

		spontaneousPostTimes = new HashMap<>();
		roomIds.forEach(this::resetSpontaneousPostTimer);
	}

	@Override
	public long nextRun() {
		long lowest;

		synchronized (spontaneousPostTimes) {
			if (spontaneousPostTimes.isEmpty()) {
				return timeBetweenSpontaneousPosts.toMillis();
			}

			Instant now = Instant.now();

			//@formatter:off
			lowest = spontaneousPostTimes.values().stream()
				.map(runTime -> Duration.between(now, runTime))
				.mapToLong(Duration::toMillis)
			.min().getAsLong();
			//@formatter:on
		}

		return (lowest < 1) ? 1 : lowest;
	}

	@Override
	public void run(IBot bot) throws Exception {
		removeRoomsBotIsNotIn(bot);
		List<Integer> roomsToPostTo = findRoomsToSpontaneouslyPostTo(bot);

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

			ChatGPTRequest request = buildChatGPTRequest(messages, bot);
			String response = sendChatGPTRequest(request);

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
			.detail("Mentioning the bot will start a conversation. The last " + numLatestMessagesToIncludeInRequest + " messages in the chat room are sent to the ChatGPT servers for context. Under certain conditions, the bot will also periodically post a message from ChatGPT on its own.")
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

			ChatGPTRequest request = buildChatGPTRequest(prevMessages, bot);
			String response = sendChatGPTRequest(request);

			resetSpontaneousPostTimer(message.getRoomId());

			return reply(response, message);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem communicating with ChatGPT.", e);
			return reply("I don't really feel like talking right now.", message);
		}
	}

	@Override
	public void ignoreNextMessage() {
		ignoreNextMessage = true;
	}

	private void removeRoomsBotIsNotIn(IBot bot) {
		synchronized (spontaneousPostTimes) {
			List<Integer> roomsBotIsIn = bot.getRooms();
			List<Integer> roomIds = spontaneousPostTimes.keySet().stream().filter(id -> {
				return !roomsBotIsIn.contains(id);
			}).collect(Collectors.toList());

			roomIds.forEach(spontaneousPostTimes::remove);
		}
	}

	private List<Integer> findRoomsToSpontaneouslyPostTo(IBot bot) {
		List<Integer> roomIds = new ArrayList<>();

		synchronized (spontaneousPostTimes) {
			for (Map.Entry<Integer, Instant> entry : spontaneousPostTimes.entrySet()) {
				Integer roomId = entry.getKey();
				Instant runTime = entry.getValue();

				if (runTime.isAfter(Instant.now())) {
					continue;
				}

				roomIds.add(roomId);
			}
		}

		return roomIds;
	}

	private ChatGPTRequest buildChatGPTRequest(List<ChatMessage> messages, IBot bot) {
		ChatGPTRequest request = new ChatGPTRequest(apiKey, prompt, completionMaxTokens);

		for (ChatMessage message : messages) {
			Content content = message.getContent();

			boolean messageWasDeleted = (content == null);
			if (messageWasDeleted) {
				continue;
			}

			String contentStr = content.getContent();
			boolean fixedFont = content.isFixedFont();
			String contentMd = ChatBuilder.toMarkdown(contentStr, fixedFont);

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

	private String sendChatGPTRequest(ChatGPTRequest request) throws IOException {
		try (CloseableHttpClient client = HttpFactory.connect().getClient()) {
			String response = request.send(client);
			return removeMentionsFromBeginningOfMessage(response);
		} catch (ChatGPTException e) {
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

	private static String removeMentionsFromBeginningOfMessage(String message) {
		String orig = message;
		while (true) {
			String result = orig.replaceAll("^@\\w+\\s+", "");
			if (result.equals(orig)) {
				return result;
			}
			orig = result;
		}
	}

	/**
	 * Resets the chat timer for the given room.
	 * @param roomId the room ID
	 */
	public void resetSpontaneousPostTimer(int roomId) {
		Instant nextRunTime = Instant.now().plus(timeBetweenSpontaneousPosts);
		synchronized (spontaneousPostTimes) {
			spontaneousPostTimes.put(roomId, nextRunTime);
		}
	}

	/**
	 * Creates an HTTP client. This method is for unit testing.
	 * @return the HTTP client
	 */
	CloseableHttpClient createClient() {
		return HttpClients.createDefault();
	}
}
